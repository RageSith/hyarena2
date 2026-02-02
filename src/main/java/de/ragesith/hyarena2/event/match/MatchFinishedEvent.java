package de.ragesith.hyarena2.event.match;

import de.ragesith.hyarena2.event.Event;

import java.util.UUID;

/**
 * Fired when a match is completely finished and ready for cleanup
 */
public class MatchFinishedEvent implements Event {
    private final UUID matchId;

    public MatchFinishedEvent(UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getMatchId() {
        return matchId;
    }
}
