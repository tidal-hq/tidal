package net.tidalhq.tidal;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.tidalhq.tidal.gui.MainScreen;
import net.tidalhq.tidal.state.ServerState;
import net.tidalhq.tidal.state.TablistState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

@Environment(EnvType.CLIENT)
public class Tidal implements ClientModInitializer {
	public static final String MOD_ID = "tidal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final MinecraftClient mc = MinecraftClient.getInstance();

	private static ServerState serverState;

	@Override
	public void onInitializeClient() {
		serverState = ServerState.getInstance();

		registerCommands();
		registerConnectionEvents();
		registerTickEvents();
	}

	private void registerConnectionEvents() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			serverState.update();

		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			serverState.reset();
		});
	}

	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal(MOD_ID).executes( context -> {
				mc.send(() -> {
					mc.setScreen(new MainScreen());
				});

				return 1;
			}
			));
		});


		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("loc").executes( context -> {
						context.getSource().sendFeedback(Text.literal(TablistState.getInstance().getCurrentLocation().name()));
						return 1;
					}
			));
		});
	}

	private void registerTickEvents() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world != null && client.player != null) {
				if (client.player.age % 20 == 0) {
					serverState.update();
				}
			}
		});
	}
}