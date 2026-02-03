package de.ragesith.hyarena2.event.queue;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a player joins a queue
 */
public class PlayerQueuedEvent implements Event {
    private final UUID playerUuid;
    private final String playerName;
    private final String arenaId;
    private final long joinTime;

    public PlayerQueuedEvent(UUID playerUuid, String playerName, String arenaId, long joinTime) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.arenaId = arenaId;
        this.joinTime = joinTime;
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
}
