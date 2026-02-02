package de.ragesith.hyarena2.event.participant;

import de.ragesith.hyarena2.event.Event;
import de.ragesith.hyarena2.participant.Participant;

import java.util.UUID;

/**
 * Fired when a participant takes damage in a match
 */
public class ParticipantDamagedEvent implements Event {
    private final UUID matchId;
    private final Participant victim;
    private final Participant attacker; // null for environmental damage
    private final double damage;

    public ParticipantDamagedEvent(UUID matchId, Participant victim, Participant attacker, double damage) {
        this.matchId = matchId;
        this.victim = victim;
        this.attacker = attacker;
        this.damage = damage;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public Participant getVictim() {
        return victim;
    }

    public Participant getAttacker() {
        return attacker;
    }

    public double getDamage() {
        return damage;
    }

    public boolean isEnvironmentalDamage() {
        return attacker == null;
    }
}
