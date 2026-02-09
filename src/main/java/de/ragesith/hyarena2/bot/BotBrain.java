package de.ragesith.hyarena2.bot;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.config.Position;

import java.util.Random;

/**
 * Priority-based decision engine for bot AI.
 * One instance per bot (stored on BotParticipant).
 * Each tick, evaluates all possible behaviors and returns the highest-scoring BrainDecision.
 */
public class BotBrain {

    private final BotDifficulty difficulty;
    private final Random random = new Random();

    // Current decision state
    private BrainDecision currentDecision = BrainDecision.IDLE;

    // Roam state
    private Position roamWaypoint;          // Current roam target (null = needs new one)
    private int roamTicksRemaining;         // Ticks left walking to waypoint
    private int idleTicks;                  // Ticks spent idle before roaming

    // Block energy system
    private double blockEnergy;             // Current energy (starts at max)
    private boolean currentlyBlocking;      // True while in Block state
    private String preBlockDecision;        // NPC state to restore after block ends

    // Constants
    private static final double BLOCK_DRAIN_RATE = 3.0;   // Energy lost per tick while blocking
    private static final double BLOCK_REGEN_RATE = 1.5;   // Energy gained per tick while not blocking
    private static final int IDLE_BEFORE_ROAM_TICKS = 40;  // ~2s idle then roam
    private static final int ROAM_MAX_TICKS = 100;         // ~5s max per waypoint
    private static final double ROAM_ARRIVAL_DIST = 2.0;   // Close enough to waypoint
    private static final double ROAM_MARGIN = 3.0;         // Margin from arena edges for waypoints

    // Zone ranges (match legacy BotManager constants)
    private static final double DEFEND_RANGE = 3.0;
    private static final double WATCHOUT_RANGE = 10.0;

    public BotBrain(BotDifficulty difficulty) {
        this.difficulty = difficulty;
        this.blockEnergy = difficulty.getBlockMaxEnergy();
    }

    /**
     * Main evaluation method — called every tick from BotManager.
     * Returns the highest-priority applicable decision.
     */
    public BrainDecision evaluate(BrainContext ctx) {
        // 1. Block energy regen when NOT blocking
        if (!currentlyBlocking) {
            blockEnergy = Math.min(difficulty.getBlockMaxEnergy(), blockEnergy + BLOCK_REGEN_RATE);
        }

        // 2. Currently blocking — drain energy, check exit conditions
        if (currentlyBlocking) {
            blockEnergy -= BLOCK_DRAIN_RATE;

            boolean shouldExitBlock = !ctx.enemyIsAttacking || blockEnergy <= 0;
            if (shouldExitBlock) {
                currentlyBlocking = false;
                // Restore previous decision
                currentDecision = preBlockDecision != null ? decisionFromNpcState(preBlockDecision) : BrainDecision.COMBAT;
                preBlockDecision = null;
                return currentDecision;
            }

            // Stay blocking
            currentDecision = BrainDecision.BLOCK;
            return currentDecision;
        }

        // 3. Check reactive block — enemy is attacking, bot has energy, bot not mid-swing
        if (ctx.enemyIsAttacking && !ctx.botIsAttacking
                && blockEnergy >= difficulty.getBlockMinEnergy()
                && ctx.nearestEnemy != null) {
            preBlockDecision = npcStateFromDecision(currentDecision);
            currentlyBlocking = true;
            blockEnergy = Math.max(0, blockEnergy); // Ensure non-negative
            currentDecision = BrainDecision.BLOCK;
            return currentDecision;
        }

        // 4. Check combat — enemy within chase range
        if (ctx.nearestEnemy != null && ctx.nearestEnemy.distance <= difficulty.getChaseRange()) {
            if (ctx.objective != null) {
                if (!ctx.botInZone) {
                    // Not on zone — never fight, always walk to zone.
                    // BLOCK (step 3) handles reactive defense if attacked en route.
                } else {
                    // On zone — only COMBAT if enemies are contesting (also in zone).
                    // Other enemies fall through to step 5 for DEFEND_ZONE (fight in place).
                    if (!ctx.enemiesInZone.isEmpty()) {
                        idleTicks = 0;
                        roamWaypoint = null;
                        currentDecision = BrainDecision.COMBAT;
                        return currentDecision;
                    }
                }
                // Fall through to step 5 for objective-aware behavior
            } else {
                // Non-objective mode — fight normally at chase range
                idleTicks = 0;
                roamWaypoint = null;
                currentDecision = BrainDecision.COMBAT;
                return currentDecision;
            }
        }

        // 5. Check objective (if present)
        if (ctx.objective != null) {
            if (ctx.botInZone) {
                // On zone — check for nearby enemies
                if (ctx.nearestEnemy != null) {
                    if (ctx.nearestEnemy.distance <= DEFEND_RANGE) {
                        idleTicks = 0;
                        currentDecision = BrainDecision.DEFEND_ZONE;
                        return currentDecision;
                    }
                    if (ctx.nearestEnemy.distance <= WATCHOUT_RANGE) {
                        idleTicks = 0;
                        currentDecision = BrainDecision.DEFEND_ZONE;
                        return currentDecision;
                    }
                }
                // On zone, no nearby enemies — idle and score
                idleTicks = 0;
                roamWaypoint = null;
                currentDecision = BrainDecision.IDLE;
                return currentDecision;
            } else {
                // Not on zone — walk there
                idleTicks = 0;
                roamWaypoint = null;
                currentDecision = BrainDecision.OBJECTIVE;
                return currentDecision;
            }
        }

        // 6. Roaming — no enemies visible, no objective
        if (roamWaypoint != null) {
            // Currently roaming — check arrival or timeout
            roamTicksRemaining--;
            if (ctx.botPos != null && ctx.botPos.distanceTo(roamWaypoint) < ROAM_ARRIVAL_DIST) {
                // Arrived at waypoint
                roamWaypoint = null;
                idleTicks = 0;
                currentDecision = BrainDecision.IDLE;
                return currentDecision;
            }
            if (roamTicksRemaining <= 0) {
                // Timeout
                roamWaypoint = null;
                idleTicks = 0;
                currentDecision = BrainDecision.IDLE;
                return currentDecision;
            }
            currentDecision = BrainDecision.ROAM;
            return currentDecision;
        }

        // 7. Idle — wait before starting a new roam
        idleTicks++;
        if (idleTicks >= IDLE_BEFORE_ROAM_TICKS && ctx.arenaBounds != null) {
            roamWaypoint = pickRandomWaypoint(ctx.arenaBounds);
            roamTicksRemaining = ROAM_MAX_TICKS;
            idleTicks = 0;
            currentDecision = BrainDecision.ROAM;
            return currentDecision;
        }

        currentDecision = BrainDecision.IDLE;
        return currentDecision;
    }

    /**
     * Gets the current roam waypoint (for marker entity placement).
     */
    public Position getRoamWaypoint() {
        return roamWaypoint;
    }

    /**
     * Gets the current decision.
     */
    public BrainDecision getCurrentDecision() {
        return currentDecision;
    }

    /**
     * Gets the NPC state name to restore after blocking ends.
     */
    public String getPreBlockDecision() {
        return preBlockDecision;
    }

    /**
     * Gets current block energy.
     */
    public double getBlockEnergy() {
        return blockEnergy;
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
        preBlockDecision = null;
    }

    /**
     * Picks a random position within arena bounds (with margin from edges).
     */
    private Position pickRandomWaypoint(ArenaConfig.Bounds bounds) {
        double minX = bounds.getMinX() + ROAM_MARGIN;
        double maxX = bounds.getMaxX() - ROAM_MARGIN;
        double minZ = bounds.getMinZ() + ROAM_MARGIN;
        double maxZ = bounds.getMaxZ() - ROAM_MARGIN;

        // Clamp if arena is too small for margin
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
        // Use midpoint Y of bounds
        double y = (bounds.getMinY() + bounds.getMaxY()) / 2.0;

        return new Position(x, y, z);
    }

    /**
     * Maps a BrainDecision to the NPC state name string (for saving pre-block state).
     */
    private static String npcStateFromDecision(BrainDecision decision) {
        return switch (decision) {
            case COMBAT -> "Combat";
            case DEFEND_ZONE -> "Defend";
            case OBJECTIVE -> "Follow";
            case ROAM -> "Follow";
            case IDLE -> "Idle";
            case BLOCK -> "Block";
        };
    }

    /**
     * Maps an NPC state name back to a BrainDecision (for restoring after block).
     */
    private static BrainDecision decisionFromNpcState(String npcState) {
        return switch (npcState) {
            case "Combat" -> BrainDecision.COMBAT;
            case "Defend", "Watchout" -> BrainDecision.DEFEND_ZONE;
            case "Follow" -> BrainDecision.COMBAT; // After block, default to combat
            case "Idle" -> BrainDecision.IDLE;
            default -> BrainDecision.COMBAT;
        };
    }
}
