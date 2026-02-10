package de.ragesith.hyarena2.event.economy;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a player earns honor.
 */
public class HonorEarnedEvent implements Event {
    private final UUID playerUuid;
    private final double amount;
    private final String reason;

    public HonorEarnedEvent(UUID playerUuid, double amount, String reason) {
        this.playerUuid = playerUuid;
        this.amount = amount;
        this.reason = reason;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public double getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }
}
