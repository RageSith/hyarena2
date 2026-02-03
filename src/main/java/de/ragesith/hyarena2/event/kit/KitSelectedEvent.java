package de.ragesith.hyarena2.event.kit;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a player selects a kit (e.g., when joining a queue).
 */
public class KitSelectedEvent implements Event {
    private final UUID playerUuid;
    private final String kitId;
    private final String arenaId;

    public KitSelectedEvent(UUID playerUuid, String kitId, String arenaId) {
        this.playerUuid = playerUuid;
        this.kitId = kitId;
        this.arenaId = arenaId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getKitId() {
        return kitId;
    }

    public String getArenaId() {
        return arenaId;
    }
}
