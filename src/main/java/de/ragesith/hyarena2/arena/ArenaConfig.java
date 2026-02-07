package de.ragesith.hyarena2.arena;

import java.util.List;

/**
 * Configuration data for an arena, loaded from JSON files in config/arenas/
 */
public class ArenaConfig {
    private String id;
    private String displayName;
    private String worldName;
    private String gameMode;
    private int minPlayers;
    private int maxPlayers;
    private int waitTimeSeconds;
    private String botDifficulty;
    private String botModelId;
    private boolean autoFillEnabled;
    private int autoFillDelaySeconds = 30;
    private int minRealPlayers = 1;
    private int matchDurationSeconds = 300; // Default 5 minutes
    private int killTarget = 0; // Kills to win (0 = unused, for elimination modes)
    private int respawnDelaySeconds = 3; // Seconds before respawn
    private List<String> allowedKits;
    private List<SpawnPoint> spawnPoints;
    private Bounds bounds;
    private List<CaptureZone> captureZones;
    private int scoreTarget = 0; // Control-seconds needed to win (0 = unused)
    private int zoneRotationSeconds = 60; // Rotation interval for multiple zones

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getWorldName() { return worldName; }
    public String getGameMode() { return gameMode; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getWaitTimeSeconds() { return waitTimeSeconds; }
    public String getBotDifficulty() { return botDifficulty; }
    public String getBotModelId() { return botModelId; }
    public boolean isAutoFillEnabled() { return autoFillEnabled; }
    public int getAutoFillDelaySeconds() { return autoFillDelaySeconds; }
    public int getMinRealPlayers() { return minRealPlayers; }
    public int getMatchDurationSeconds() { return matchDurationSeconds; }
    public int getKillTarget() { return killTarget; }
    public int getRespawnDelaySeconds() { return respawnDelaySeconds; }
    public List<String> getAllowedKits() { return allowedKits; }
    public List<SpawnPoint> getSpawnPoints() { return spawnPoints; }
    public Bounds getBounds() { return bounds; }
    public List<CaptureZone> getCaptureZones() { return captureZones; }
    public int getScoreTarget() { return scoreTarget; }
    public int getZoneRotationSeconds() { return zoneRotationSeconds; }

    /**
     * Validates the arena configuration
     * @return true if valid, false otherwise
     */
    public boolean validate() {
        if (id == null || id.isEmpty()) return false;
        if (displayName == null || displayName.isEmpty()) return false;
        if (worldName == null || worldName.isEmpty()) return false;
        if (gameMode == null || gameMode.isEmpty()) return false;
        if (minPlayers <= 0 || maxPlayers <= 0) return false;
        if (minPlayers > maxPlayers) return false;
        if (waitTimeSeconds < 0) return false;
        if (spawnPoints == null || spawnPoints.size() < maxPlayers) return false;
        if (bounds == null) return false;
        return true;
    }

    /**
     * Represents a spawn point in the arena
     */
    public static class SpawnPoint {
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }

    /**
     * Represents a capture zone for King of the Hill mode
     */
    public static class CaptureZone {
        private String displayName;
        private double minX;
        private double minY;
        private double minZ;
        private double maxX;
        private double maxY;
        private double maxZ;

        public String getDisplayName() { return displayName; }
        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }

        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX &&
                   y >= minY && y <= maxY &&
                   z >= minZ && z <= maxZ;
        }
    }

    /**
     * Represents the arena boundaries
     */
    public static class Bounds {
        private double minX;
        private double minY;
        private double minZ;
        private double maxX;
        private double maxY;
        private double maxZ;

        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }

        /**
         * Checks if a position is within the bounds
         */
        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX &&
                   y >= minY && y <= maxY &&
                   z >= minZ && z <= maxZ;
        }

        /**
         * Checks if a position is within the bounds shrunk by margin on each side.
         */
        public boolean containsWithMargin(double x, double y, double z, double margin) {
            return x >= minX + margin && x <= maxX - margin &&
                   y >= minY + margin && y <= maxY - margin &&
                   z >= minZ + margin && z <= maxZ - margin;
        }

        /**
         * Clamps a position to be inside the bounds shrunk by margin.
         * Returns a double[3] with {clampedX, clampedY, clampedZ}.
         */
        public double[] clampInside(double x, double y, double z, double margin) {
            double effectiveMinX = minX + margin;
            double effectiveMaxX = maxX - margin;
            double effectiveMinY = minY + margin;
            double effectiveMaxY = maxY - margin;
            double effectiveMinZ = minZ + margin;
            double effectiveMaxZ = maxZ - margin;
            return new double[] {
                Math.max(effectiveMinX, Math.min(x, effectiveMaxX)),
                Math.max(effectiveMinY, Math.min(y, effectiveMaxY)),
                Math.max(effectiveMinZ, Math.min(z, effectiveMaxZ))
            };
        }
    }
}
