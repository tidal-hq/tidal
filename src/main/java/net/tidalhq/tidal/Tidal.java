package net.tidalhq.tidal;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.tidalhq.tidal.gui.MainScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

@Environment(EnvType.CLIENT)
public class Tidal implements ClientModInitializer {
	public static final String MOD_ID = "tidal";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final MinecraftClient mc = MinecraftClient.getInstance();

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal(MOD_ID)
				.executes(context -> {

							// Escape from command context and send an event to the client
							mc.send(() -> {
								mc.setScreen(new MainScreen());
							});

							return 1;
						}
				)));
	}
}