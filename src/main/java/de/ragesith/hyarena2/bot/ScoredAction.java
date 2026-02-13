package de.ragesith.hyarena2.bot;

/**
 * Result of BotBrain.evaluate() — the highest-scoring action with its target.
 */
public record ScoredAction(
    BrainDecision decision,
    double score,
    EnemyInfo target   // null for non-targeted actions (ROAM, IDLE, OBJECTIVE, STRAFE_EVADE)
) {
    public static ScoredAction of(BrainDecision decision, double score) {
        return new ScoredAction(decision, score, null);
    }

    public static ScoredAction of(BrainDecision decision, double score, EnemyInfo target) {
        return new ScoredAction(decision, score, target);
    }
}
