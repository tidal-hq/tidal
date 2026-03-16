package net.tidalhq.tidal.notification;

import net.tidalhq.tidal.Tidal;

import java.util.ArrayList;
import java.util.List;

public class Notifier {
    private final List<NotificationChannel> channels = new ArrayList<>();
    private final String prefix;

    public Notifier() {
        this(null);
    }

    public Notifier(String prefix) {
        this.prefix = prefix;
        channels.add(new ChatChannel());
    }

    public void addChannel(NotificationChannel channel) {
        channels.add(channel);
    }

    public Notifier scoped(String name) {
        Notifier child = new Notifier(name);
        child.channels.clear();
        child.channels.addAll(this.channels);
        return child;
    }

    public void send(String message, Notification.NotificationLevel level) {
        String formatted = prefix != null ? "[" + prefix + "] " + message : message;
        Notification notification = new Notification(formatted, level);
        channels.forEach(ch -> ch.send(notification));
        switch (level) {
            case INFO    -> Tidal.LOGGER.info("[{}] {}", prefix != null ? prefix : "Tidal", message);
            case WARNING -> Tidal.LOGGER.warn("[{}] {}", prefix != null ? prefix : "Tidal", message);
            case DANGER,
                 ERROR   -> Tidal.LOGGER.error("[{}] {}", prefix != null ? prefix : "Tidal", message);
        }
    }

    public void info(String message)    { send(message, Notification.NotificationLevel.INFO); }
    public void warning(String message) { send(message, Notification.NotificationLevel.WARNING); }
    public void danger(String message)  { send(message, Notification.NotificationLevel.DANGER); }
    public void error(String message)   { send(message, Notification.NotificationLevel.ERROR); }

    public void debug(String message) {
        Tidal.LOGGER.debug("[{}] {}", prefix != null ? prefix : "Tidal", message);
    }
}