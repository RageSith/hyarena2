package de.ragesith.hyarena2.economy;

/**
 * Configuration for a single honor rank.
 * Loaded from economy.json as part of EconomyConfig.
 */
public class HonorRankConfig {
    private double threshold;
    private String id;
    private String displayName;
    private String permission;

    public HonorRankConfig() {}

    public HonorRankConfig(double threshold, String id, String displayName, String permission) {
        this.threshold = threshold;
        this.id = id;
        this.displayName = displayName;
        this.permission = permission;
    }

    public double getThreshold() {
        return threshold;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermission() {
        return permission;
    }
}
