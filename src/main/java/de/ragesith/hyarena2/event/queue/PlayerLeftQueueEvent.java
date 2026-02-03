package de.ragesith.hyarena2.event.queue;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a player leaves a queue
 */
public class PlayerLeftQueueEvent implements Event {
    private final UUID playerUuid;
    private final String arenaId;
    private final String reason;

    public PlayerLeftQueueEvent(UUID playerUuid, String arenaId, String reason) {
        this.playerUuid = playerUuid;
        this.arenaId = arenaId;
        this.reason = reason;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getReason() {
        return reason;
    }
}
