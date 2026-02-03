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
    private List<String> allowedKits;
    private List<SpawnPoint> spawnPoints;
    private Bounds bounds;

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
    public List<String> getAllowedKits() { return allowedKits; }
    public List<SpawnPoint> getSpawnPoints() { return spawnPoints; }
    public Bounds getBounds() { return bounds; }

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
    }
}
