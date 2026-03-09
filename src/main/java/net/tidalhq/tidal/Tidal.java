package net.tidalhq.tidal;

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
import net.tidalhq.tidal.feature.impl.AutoGodPotFeature;
import net.tidalhq.tidal.feature.impl.PestWarningFeature;
import net.tidalhq.tidal.gui.MainScreen;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.macro.MacroManager;
import net.tidalhq.tidal.macro.impl.SShapeMushroomSDSMacro;
import net.tidalhq.tidal.notification.Notifier;
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

		FeatureContext featureCtx = new FeatureContext(gameState, eventBus, notifier);
		FeatureManager featureManager = new FeatureManager();
		featureManager.register(new PestWarningFeature(featureCtx));
		featureManager.register(new AutoGodPotFeature(featureCtx));
		featureManager.setEnabled("pest_warning", true);
		featureManager.setEnabled("auto_god_pot", true);

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

		ClientTickEvents.END_CLIENT_TICK.register(c ->
				eventBus.post(new ClientTickEvent()));

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
	}
}