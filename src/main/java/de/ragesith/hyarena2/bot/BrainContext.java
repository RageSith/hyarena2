package de.ragesith.hyarena2.bot;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.config.Position;

import java.util.List;
import java.util.UUID;

/**
 * Data bag passed to BotBrain.evaluate() each tick.
 * Built by BotManager from existing data — no new queries needed.
 */
public class BrainContext {
    public final BotParticipant bot;
    public final Match match;
    public final Store<EntityStore> store;
    public final Position botPos;
    public final BotObjective objective;          // null for non-objective modes
    public final NearestEnemy nearestEnemy;        // null if no enemies found
    public final List<UUID> enemiesInZone;         // empty for non-objective modes
    public final boolean botInZone;                // false for non-objective modes
    public final boolean enemyIsAttacking;         // current target's attack state
    public final boolean botIsAttacking;           // bot's own attack state
    public final ArenaConfig.Bounds arenaBounds;   // for roam waypoint generation
    public final NearestEnemy threatTarget;        // resolved lowest-HP active threat (null if none)

    public BrainContext(BotParticipant bot, Match match, Store<EntityStore> store, Position botPos,
                        BotObjective objective, NearestEnemy nearestEnemy, List<UUID> enemiesInZone,
                        boolean botInZone, boolean enemyIsAttacking, boolean botIsAttacking,
                        ArenaConfig.Bounds arenaBounds, NearestEnemy threatTarget) {
        this.bot = bot;
        this.match = match;
        this.store = store;
        this.botPos = botPos;
        this.objective = objective;
        this.nearestEnemy = nearestEnemy;
        this.enemiesInZone = enemiesInZone;
        this.botInZone = botInZone;
        this.enemyIsAttacking = enemyIsAttacking;
        this.botIsAttacking = botIsAttacking;
        this.arenaBounds = arenaBounds;
        this.threatTarget = threatTarget;
    }
}
