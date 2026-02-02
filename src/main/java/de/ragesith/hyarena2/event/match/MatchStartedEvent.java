package de.ragesith.hyarena2.event.match;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a match enters the STARTING state (countdown begins)
 */
public class MatchStartedEvent implements Event {
    private final UUID matchId;
    private final int countdownSeconds;

    public MatchStartedEvent(UUID matchId, int countdownSeconds) {
        this.matchId = matchId;
        this.countdownSeconds = countdownSeconds;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }
}
