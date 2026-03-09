package net.tidalhq.tidal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.tidalhq.tidal.config.ConfigSerializer;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.ClientTickEvent;
import net.tidalhq.tidal.event.impl.ServerConnectEvent;
import net.tidalhq.tidal.event.impl.ServerDisconnectEvent;
import net.tidalhq.tidal.feature.FeatureContext;
import net.tidalhq.tidal.feature.FeatureManager;
import net.tidalhq.tidal.feature.impl.PestWarningFeature;
import net.tidalhq.tidal.gui.MainScreen;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.macro.MacroManager;
import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;
import net.tidalhq.tidal.notification.Notifier;
import net.tidalhq.tidal.pathfind.PathfindingSession;
import net.tidalhq.tidal.state.CompositeGameStateView;
import net.tidalhq.tidal.state.ServerState;
import net.tidalhq.tidal.state.TablistState;
import net.tidalhq.tidal.world.MinecraftWorldAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class Tidal implements ClientModInitializer {

	public static final String MOD_ID = "tidal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static FeatureManager featureManager;
	private static PathfindingSession activeNavigation;

	private static MacroManager macroManager;
	public static MacroManager getMacroManager() { return macroManager; }

	@Override
	public void onInitializeClient() {
		MinecraftClient client = MinecraftClient.getInstance();
		EventBus eventBus = EventBus.getInstance();

		ServerState serverState = new ServerState(eventBus);
		TablistState tablistState = new TablistState(eventBus);
		CompositeGameStateView gameState = new CompositeGameStateView(serverState, tablistState);

		Notifier notifier = new Notifier();
		MinecraftWorldAccessor worldAccessor = new MinecraftWorldAccessor(client);

		FeatureContext featureCtx = new FeatureContext(gameState, eventBus, notifier, worldAccessor);
		featureManager = new FeatureManager();
		featureManager.register(new PestWarningFeature(featureCtx));
		featureManager.setEnabled("pest_warning", true);

		ConfigSerializer configSerializer = new ConfigSerializer(
				net.fabricmc.loader.api.FabricLoader.getInstance()
						.getConfigDir().resolve("tidal.properties")
		);
		configSerializer.load(featureManager.getRegistry(), featureManager);

		Runtime.getRuntime().addShutdownHook(new Thread(() ->
				configSerializer.save(featureManager.getRegistry())));


		MacroContext ctx = new MacroContext(worldAccessor, gameState, eventBus, notifier);
		macroManager = new MacroManager(ctx, featureManager);
		macroManager.register("ssdsmushroom", new SShapeMushroomSDSMacro(ctx));

		registerFabricEvents(eventBus, client);
		registerCommands();
	}

	private void registerFabricEvents(EventBus eventBus, MinecraftClient client) {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, c) ->
				eventBus.post(new ServerConnectEvent(c.getCurrentServerEntry())));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, c) ->
				eventBus.post(new ServerDisconnectEvent()));

		ClientTickEvents.END_CLIENT_TICK.register(c -> {
			eventBus.post(new ClientTickEvent());
			if (activeNavigation != null && activeNavigation.isActive()) {
				activeNavigation.onTick();
			}
		});
		ClientReceiveMessageEvents.GAME.register((message, signed) ->
				eventBus.post(new ClientReceiveGameMessageEvent(message.getString())));
	}

	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal(MOD_ID)
						.executes(context -> {
							MinecraftClient.getInstance().send(() ->
									MinecraftClient.getInstance().setScreen(new MainScreen()));
							return 1;
						})));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("macro")
						.then(ClientCommandManager.literal("toggle")
								.executes(context -> {
									macroManager.setEnabled(!macroManager.isEnabled());
									context.getSource().sendFeedback(Text.literal(
											"Macro " + (macroManager.isEnabled() ? "enabled" : "disabled")));
									return 1;
								}))
						.then(ClientCommandManager.literal("select")
								.then(ClientCommandManager.argument("name", StringArgumentType.string())
										.executes(context -> {
											String name = StringArgumentType.getString(context, "name");
											macroManager.setActiveMacro(name);
											context.getSource().sendFeedback(Text.literal("Selected macro: " + name));
											return 1;
										})))));
		registerGotoCommand();
	}

	private void registerGotoCommand() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("goto")
						.then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
								.then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
										.then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
												.executes(context -> {
													int x = IntegerArgumentType.getInteger(context, "x");
													int y = IntegerArgumentType.getInteger(context, "y");
													int z = IntegerArgumentType.getInteger(context, "z");

													net.minecraft.util.math.BlockPos goal =
															new net.minecraft.util.math.BlockPos(x, y, z);

													if (activeNavigation != null && activeNavigation.isActive()) {
														activeNavigation.stop();
													}

													activeNavigation = new PathfindingSession(
															result -> context.getSource().sendFeedback(
																	Text.literal("[goto] Failed: " + result.getStatus()
																					+ " → " + goal.toShortString())
																			.styled(s -> s.withColor(0xFF5555))),
															() -> context.getSource().sendFeedback(
																	Text.literal("[goto] Arrived at " + goal.toShortString())
																			.styled(s -> s.withColor(0x55FF55)))
													);

													context.getSource().sendFeedback(
															Text.literal("[goto] Searching path to "
																			+ goal.toShortString() + "…")
																	.styled(s -> s.withColor(0xFFAA00)));

													MinecraftClient.getInstance().execute(() -> activeNavigation.navigateTo(goal));
													return 1;
												}))))
						.then(ClientCommandManager.literal("cancel")
								.executes(context -> {
									if (activeNavigation == null || !activeNavigation.isActive()) {
										context.getSource().sendFeedback(
												Text.literal("[goto] No active navigation."));
										return 0;
									}
									activeNavigation.stop();
									context.getSource().sendFeedback(
											Text.literal("[goto] Navigation cancelled.")
													.styled(s -> s.withColor(0xAAAAAA)));
									return 1;
								}))));
	}
}