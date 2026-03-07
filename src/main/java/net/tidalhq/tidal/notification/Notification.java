package net.tidalhq.tidal.notification;

public record Notification(String message, NotificationLevel level) {
    public enum NotificationLevel { INFO, WARNING, DANGER, ERROR }
}
