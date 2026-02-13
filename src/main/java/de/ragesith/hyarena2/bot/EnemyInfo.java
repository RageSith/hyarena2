package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.participant.Participant;

/**
 * Per-enemy data built each tick for utility scoring.
 * Contains everything the brain needs to score actions against this enemy.
 */
public class EnemyInfo {
    public final Participant participant;
    public final Ref<EntityStore> entityRef;
    public final Position position;
    public final double distance;
    public final double healthPercent;

    // Current interaction state
    public final boolean isAttacking;
    public final boolean isRangedAttacking;
    public final boolean isInZone;

    // Threat data (from brain's threat map)
    public final boolean isThreat;
    public final long lastHitTick;
    public final ThreatType threatType;
    public final double threatDamage;

    public EnemyInfo(Participant participant, Ref<EntityStore> entityRef, Position position,
                     double distance, double healthPercent,
                     boolean isAttacking, boolean isRangedAttacking, boolean isInZone,
                     boolean isThreat, long lastHitTick, ThreatType threatType, double threatDamage) {
        this.participant = participant;
        this.entityRef = entityRef;
        this.position = position;
        this.distance = distance;
        this.healthPercent = healthPercent;
        this.isAttacking = isAttacking;
        this.isRangedAttacking = isRangedAttacking;
        this.isInZone = isInZone;
        this.isThreat = isThreat;
        this.lastHitTick = lastHitTick;
        this.threatType = threatType;
        this.threatDamage = threatDamage;
    }
}
