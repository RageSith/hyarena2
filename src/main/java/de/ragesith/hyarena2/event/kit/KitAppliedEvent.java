package de.ragesith.hyarena2.event.kit;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a kit is applied to a player (at match start).
 */
public class KitAppliedEvent implements Event {
    private final UUID playerUuid;
    private final String kitId;

    public KitAppliedEvent(UUID playerUuid, String kitId) {
        this.playerUuid = playerUuid;
        this.kitId = kitId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getKitId() {
        return kitId;
    }
}
