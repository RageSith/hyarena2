package de.ragesith.hyarena2.event.match;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired per-player when they should receive match rewards.
 * Can be fired at any moment: elimination, match end, wave clear.
 */
public class PlayerMatchRewardEvent implements Event {
    private final UUID playerUuid;
    private final UUID matchId;
    private final boolean winner;
    private final int kills;
    private final boolean hasBots;
    private final boolean minigame;

    public PlayerMatchRewardEvent(UUID playerUuid, UUID matchId, boolean winner, int kills, boolean hasBots) {
        this(playerUuid, matchId, winner, kills, hasBots, false);
    }

    public PlayerMatchRewardEvent(UUID playerUuid, UUID matchId, boolean winner, int kills, boolean hasBots, boolean minigame) {
        this.playerUuid = playerUuid;
        this.matchId = matchId;
        this.winner = winner;
        this.kills = kills;
        this.hasBots = hasBots;
        this.minigame = minigame;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public boolean isWinner() {
        return winner;
    }

    public int getKills() {
        return kills;
    }

    public boolean hasBots() {
        return hasBots;
    }

    public boolean isMinigame() {
        return minigame;
    }
}
