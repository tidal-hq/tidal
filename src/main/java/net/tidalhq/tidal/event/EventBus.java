package net.tidalhq.tidal.event;

import net.tidalhq.tidal.Tidal;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// event bus singleton
public class EventBus {
    private static EventBus instance;
    private final Map<Class<?>, List<EventHandler>> listeners = new ConcurrentHashMap<>();


    public static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }

        return instance;
    }

    private EventBus() {}

    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class) && method.getParameterCount() == 1) {
                Class<?> eventType = method.getParameterTypes()[0];
                method.setAccessible(true);

                listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                        .add(new EventHandler(listener, method));
            }
        }
    }

    public void unregister(Object listener) {
        for (List<EventHandler> handlers : listeners.values()) {
            handlers.removeIf(handler -> handler.listener == listener);
        }
    }

    public void post(Event event) {
        List<EventHandler> handlers = listeners.get(event.getClass());
        if (handlers != null) {
            for (EventHandler handler : handlers) {
                try {
                    handler.method.invoke(handler.listener, event);
                } catch (Exception e) {
                    Tidal.LOGGER.error("EventBus", e);
                }
            }
        }
    }

    private static class EventHandler {
        final Object listener;
        final Method method;

        EventHandler(Object listener, Method method) {
            this.listener = listener;
            this.method = method;
        }
    }

}
