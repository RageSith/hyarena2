package de.ragesith.hyarena2.economy;

/**
 * Return type for match reward calculations.
 * Holds the AP and honor earned from a single match.
 */
public class MatchRewardResult {
    private final int apEarned;
    private final double honorEarned;

    public MatchRewardResult(int apEarned, double honorEarned) {
        this.apEarned = apEarned;
        this.honorEarned = honorEarned;
    }

    public int getApEarned() {
        return apEarned;
    }

    public double getHonorEarned() {
        return honorEarned;
    }
}
