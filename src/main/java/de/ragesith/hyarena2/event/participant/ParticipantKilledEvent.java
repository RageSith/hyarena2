package de.ragesith.hyarena2.event.participant;

import de.ragesith.hyarena2.event.Event;
import de.ragesith.hyarena2.participant.Participant;

import java.util.UUID;

/**
 * Fired when a participant is killed in a match
 */
public class ParticipantKilledEvent implements Event {
    private final UUID matchId;
    private final Participant victim;
    private final Participant killer; // null for environmental deaths

    public ParticipantKilledEvent(UUID matchId, Participant victim, Participant killer) {
        this.matchId = matchId;
        this.victim = victim;
        this.killer = killer;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public Participant getVictim() {
        return victim;
    }

    public Participant getKiller() {
        return killer;
    }

    public boolean isEnvironmentalDeath() {
        return killer == null;
    }
}
