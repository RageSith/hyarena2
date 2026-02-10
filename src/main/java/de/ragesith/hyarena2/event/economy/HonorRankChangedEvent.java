package de.ragesith.hyarena2.event.economy;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a player's honor rank changes (promotion or demotion).
 */
public class HonorRankChangedEvent implements Event {
    private final UUID playerUuid;
    private final String oldRankId;
    private final String newRankId;
    private final String newRankDisplayName;

    public HonorRankChangedEvent(UUID playerUuid, String oldRankId, String newRankId, String newRankDisplayName) {
        this.playerUuid = playerUuid;
        this.oldRankId = oldRankId;
        this.newRankId = newRankId;
        this.newRankDisplayName = newRankDisplayName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getOldRankId() {
        return oldRankId;
    }

    public String getNewRankId() {
        return newRankId;
    }

    public String getNewRankDisplayName() {
        return newRankDisplayName;
    }
}
