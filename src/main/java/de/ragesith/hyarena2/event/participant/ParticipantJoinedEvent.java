package de.ragesith.hyarena2.event.participant;

import de.ragesith.hyarena2.event.Event;
import de.ragesith.hyarena2.participant.Participant;

import java.util.UUID;

/**
 * Fired when a participant joins a match
 */
public class ParticipantJoinedEvent implements Event {
    private final UUID matchId;
    private final Participant participant;

    public ParticipantJoinedEvent(UUID matchId, Participant participant) {
        this.matchId = matchId;
        this.participant = participant;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public Participant getParticipant() {
        return participant;
    }
}
