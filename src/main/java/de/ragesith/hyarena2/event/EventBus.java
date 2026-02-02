package de.ragesith.hyarena2.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple event bus for decoupled communication between components.
 * Allows publishing events and subscribing handlers without tight coupling.
 *
 * <p>Thread Safety: Uses ConcurrentHashMap and CopyOnWriteArrayList
 * for safe concurrent access.
 *
 * <p>Usage:
 * <pre>
 * // Subscribe to an event type
 * eventBus.subscribe(MatchStartedEvent.class, event -> {
 *     System.out.println("Match started: " + event.matchId());
 * });
 *
 * // Publish an event
 * eventBus.publish(new MatchStartedEvent(matchId));
 * </pre>
 */
public class EventBus {

    private final Map<Class<?>, List<Consumer<?>>> handlers = new ConcurrentHashMap<>();
    private volatile boolean active = true;

    /**
     * Subscribes a handler to an event type.
     *
     * @param eventType the event class to subscribe to
     * @param handler   the handler to invoke when events are published
     * @param <T>       the event type
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        if (eventType == null || handler == null) {
            return;
        }

        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(handler);
    }

    /**
     * Unsubscribes a handler from an event type.
     *
     * @param eventType the event class to unsubscribe from
     * @param handler   the handler to remove
     * @param <T>       the event type
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        if (eventType == null || handler == null) {
            return;
        }

        List<Consumer<?>> list = handlers.get(eventType);
        if (list != null) {
            list.remove(handler);
        }
    }

    /**
     * Publishes an event to all subscribed handlers.
     * Handlers are invoked synchronously in subscription order.
     * Exceptions in handlers are caught and logged.
     *
     * @param event the event to publish
     * @param <T>   the event type
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        if (!active || event == null) {
            return;
        }

        List<Consumer<?>> list = handlers.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }

        for (Consumer<?> handler : list) {
            try {
                ((Consumer<T>) handler).accept(event);
            } catch (Exception e) {
                System.err.println("[EventBus] Handler failed for " + event.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Publishes an event asynchronously on a virtual thread.
     *
     * @param event the event to publish
     * @param <T>   the event type
     */
    public <T> void publishAsync(T event) {
        if (!active || event == null) {
            return;
        }

        Thread.startVirtualThread(() -> publish(event));
    }

    /**
     * Checks if there are any handlers subscribed to an event type.
     */
    public boolean hasHandlers(Class<?> eventType) {
        List<Consumer<?>> list = handlers.get(eventType);
        return list != null && !list.isEmpty();
    }

    /**
     * Gets the number of handlers subscribed to an event type.
     */
    public int getHandlerCount(Class<?> eventType) {
        List<Consumer<?>> list = handlers.get(eventType);
        return list != null ? list.size() : 0;
    }

    /**
     * Clears all handlers for an event type.
     */
    public void clearHandlers(Class<?> eventType) {
        handlers.remove(eventType);
    }

    /**
     * Clears all handlers for all event types.
     */
    public void clearAllHandlers() {
        handlers.clear();
    }

    /**
     * Shuts down the event bus.
     */
    public void shutdown() {
        active = false;
        clearAllHandlers();
        System.out.println("[HyArena2] EventBus shutdown complete");
    }

    /**
     * Checks if the event bus is active.
     */
    public boolean isActive() {
        return active;
    }
}
