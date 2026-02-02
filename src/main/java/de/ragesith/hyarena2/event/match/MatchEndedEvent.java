package de.ragesith.hyarena2.event.match;

import de.ragesith.hyarena2.event.Event;

import java.util.List;
import java.util.UUID;

/**
 * Fired when a match ends (win condition met, transitions to ENDING state)
 */
public class MatchEndedEvent implements Event {
    private final UUID matchId;
    private final List<UUID> winners;
    private final String victoryMessage;

    public MatchEndedEvent(UUID matchId, List<UUID> winners, String victoryMessage) {
        this.matchId = matchId;
        this.winners = winners;
        this.victoryMessage = victoryMessage;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public List<UUID> getWinners() {
        return winners;
    }

    public String getVictoryMessage() {
        return victoryMessage;
    }
}
