package de.ragesith.hyarena2.event.participant;

import de.ragesith.hyarena2.event.Event;
import de.ragesith.hyarena2.participant.Participant;

import java.util.UUID;

/**
 * Fired when a participant leaves a match (disconnect or removal)
 */
public class ParticipantLeftEvent implements Event {
    private final UUID matchId;
    private final Participant participant;
    private final String reason;

    public ParticipantLeftEvent(UUID matchId, Participant participant, String reason) {
        this.matchId = matchId;
        this.participant = participant;
        this.reason = reason;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public Participant getParticipant() {
        return participant;
    }

    public String getReason() {
        return reason;
    }
}
