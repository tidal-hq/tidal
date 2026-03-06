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
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.ClientTickEvent;
import net.tidalhq.tidal.event.impl.ServerConnectEvent;
import net.tidalhq.tidal.event.impl.ServerDisconnectEvent;
import net.tidalhq.tidal.gui.MainScreen;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.macro.MacroManager;
import net.tidalhq.tidal.state.ServerState;
import net.tidalhq.tidal.state.TablistState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class Tidal implements ClientModInitializer {
	public static final String MOD_ID = "tidal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static MacroManager macroManager;
	public static MacroManager getMacroManager() {
		return macroManager;
	}

	private static EventBus eventBus;

	@Override
	public void onInitializeClient() {
		MacroContext ctx = new MacroContext(
			MinecraftClient.getInstance(),
				ServerState.getInstance(),
				TablistState.getInstance(),
				EventBus.getInstance()
		);
		eventBus = EventBus.getInstance();

		macroManager = new MacroManager(ctx);

		eventBus.register(this);

		registerCommands();
		registerEvents();
	}

	private void registerEvents() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			eventBus.post(new ServerConnectEvent(client.getCurrentServerEntry()));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			eventBus.post(new ServerDisconnectEvent());
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			eventBus.post(new ClientTickEvent());
		});

		ClientReceiveMessageEvents.GAME.register((message, signedMessage) -> {
			eventBus.post(new ClientReceiveGameMessageEvent(message.getString()));
		});
	}

	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal(MOD_ID).executes( context -> {
				MinecraftClient.getInstance().send(() -> {
					MinecraftClient.getInstance().setScreen(new MainScreen());
				});

				return 1;
			}
			));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("macro")
					.then(ClientCommandManager.literal("toggle")
							.executes(context -> {
								macroManager.setEnabled(!macroManager.isEnabled());
								context.getSource().sendFeedback(
										Text.literal("Macro " + (macroManager.isEnabled() ? "enabled" : "disabled"))
								);
								return 1;
							}))
					.then(ClientCommandManager.literal("select")
							.then(ClientCommandManager.argument("name", StringArgumentType.string())
									.executes(context -> {
										String name = StringArgumentType.getString(context, "name");
										macroManager.setActiveMacro(name);
										context.getSource().sendFeedback(Text.literal("Selected macro: " + name));
										return 1;
									}))));
		});
	}
}