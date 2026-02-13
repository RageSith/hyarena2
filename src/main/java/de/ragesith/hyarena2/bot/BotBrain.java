package de.ragesith.hyarena2.bot;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.participant.Participant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Utility-based decision engine for bot AI.
 * One instance per bot (stored on BotParticipant).
 * Each tick, scores all possible actions and returns the highest-scoring ScoredAction.
 */
public class BotBrain {

    private final BotDifficulty difficulty;
    private final Random random = new Random();

    // Current decision state (for momentum bonus)
    private BrainDecision currentDecision = BrainDecision.IDLE;

    // Threat tracking — maps attacker participant UUID → ThreatEntry
    private final Map<UUID, ThreatEntry> threats = new HashMap<>();
    private long currentTick = 0;

    // Roam state
    private Position roamWaypoint;
    private int roamTicksRemaining;
    private int idleTicks;

    // Block energy system
    private double blockEnergy;
    private boolean currentlyBlocking;

    // Constants
    private static final double BLOCK_DRAIN_RATE = 3.0;
    private static final double BLOCK_REGEN_RATE = 1.5;
    private static final int IDLE_BEFORE_ROAM_TICKS = 40;
    private static final int ROAM_MAX_TICKS = 100;
    private static final double ROAM_ARRIVAL_DIST = 2.0;
    private static final double ROAM_MARGIN = 3.0;

    public BotBrain(BotDifficulty difficulty) {
        this.difficulty = difficulty;
        this.blockEnergy = difficulty.getBlockMaxEnergy();
    }

    /**
     * Main evaluation method — called every tick from BotManager.
     * Scores all possible actions and returns the highest-scoring one.
     */
    public ScoredAction evaluate(BrainContext ctx) {
        currentTick++;

        // Prune stale threats
        pruneThreats(ctx);

        // Block energy management
        if (currentlyBlocking) {
            blockEnergy -= BLOCK_DRAIN_RATE;
            if (blockEnergy <= 0) {
                blockEnergy = 0;
                currentlyBlocking = false;
            }
        } else {
            blockEnergy = Math.min(difficulty.getBlockMaxEnergy(), blockEnergy + BLOCK_REGEN_RATE);
        }

        // Track best action
        ScoredAction best = ScoredAction.of(BrainDecision.IDLE, 0.05);

        // ========== Per-Enemy Actions ==========
        for (EnemyInfo enemy : ctx.enemies) {
            // ATTACK_ENEMY
            double attackScore = scoreAttackEnemy(enemy, ctx);
            if (attackScore > best.score()) {
                best = ScoredAction.of(BrainDecision.COMBAT, attackScore, enemy);
            }

            // DEFEND_ZONE — only if bot is on the zone
            if (ctx.objective != null && ctx.botInZone) {
                double defendScore = scoreDefendZone(enemy, ctx);
                if (defendScore > best.score()) {
                    best = ScoredAction.of(BrainDecision.DEFEND_ZONE, defendScore, enemy);
                }
            }
        }

        // ========== Singleton Actions ==========

        // BLOCK
        double blockScore = scoreBlock(ctx);
        if (blockScore > best.score()) {
            best = ScoredAction.of(BrainDecision.BLOCK, blockScore);
        }

        // GO_TO_OBJECTIVE
        if (ctx.objective != null) {
            double objectiveScore = scoreObjective(ctx);
            if (objectiveScore > best.score()) {
                best = ScoredAction.of(BrainDecision.OBJECTIVE, objectiveScore);
            }
        }

        // STRAFE_EVADE
        double strafeScore = scoreStrafeEvade(ctx);
        if (strafeScore > best.score()) {
            best = ScoredAction.of(BrainDecision.STRAFE_EVADE, strafeScore);
        }

        // ROAM
        double roamScore = scoreRoam(ctx);
        if (roamScore > best.score()) {
            best = ScoredAction.of(BrainDecision.ROAM, roamScore);
        }

        // Apply decision momentum — current action gets a bonus
        if (best.decision() == currentDecision) {
            // Already the winner, momentum already implicitly applied
        } else {
            // Check if current decision + momentum would beat the new best
            double currentScore = rescoreCurrentDecision(ctx);
            double currentWithMomentum = currentScore + difficulty.getDecisionMomentum();
            if (currentWithMomentum > best.score() && currentScore > 0.01) {
                // Stick with current decision — but we need to rebuild the ScoredAction
                // Use the current decision's natural score (the momentum is just tie-breaking)
                best = rebuildCurrentAction(ctx, currentScore);
            }
        }

        // Update roam state based on final decision
        updateRoamState(best.decision(), ctx);

        // Track blocking state
        currentlyBlocking = (best.decision() == BrainDecision.BLOCK);

        currentDecision = best.decision();
        return best;
    }

    // ========== Scoring Functions ==========

    /**
     * ATTACK_ENEMY: combatWeight × distanceFeasibility × targetAttractiveness × threatResponse × zoneRelevance
     */
    private double scoreAttackEnemy(EnemyInfo enemy, BrainContext ctx) {
        double combat = difficulty.getCombatWeight();

        // Distance feasibility: 1.0 at attackRange, falls off to 0 at chaseRange, 0 beyond
        double dist = enemy.distance;
        double attackRange = difficulty.getAttackRange();
        double chaseRange = difficulty.getChaseRange();
        if (dist > chaseRange) return 0;
        double distanceFeasibility = dist <= attackRange ? 1.0 : 1.0 - ((dist - attackRange) / (chaseRange - attackRange));

        // Target attractiveness: prefer wounded enemies
        double targetAttractiveness = 1.0 - (enemy.healthPercent * 0.4); // 1.0 for 0% HP, 0.6 for 100% HP

        // Threat response: bonus for enemies that have hit us
        double threatResponse = enemy.isThreat ? 1.3 : 1.0;
        // Extra bonus for active attackers (they're mid-swing, punish them)
        if (enemy.isAttacking && !enemy.isRangedAttacking) {
            threatResponse *= 1.1;
        }

        // Zone relevance: bonus for enemies contesting the zone
        double zoneRelevance = 1.0;
        if (ctx.objective != null && enemy.isInZone) {
            zoneRelevance = 1.4; // High priority to stop capturers
        }

        return combat * distanceFeasibility * targetAttractiveness * threatResponse * zoneRelevance;
    }

    /**
     * DEFEND_ZONE: objectiveWeight × onZoneBonus × enemyProximity × holdingValue
     * Only scored when bot is on the zone.
     */
    private double scoreDefendZone(EnemyInfo enemy, BrainContext ctx) {
        double obj = difficulty.getObjectiveWeight();

        // On-zone bonus: always 1.2 (we only call this when botInZone)
        double onZoneBonus = 1.2;

        // Enemy proximity: higher score for closer enemies (defend in place)
        double maxDefendDist = 10.0;
        if (enemy.distance > maxDefendDist) return 0;
        double enemyProximity = 1.0 - (enemy.distance / maxDefendDist);

        // Holding value: bonus when we're the controller (don't abandon the zone)
        double holdingValue = 1.0;
        if (ctx.objective.currentController() != null &&
            ctx.objective.currentController().equals(ctx.bot.getUniqueId())) {
            holdingValue = 1.3;
        }

        return obj * onZoneBonus * enemyProximity * holdingValue;
    }

    /**
     * BLOCK: selfPreservation × incomingMeleeUrgency × blockEnergy × proximityToAttacker × notMidSwing
     */
    private double scoreBlock(BrainContext ctx) {
        double selfPres = difficulty.getSelfPreservationWeight();

        // Need an attacking enemy to block
        EnemyInfo attacker = ctx.nearestAttackingEnemy;
        if (attacker == null) return 0;

        // Only block melee attacks (not ranged — strafe instead)
        if (attacker.isRangedAttacking) return 0;
        if (!attacker.isAttacking) return 0;

        // Proximity to attacker — KEY FIX: score drops sharply for distant enemies
        double maxBlockDist = difficulty.getAttackRange() + 2.0;
        if (attacker.distance > maxBlockDist) return 0;
        double proximityToAttacker = 1.0 - (attacker.distance / maxBlockDist);

        // Block energy: can't block without energy
        double energyFactor = blockEnergy >= difficulty.getBlockMinEnergy() ? 1.0 : 0;
        double energyLevel = blockEnergy / difficulty.getBlockMaxEnergy();

        // Not mid-swing: can't raise shield while attacking
        double notMidSwing = ctx.botIsAttacking ? 0 : 1.0;

        // Incoming urgency: higher when health is lower
        double incomingUrgency = 1.0 + (1.0 - ctx.botHealthPercent) * 0.5;

        return selfPres * incomingUrgency * energyFactor * energyLevel * proximityToAttacker * notMidSwing;
    }

    /**
     * GO_TO_OBJECTIVE: objectiveWeight × hasObjective × distanceToZone × notOnZone × survivalMod × inverseThreatPressure
     */
    private double scoreObjective(BrainContext ctx) {
        if (ctx.objective == null) return 0;

        double obj = difficulty.getObjectiveWeight();

        // Not on zone: go there
        double notOnZone = ctx.botInZone ? 0.1 : 1.0; // Tiny score when already there (centering)

        // Distance to zone center
        double distToZone = ctx.botPos != null ? ctx.botPos.distanceTo(ctx.objective.position()) : 0;
        // Urgency increases as we get closer (almost there, don't give up)
        double distanceFactor = distToZone > 1.5 ? 0.8 + 0.2 * Math.min(1.0, 5.0 / distToZone) : 1.0;

        // Survival modifier: reduce objective score when very low HP
        double survivalMod = ctx.botHealthPercent < difficulty.getRetreatThreshold() ? 0.5 : 1.0;

        // Inverse threat pressure: reduce objective urgency when under heavy fire
        double threatPressure = 1.0;
        if (ctx.activeThreatCount > 0) {
            threatPressure = 1.0 / (1.0 + ctx.activeThreatCount * 0.3);
        }

        return obj * notOnZone * distanceFactor * survivalMod * threatPressure;
    }

    /**
     * STRAFE_EVADE: selfPreservation × underRangedFire × healthPressure × notInMeleeRange × hasGoal
     */
    private double scoreStrafeEvade(BrainContext ctx) {
        double selfPres = difficulty.getSelfPreservationWeight();

        // Must be under ranged fire
        if (ctx.rangedThreatCount == 0) return 0;
        double underRangedFire = Math.min(1.0, ctx.rangedThreatCount * 0.6);

        // Health pressure: more evasive when wounded
        double healthPressure = 1.0 + (1.0 - ctx.botHealthPercent) * 0.5;

        // Not in melee range (if a melee enemy is close, fight instead of strafe)
        double notInMelee = 1.0;
        if (ctx.nearestEnemy != null && ctx.nearestEnemy.distance <= difficulty.getAttackRange()) {
            notInMelee = 0.2; // Greatly reduce strafe when melee enemy is close
        }

        // Has a goal (objective or enemy to strafe toward)
        double hasGoal = (ctx.objective != null || ctx.nearestEnemy != null) ? 1.0 : 0.3;

        return selfPres * underRangedFire * healthPressure * notInMelee * hasGoal;
    }

    /**
     * ROAM: base score when no threats, no objective, no nearby enemies.
     */
    private double scoreRoam(BrainContext ctx) {
        if (ctx.objective != null) return 0; // Objective modes don't roam
        if (ctx.activeThreatCount > 0) return 0;
        if (ctx.nearestEnemy != null && ctx.nearestEnemy.distance < difficulty.getChaseRange()) return 0;

        // Higher score after idling for a while
        double idleBonus = idleTicks >= IDLE_BEFORE_ROAM_TICKS ? 1.0 : 0.5;
        return 0.15 * idleBonus;
    }

    /**
     * Re-evaluates the score of the current decision (for momentum comparison).
     */
    private double rescoreCurrentDecision(BrainContext ctx) {
        return switch (currentDecision) {
            case COMBAT -> {
                double best = 0;
                for (EnemyInfo e : ctx.enemies) {
                    best = Math.max(best, scoreAttackEnemy(e, ctx));
                }
                yield best;
            }
            case DEFEND_ZONE -> {
                if (ctx.objective == null || !ctx.botInZone) yield 0;
                double best = 0;
                for (EnemyInfo e : ctx.enemies) {
                    best = Math.max(best, scoreDefendZone(e, ctx));
                }
                yield best;
            }
            case BLOCK -> scoreBlock(ctx);
            case OBJECTIVE -> scoreObjective(ctx);
            case STRAFE_EVADE -> scoreStrafeEvade(ctx);
            case ROAM -> scoreRoam(ctx);
            case IDLE -> 0.05;
        };
    }

    /**
     * Rebuilds a ScoredAction for the current decision with proper target.
     */
    private ScoredAction rebuildCurrentAction(BrainContext ctx, double score) {
        return switch (currentDecision) {
            case COMBAT -> {
                EnemyInfo bestTarget = null;
                double bestScore = 0;
                for (EnemyInfo e : ctx.enemies) {
                    double s = scoreAttackEnemy(e, ctx);
                    if (s > bestScore) {
                        bestScore = s;
                        bestTarget = e;
                    }
                }
                yield ScoredAction.of(BrainDecision.COMBAT, score, bestTarget);
            }
            case DEFEND_ZONE -> {
                EnemyInfo bestTarget = null;
                double bestScore = 0;
                for (EnemyInfo e : ctx.enemies) {
                    double s = scoreDefendZone(e, ctx);
                    if (s > bestScore) {
                        bestScore = s;
                        bestTarget = e;
                    }
                }
                yield ScoredAction.of(BrainDecision.DEFEND_ZONE, score, bestTarget);
            }
            default -> ScoredAction.of(currentDecision, score);
        };
    }

    /**
     * Updates roam/idle tick state based on the chosen decision.
     */
    private void updateRoamState(BrainDecision decision, BrainContext ctx) {
        if (decision == BrainDecision.ROAM) {
            if (roamWaypoint == null && ctx.arenaBounds != null) {
                roamWaypoint = pickRandomWaypoint(ctx.arenaBounds);
                roamTicksRemaining = ROAM_MAX_TICKS;
            } else if (roamWaypoint != null) {
                roamTicksRemaining--;
                if (roamTicksRemaining <= 0 ||
                    (ctx.botPos != null && ctx.botPos.distanceTo(roamWaypoint) < ROAM_ARRIVAL_DIST)) {
                    roamWaypoint = null;
                    idleTicks = 0;
                }
            }
        } else if (decision == BrainDecision.IDLE) {
            idleTicks++;
            roamWaypoint = null;
        } else {
            // Any active decision resets roam/idle
            idleTicks = 0;
            roamWaypoint = null;
        }
    }

    // ========== Getters ==========

    public Position getRoamWaypoint() { return roamWaypoint; }
    public BrainDecision getCurrentDecision() { return currentDecision; }
    public double getBlockEnergy() { return blockEnergy; }
    public Map<UUID, ThreatEntry> getThreats() { return threats; }

    public boolean hasActiveThreats() {
        return !threats.isEmpty();
    }

    // ========== Threat System ==========

    /**
     * Registers an attacker as a threat with type and damage info.
     */
    public void registerThreat(UUID attackerUuid, ThreatType type, double damage) {
        if (attackerUuid == null) return;
        ThreatEntry existing = threats.get(attackerUuid);
        if (existing != null) {
            existing.update(currentTick, type, damage);
        } else {
            threats.put(attackerUuid, new ThreatEntry(attackerUuid, currentTick, type, damage));
        }
    }

    /**
     * Removes stale threats: dead, timed out, or too far (using difficulty params).
     */
    private void pruneThreats(BrainContext ctx) {
        if (threats.isEmpty()) return;

        long timeout = difficulty.getThreatMemoryTicks();
        double maxDist = difficulty.getThreatDistanceMax();

        Iterator<Map.Entry<UUID, ThreatEntry>> it = threats.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ThreatEntry> entry = it.next();
            ThreatEntry threat = entry.getValue();

            // Timeout check
            if ((currentTick - threat.getLastHitTick()) > timeout) {
                it.remove();
                continue;
            }

            // Dead check
            if (ctx.match != null) {
                Participant attacker = ctx.match.getParticipant(threat.getAttackerUuid());
                if (attacker == null || !attacker.isAlive()) {
                    it.remove();
                    continue;
                }
            }

            // Distance check — only prune if we can resolve position
            if (ctx.botPos != null) {
                Position attackerPos = findAttackerPosition(threat.getAttackerUuid(), ctx);
                if (attackerPos != null) {
                    double dist = ctx.botPos.distanceTo(attackerPos);
                    if (dist > maxDist) {
                        it.remove();
                    }
                }
                // If position can't be resolved, keep the threat until timeout
            }
        }
    }

    /**
     * Finds an attacker's position from the enemies list in context.
     */
    private Position findAttackerPosition(UUID attackerUuid, BrainContext ctx) {
        for (EnemyInfo enemy : ctx.enemies) {
            if (enemy.participant.getUniqueId().equals(attackerUuid)) {
                return enemy.position;
            }
        }
        return null;
    }

    /**
     * Resets brain state (for respawn).
     */
    public void reset() {
        currentDecision = BrainDecision.IDLE;
        roamWaypoint = null;
        roamTicksRemaining = 0;
        idleTicks = 0;
        blockEnergy = difficulty.getBlockMaxEnergy();
        currentlyBlocking = false;
        threats.clear();
        currentTick = 0;
    }

    /**
     * Picks a random position within arena bounds (with margin from edges).
     */
    private Position pickRandomWaypoint(ArenaConfig.Bounds bounds) {
        double minX = bounds.getMinX() + ROAM_MARGIN;
        double maxX = bounds.getMaxX() - ROAM_MARGIN;
        double minZ = bounds.getMinZ() + ROAM_MARGIN;
        double maxZ = bounds.getMaxZ() - ROAM_MARGIN;

        if (minX >= maxX) {
            minX = bounds.getMinX();
            maxX = bounds.getMaxX();
        }
        if (minZ >= maxZ) {
            minZ = bounds.getMinZ();
            maxZ = bounds.getMaxZ();
        }

        double x = minX + random.nextDouble() * (maxX - minX);
        double z = minZ + random.nextDouble() * (maxZ - minZ);
        double y = (bounds.getMinY() + bounds.getMaxY()) / 2.0;

        return new Position(x, y, z);
    }
}
