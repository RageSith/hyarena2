package de.ragesith.hyarena2.queue;

import java.util.UUID;

/**
 * Represents a player's entry in a queue.
 */
public class QueueEntry {
    private final UUID playerUuid;
    private final String playerName;
    private final String arenaId;
    private final long joinTime;
    private volatile int cachedPosition;

    public QueueEntry(UUID playerUuid, String playerName, String arenaId) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.arenaId = arenaId;
        this.joinTime = System.currentTimeMillis();
        this.cachedPosition = 1;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getArenaId() {
        return arenaId;
    }

    public long getJoinTime() {
        return joinTime;
    }

    /**
     * Gets the duration in milliseconds since joining the queue.
     */
    public long getQueueDuration() {
        return System.currentTimeMillis() - joinTime;
    }

    /**
     * Gets the cached queue position (1-indexed).
     */
    public int getCachedPosition() {
        return cachedPosition;
    }

    /**
     * Sets the cached queue position.
     */
    public void setCachedPosition(int position) {
        this.cachedPosition = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueEntry that = (QueueEntry) o;
        return playerUuid.equals(that.playerUuid);
    }

    @Override
    public int hashCode() {
        return playerUuid.hashCode();
    }
}
