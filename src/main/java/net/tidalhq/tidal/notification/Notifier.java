package net.tidalhq.tidal.notification;

import java.util.ArrayList;
import java.util.List;

public class Notifier {
    private final List<NotificationChannel> channels = new ArrayList<>();

    public Notifier() {
        channels.add(new ChatChannel()); // always on
    }

    public void addChannel(NotificationChannel channel) {
        channels.add(channel);
    }

    public void send(String message, Notification.NotificationLevel level) {
        Notification notification = new Notification(message, level);
        channels.forEach(ch -> ch.send(notification));
    }

    public void info(String message)    { send(message, Notification.NotificationLevel.INFO); }
    public void warning(String message) { send(message, Notification.NotificationLevel.WARNING); }
    public void danger(String message)  { send(message, Notification.NotificationLevel.DANGER); }
}