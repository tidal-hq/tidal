package net.tidalhq.tidal.notification;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Map;

public class ChatChannel implements NotificationChannel {
    @SuppressWarnings("resource")
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final Map<Notification.NotificationLevel, String> COLORS = Map.of(
            Notification.NotificationLevel.INFO,    "§f",
            Notification.NotificationLevel.WARNING, "§e",
            Notification.NotificationLevel.DANGER,  "§c"
    );

    private static final Text PREFIX = Text.literal("§f[")
            .append(Text.literal("§bTidal"))
            .append(Text.literal("§f] "));

    @Override
    public void send(Notification notification) {
        if (client.player == null) return;
        String color = COLORS.getOrDefault(notification.level(), "§f");
        Text message = Text.literal("").append(PREFIX).append(Text.literal(color + notification.message()));
        client.player.sendMessage(message, false);
    }
}