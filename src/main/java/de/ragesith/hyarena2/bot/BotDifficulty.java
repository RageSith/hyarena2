package de.ragesith.hyarena2.bot;

/**
 * Bot difficulty levels with configurable parameters.
 * Each difficulty affects combat effectiveness and behavior.
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
        15.0    // baseDamage
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
        20.0    // baseDamage
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
        28.0    // baseDamage
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

    BotDifficulty(int reactionTimeMs, double aimAccuracy, double attackRange,
                  double chaseRange, double movementSpeedMultiplier, double healthMultiplier,
                  int attackCooldownMs, double retreatThreshold, double baseDamage) {
        this.reactionTimeMs = reactionTimeMs;
        this.aimAccuracy = aimAccuracy;
        this.attackRange = attackRange;
        this.chaseRange = chaseRange;
        this.movementSpeedMultiplier = movementSpeedMultiplier;
        this.healthMultiplier = healthMultiplier;
        this.attackCooldownMs = attackCooldownMs;
        this.retreatThreshold = retreatThreshold;
        this.baseDamage = baseDamage;
    }

    /**
     * Time in milliseconds before bot reacts to events.
     * Lower = faster reactions.
     */
    public int getReactionTimeMs() {
        return reactionTimeMs;
    }

    /**
     * Probability (0.0-1.0) that an attack will hit.
     * Higher = more accurate.
     */
    public double getAimAccuracy() {
        return aimAccuracy;
    }

    /**
     * Maximum distance (blocks) at which bot can attack.
     */
    public double getAttackRange() {
        return attackRange;
    }

    /**
     * Maximum distance (blocks) at which bot will chase targets.
     */
    public double getChaseRange() {
        return chaseRange;
    }

    /**
     * Multiplier for movement speed.
     * 1.0 = normal speed.
     */
    public double getMovementSpeedMultiplier() {
        return movementSpeedMultiplier;
    }

    /**
     * Multiplier for max health.
     * 1.0 = base 20 HP (100 internal units).
     */
    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    /**
     * Time in milliseconds between attacks.
     */
    public int getAttackCooldownMs() {
        return attackCooldownMs;
    }

    /**
     * Health percentage (0.0-1.0) at which bot will retreat.
     */
    public double getRetreatThreshold() {
        return retreatThreshold;
    }

    /**
     * Base damage per hit.
     */
    public double getBaseDamage() {
        return baseDamage;
    }

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
