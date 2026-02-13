package de.ragesith.hyarena2.bot;

/**
 * Bot difficulty levels with configurable parameters.
 * Each difficulty affects combat effectiveness, behavior, and utility AI weights.
 */
public enum BotDifficulty {
    EASY(
        1500,   // reactionTimeMs
        0.40,   // aimAccuracy (40%)
        3.0,    // attackRange
        15.0,   // chaseRange
        0.8,    // movementSpeedMultiplier
        0.8,    // healthMultiplier
        1000,   // attackCooldownMs
        0.30,   // retreatThreshold (30% health)
        15.0,   // baseDamage
        0.20,   // blockProbability (20%) — legacy path
        40.0,   // blockMaxEnergy — brain path
        20.0,   // blockMinEnergy — brain path
        // Utility AI weights
        0.6,    // combatWeight
        0.5,    // objectiveWeight
        0.4,    // selfPreservationWeight
        0.08,   // decisionMomentum
        80,     // threatMemoryTicks (4s at 20 TPS)
        20.0    // threatDistanceMax
    ),
    MEDIUM(
        800,    // reactionTimeMs
        0.65,   // aimAccuracy (65%)
        4.0,    // attackRange
        25.0,   // chaseRange
        1.0,    // movementSpeedMultiplier
        1.0,    // healthMultiplier
        900,    // attackCooldownMs
        0.25,   // retreatThreshold (25% health)
        20.0,   // baseDamage
        0.40,   // blockProbability (40%) — legacy path
        70.0,   // blockMaxEnergy — brain path
        15.0,   // blockMinEnergy — brain path
        // Utility AI weights
        0.75,   // combatWeight
        0.7,    // objectiveWeight
        0.55,   // selfPreservationWeight
        0.05,   // decisionMomentum
        120,    // threatMemoryTicks (6s at 20 TPS)
        40.0    // threatDistanceMax
    ),
    HARD(
        300,    // reactionTimeMs
        0.90,   // aimAccuracy (90%)
        5.0,    // attackRange
        40.0,   // chaseRange
        1.2,    // movementSpeedMultiplier
        1.4,    // healthMultiplier
        600,    // attackCooldownMs
        0.15,   // retreatThreshold (15% health)
        28.0,   // baseDamage
        0.70,   // blockProbability (70%) — legacy path
        100.0,  // blockMaxEnergy — brain path
        10.0,   // blockMinEnergy — brain path
        // Utility AI weights
        0.9,    // combatWeight
        0.85,   // objectiveWeight
        0.7,    // selfPreservationWeight
        0.03,   // decisionMomentum
        200,    // threatMemoryTicks (10s at 20 TPS)
        60.0    // threatDistanceMax
    );

    private final int reactionTimeMs;
    private final double aimAccuracy;
    private final double attackRange;
    private final double chaseRange;
    private final double movementSpeedMultiplier;
    private final double healthMultiplier;
    private final int attackCooldownMs;
    private final double retreatThreshold;
    private final double baseDamage;
    private final double blockProbability;
    private final double blockMaxEnergy;
    private final double blockMinEnergy;

    // Utility AI weights
    private final double combatWeight;
    private final double objectiveWeight;
    private final double selfPreservationWeight;
    private final double decisionMomentum;
    private final int threatMemoryTicks;
    private final double threatDistanceMax;

    BotDifficulty(int reactionTimeMs, double aimAccuracy, double attackRange,
                  double chaseRange, double movementSpeedMultiplier, double healthMultiplier,
                  int attackCooldownMs, double retreatThreshold, double baseDamage,
                  double blockProbability, double blockMaxEnergy, double blockMinEnergy,
                  double combatWeight, double objectiveWeight, double selfPreservationWeight,
                  double decisionMomentum, int threatMemoryTicks, double threatDistanceMax) {
        this.reactionTimeMs = reactionTimeMs;
        this.aimAccuracy = aimAccuracy;
        this.attackRange = attackRange;
        this.chaseRange = chaseRange;
        this.movementSpeedMultiplier = movementSpeedMultiplier;
        this.healthMultiplier = healthMultiplier;
        this.attackCooldownMs = attackCooldownMs;
        this.retreatThreshold = retreatThreshold;
        this.baseDamage = baseDamage;
        this.blockProbability = blockProbability;
        this.blockMaxEnergy = blockMaxEnergy;
        this.blockMinEnergy = blockMinEnergy;
        this.combatWeight = combatWeight;
        this.objectiveWeight = objectiveWeight;
        this.selfPreservationWeight = selfPreservationWeight;
        this.decisionMomentum = decisionMomentum;
        this.threatMemoryTicks = threatMemoryTicks;
        this.threatDistanceMax = threatDistanceMax;
    }

    public int getReactionTimeMs() { return reactionTimeMs; }
    public double getAimAccuracy() { return aimAccuracy; }
    public double getAttackRange() { return attackRange; }
    public double getChaseRange() { return chaseRange; }
    public double getMovementSpeedMultiplier() { return movementSpeedMultiplier; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public int getAttackCooldownMs() { return attackCooldownMs; }
    public double getRetreatThreshold() { return retreatThreshold; }
    public double getBaseDamage() { return baseDamage; }
    public double getBlockProbability() { return blockProbability; }
    public double getBlockMaxEnergy() { return blockMaxEnergy; }
    public double getBlockMinEnergy() { return blockMinEnergy; }

    // Utility AI weight getters
    public double getCombatWeight() { return combatWeight; }
    public double getObjectiveWeight() { return objectiveWeight; }
    public double getSelfPreservationWeight() { return selfPreservationWeight; }
    public double getDecisionMomentum() { return decisionMomentum; }
    public int getThreatMemoryTicks() { return threatMemoryTicks; }
    public double getThreatDistanceMax() { return threatDistanceMax; }

    /**
     * Calculates the max health based on base health and difficulty multiplier.
     * @param baseHealth the base health value (typically 100)
     * @return adjusted max health
     */
    public double calculateMaxHealth(double baseHealth) {
        return baseHealth * healthMultiplier;
    }

    /**
     * Parses a difficulty from string, defaulting to MEDIUM if invalid.
     */
    public static BotDifficulty fromString(String value) {
        if (value == null || value.isEmpty()) {
            return MEDIUM;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
