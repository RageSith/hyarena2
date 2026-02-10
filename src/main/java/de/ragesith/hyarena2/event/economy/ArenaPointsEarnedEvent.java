package de.ragesith.hyarena2.event.economy;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a player earns ArenaPoints.
 */
public class ArenaPointsEarnedEvent implements Event {
    private final UUID playerUuid;
    private final int amount;
    private final String reason;

    public ArenaPointsEarnedEvent(UUID playerUuid, int amount, String reason) {
        this.playerUuid = playerUuid;
        this.amount = amount;
        this.reason = reason;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }
}
