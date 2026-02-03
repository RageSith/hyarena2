package de.ragesith.hyarena2.queue;

import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.kit.KitSelectedEvent;
import de.ragesith.hyarena2.event.queue.PlayerLeftQueueEvent;
import de.ragesith.hyarena2.event.queue.PlayerQueuedEvent;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.kit.KitManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages queues for all arenas.
 * Players queue for specific arenas, and the Matchmaker creates matches when conditions are met.
 */
public class QueueManager {

    private final EventBus eventBus;
    private final HubManager hubManager;
    private final String hubWorldName;

    // Queue for each arena
    private final Map<String, Queue<QueueEntry>> queues = new ConcurrentHashMap<>();

    // Track which queue each player is in
    private final Map<UUID, QueueEntry> playerQueues = new ConcurrentHashMap<>();

    // Historical queue times for average calculation (arena -> deque of durations in ms)
    private final Map<String, Deque<Long>> queueTimeHistory = new ConcurrentHashMap<>();

    // Track when players left a queue (for cooldown)
    private final Map<UUID, Long> leaveTimestamps = new ConcurrentHashMap<>();

    // Maximum history size for average calculation
    private static final int MAX_HISTORY_SIZE = 50;

    // Cooldown after leaving queue before rejoining (milliseconds)
    private static final long QUEUE_LEAVE_COOLDOWN_MS = 3000;

    // Function to check if player is in a match
    private java.util.function.Predicate<UUID> matchChecker;

    // Function to check if player is in hub world
    private java.util.function.Predicate<UUID> hubWorldChecker;

    // Kit manager reference for kit validation
    private KitManager kitManager;

    public QueueManager(EventBus eventBus, HubManager hubManager, String hubWorldName) {
        this.eventBus = eventBus;
        this.hubManager = hubManager;
        this.hubWorldName = hubWorldName;
    }

    /**
     * Sets the function to check if a player is in a match.
     */
    public void setMatchChecker(java.util.function.Predicate<UUID> checker) {
        this.matchChecker = checker;
    }

    /**
     * Sets the function to check if a player is in the hub world.
     */
    public void setHubWorldChecker(java.util.function.Predicate<UUID> checker) {
        this.hubWorldChecker = checker;
    }

    /**
     * Sets the kit manager for kit validation.
     */
    public void setKitManager(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    /**
     * Attempts to add a player to a queue without kit selection.
     *
     * @return JoinResult indicating success or failure reason
     */
    public JoinResult joinQueue(UUID playerUuid, String playerName, String arenaId) {
        return joinQueue(playerUuid, playerName, arenaId, null);
    }

    /**
     * Attempts to add a player to a queue with optional kit selection.
     *
     * @param playerUuid  the player's UUID
     * @param playerName  the player's display name
     * @param arenaId     the arena to queue for
     * @param kitId       the selected kit ID, or null for default/none
     * @return JoinResult indicating success or failure reason
     */
    public JoinResult joinQueue(UUID playerUuid, String playerName, String arenaId, String kitId) {
        // Check if already in a queue
        if (playerQueues.containsKey(playerUuid)) {
            return JoinResult.ALREADY_IN_QUEUE;
        }

        // Check if player is in a match
        if (matchChecker != null && matchChecker.test(playerUuid)) {
            return JoinResult.IN_MATCH;
        }

        // Check cooldown after leaving a queue
        if (isOnQueueCooldown(playerUuid)) {
            return JoinResult.ON_COOLDOWN;
        }

        // Check if player is in hub world
        if (hubWorldChecker != null && !hubWorldChecker.test(playerUuid)) {
            return JoinResult.NOT_IN_HUB;
        }

        // Validate kit if specified and kit manager is available
        if (kitId != null && kitManager != null) {
            if (!kitManager.kitExists(kitId)) {
                return JoinResult.INVALID_KIT;
            }
            if (!kitManager.isKitInArena(kitId, arenaId)) {
                return JoinResult.INVALID_KIT;
            }
            // Note: Permission check requires Player object, done at command level
        }

        QueueEntry entry = new QueueEntry(playerUuid, playerName, arenaId, kitId);
        Queue<QueueEntry> queue = queues.computeIfAbsent(arenaId, k -> new ConcurrentLinkedQueue<>());
        queue.add(entry);
        playerQueues.put(playerUuid, entry);

        // Clear any leave timestamp since they successfully joined
        leaveTimestamps.remove(playerUuid);

        // Update positions for this queue
        updateQueuePositions(arenaId);

        // Publish queue event
        eventBus.publish(new PlayerQueuedEvent(playerUuid, playerName, arenaId, entry.getJoinTime()));

        // Publish kit selected event if kit was specified
        if (kitId != null) {
            eventBus.publish(new KitSelectedEvent(playerUuid, kitId, arenaId));
        }

        return JoinResult.SUCCESS;
    }

    /**
     * Checks if a player is on cooldown after leaving a queue.
     */
    public boolean isOnQueueCooldown(UUID playerUuid) {
        Long leaveTime = leaveTimestamps.get(playerUuid);
        if (leaveTime == null) {
            return false;
        }
        return System.currentTimeMillis() - leaveTime < QUEUE_LEAVE_COOLDOWN_MS;
    }

    /**
     * Gets the remaining cooldown time in seconds.
     */
    public int getRemainingCooldownSeconds(UUID playerUuid) {
        Long leaveTime = leaveTimestamps.get(playerUuid);
        if (leaveTime == null) {
            return 0;
        }
        long remaining = QUEUE_LEAVE_COOLDOWN_MS - (System.currentTimeMillis() - leaveTime);
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /**
     * Updates cached positions for all entries in a queue.
     */
    private void updateQueuePositions(String arenaId) {
        Queue<QueueEntry> queue = queues.get(arenaId);
        if (queue == null) return;

        int pos = 1;
        for (QueueEntry e : queue) {
            e.setCachedPosition(pos++);
        }
    }

    /**
     * Removes a player from their current queue.
     *
     * @return the queue entry if found and removed, null otherwise
     */
    public QueueEntry leaveQueue(UUID playerUuid, String reason) {
        QueueEntry entry = playerQueues.remove(playerUuid);
        if (entry != null) {
            String arenaId = entry.getArenaId();
            Queue<QueueEntry> queue = queues.get(arenaId);
            if (queue != null) {
                queue.remove(entry);
                // Update positions for remaining entries
                updateQueuePositions(arenaId);
            }

            // Record leave time for cooldown (unless disconnected)
            if (!"Disconnected".equals(reason)) {
                leaveTimestamps.put(playerUuid, System.currentTimeMillis());
            }

            // Publish event
            eventBus.publish(new PlayerLeftQueueEvent(playerUuid, arenaId, reason));
        }
        return entry;
    }

    /**
     * Checks if a player is currently in any queue.
     */
    public boolean isInQueue(UUID playerUuid) {
        return playerQueues.containsKey(playerUuid);
    }

    /**
     * Gets the queue entry for a player.
     */
    public QueueEntry getQueueEntry(UUID playerUuid) {
        return playerQueues.get(playerUuid);
    }

    /**
     * Gets the queue for a specific arena.
     */
    public Queue<QueueEntry> getQueue(String arenaId) {
        return queues.getOrDefault(arenaId, new ConcurrentLinkedQueue<>());
    }

    /**
     * Gets the current queue size for an arena.
     */
    public int getQueueSize(String arenaId) {
        Queue<QueueEntry> queue = queues.get(arenaId);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Gets a player's position in their queue (1-indexed).
     * Uses cached position for O(1) lookup.
     *
     * @return position in queue, or -1 if not in queue
     */
    public int getQueuePosition(UUID playerUuid) {
        QueueEntry entry = playerQueues.get(playerUuid);
        if (entry == null) {
            return -1;
        }
        return entry.getCachedPosition();
    }

    /**
     * Removes and returns entries for a match.
     *
     * @param arenaId the arena to get entries for
     * @param count   the number of entries to retrieve
     * @return list of queue entries, or empty list if not enough players
     */
    public List<QueueEntry> pollEntries(String arenaId, int count) {
        Queue<QueueEntry> queue = queues.get(arenaId);
        if (queue == null || queue.size() < count) {
            return Collections.emptyList();
        }

        List<QueueEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            QueueEntry entry = queue.poll();
            if (entry != null) {
                playerQueues.remove(entry.getPlayerUuid());
                // Record queue time for statistics
                recordQueueTime(arenaId, entry.getQueueDuration());
                entries.add(entry);
            }
        }

        // Update positions for remaining entries
        updateQueuePositions(arenaId);

        return entries;
    }

    /**
     * Records a queue time for average calculation.
     */
    private void recordQueueTime(String arenaId, long durationMs) {
        Deque<Long> history = queueTimeHistory.computeIfAbsent(arenaId, k -> new ConcurrentLinkedDeque<>());
        history.addLast(durationMs);

        // Trim history if too large
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeFirst();
        }
    }

    /**
     * Gets the average queue time for an arena in milliseconds.
     *
     * @return average queue time, or 0 if no data available
     */
    public long getAverageQueueTime(String arenaId) {
        Deque<Long> history = queueTimeHistory.get(arenaId);
        if (history == null || history.isEmpty()) {
            return 0;
        }

        long sum = 0;
        int count = 0;
        for (Long time : history) {
            sum += time;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    /**
     * Gets the average queue time formatted as a string (e.g., "1m 30s").
     */
    public String getAverageQueueTimeFormatted(String arenaId) {
        long avgMs = getAverageQueueTime(arenaId);
        if (avgMs == 0) {
            return "N/A";
        }

        long seconds = avgMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (seconds == 0) {
            return minutes + "m";
        }
        return minutes + "m " + seconds + "s";
    }

    /**
     * Gets all arena IDs that have active queues.
     */
    public Set<String> getActiveArenaIds() {
        return new HashSet<>(queues.keySet());
    }

    /**
     * Handles player disconnect - removes from queue.
     */
    public void handlePlayerDisconnect(UUID playerUuid) {
        leaveQueue(playerUuid, "Disconnected");
        // Clean up cooldown timestamp
        leaveTimestamps.remove(playerUuid);
    }

    /**
     * Gets the total number of players across all queues.
     */
    public int getTotalQueuedPlayers() {
        return playerQueues.size();
    }

    /**
     * Result of attempting to join a queue.
     */
    public enum JoinResult {
        SUCCESS,
        ALREADY_IN_QUEUE,
        IN_MATCH,
        ON_COOLDOWN,
        NOT_IN_HUB,
        INVALID_KIT
    }
}
