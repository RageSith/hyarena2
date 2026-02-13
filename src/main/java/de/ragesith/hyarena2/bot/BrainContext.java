package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.config.Position;

import java.util.List;

/**
 * Data bag passed to BotBrain.evaluate() each tick.
 * Built by BotManager from existing data — no new queries needed.
 */
public class BrainContext {
    public final BotParticipant bot;
    public final Match match;
    public final Store<EntityStore> store;
    public final Position botPos;
    public final BotObjective objective;              // null for non-objective modes
    public final boolean botInZone;                   // false for non-objective modes
    public final boolean botIsAttacking;              // bot's own attack state
    public final ArenaConfig.Bounds arenaBounds;      // for roam waypoint generation

    // Full enemy list with per-enemy data
    public final List<EnemyInfo> enemies;

    // Aggregated threat counts (derived from enemies list)
    public final int activeThreatCount;
    public final int rangedThreatCount;
    public final int meleeThreatCount;

    // Bot state
    public final double botHealthPercent;
    public final double botBlockEnergy;

    // Convenience refs (derived from enemies list, may be null)
    public final EnemyInfo nearestEnemy;
    public final EnemyInfo nearestAttackingEnemy;
    public final EnemyInfo nearestThreat;

    public BrainContext(BotParticipant bot, Match match, Store<EntityStore> store, Position botPos,
                        BotObjective objective, boolean botInZone, boolean botIsAttacking,
                        ArenaConfig.Bounds arenaBounds, List<EnemyInfo> enemies,
                        double botHealthPercent, double botBlockEnergy) {
        this.bot = bot;
        this.match = match;
        this.store = store;
        this.botPos = botPos;
        this.objective = objective;
        this.botInZone = botInZone;
        this.botIsAttacking = botIsAttacking;
        this.arenaBounds = arenaBounds;
        this.enemies = enemies;
        this.botHealthPercent = botHealthPercent;
        this.botBlockEnergy = botBlockEnergy;

        // Derive aggregated counts and convenience refs
        int threats = 0, ranged = 0, melee = 0;
        EnemyInfo closest = null;
        EnemyInfo closestAttacking = null;
        EnemyInfo closestThreat = null;

        for (EnemyInfo e : enemies) {
            if (e.isThreat) {
                threats++;
                if (e.threatType == ThreatType.RANGED) ranged++;
                else if (e.threatType == ThreatType.MELEE) melee++;

                if (closestThreat == null || e.distance < closestThreat.distance) {
                    closestThreat = e;
                }
            }
            if (closest == null || e.distance < closest.distance) {
                closest = e;
            }
            if ((e.isAttacking || e.isRangedAttacking) &&
                (closestAttacking == null || e.distance < closestAttacking.distance)) {
                closestAttacking = e;
            }
        }

        this.activeThreatCount = threats;
        this.rangedThreatCount = ranged;
        this.meleeThreatCount = melee;
        this.nearestEnemy = closest;
        this.nearestAttackingEnemy = closestAttacking;
        this.nearestThreat = closestThreat;
    }
}
