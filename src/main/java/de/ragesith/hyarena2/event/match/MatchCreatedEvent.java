package de.ragesith.hyarena2.event.match;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a new match is created
 */
public class MatchCreatedEvent implements Event {
    private final UUID matchId;
    private final String arenaId;
    private final String gameMode;

    public MatchCreatedEvent(UUID matchId, String arenaId, String gameMode) {
        this.matchId = matchId;
        this.arenaId = arenaId;
        this.gameMode = gameMode;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getGameMode() {
        return gameMode;
    }
}
