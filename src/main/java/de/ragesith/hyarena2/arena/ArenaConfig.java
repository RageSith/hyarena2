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
    private List<String> randomKitPool; // Kit IDs for random assignment (Kit Roulette)

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
    public List<String> getRandomKitPool() { return randomKitPool; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public void setWaitTimeSeconds(int waitTimeSeconds) { this.waitTimeSeconds = waitTimeSeconds; }
    public void setBotDifficulty(String botDifficulty) { this.botDifficulty = botDifficulty; }
    public void setBotModelId(String botModelId) { this.botModelId = botModelId; }
    public void setAutoFillEnabled(boolean autoFillEnabled) { this.autoFillEnabled = autoFillEnabled; }
    public void setAutoFillDelaySeconds(int autoFillDelaySeconds) { this.autoFillDelaySeconds = autoFillDelaySeconds; }
    public void setMinRealPlayers(int minRealPlayers) { this.minRealPlayers = minRealPlayers; }
    public void setMatchDurationSeconds(int matchDurationSeconds) { this.matchDurationSeconds = matchDurationSeconds; }
    public void setKillTarget(int killTarget) { this.killTarget = killTarget; }
    public void setRespawnDelaySeconds(int respawnDelaySeconds) { this.respawnDelaySeconds = respawnDelaySeconds; }
    public void setAllowedKits(List<String> allowedKits) { this.allowedKits = allowedKits; }
    public void setSpawnPoints(List<SpawnPoint> spawnPoints) { this.spawnPoints = spawnPoints; }
    public void setBounds(Bounds bounds) { this.bounds = bounds; }
    public void setCaptureZones(List<CaptureZone> captureZones) { this.captureZones = captureZones; }
    public void setScoreTarget(int scoreTarget) { this.scoreTarget = scoreTarget; }
    public void setZoneRotationSeconds(int zoneRotationSeconds) { this.zoneRotationSeconds = zoneRotationSeconds; }
    public void setRandomKitPool(List<String> randomKitPool) { this.randomKitPool = randomKitPool; }

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

        // Normalize bounds so min < max on all axes (Gson may deserialize swapped corners)
        bounds.normalize();

        // Normalize capture zones too
        if (captureZones != null) {
            for (CaptureZone zone : captureZones) {
                double tmpMinX = Math.min(zone.getMinX(), zone.getMaxX());
                double tmpMaxX = Math.max(zone.getMinX(), zone.getMaxX());
                double tmpMinY = Math.min(zone.getMinY(), zone.getMaxY());
                double tmpMaxY = Math.max(zone.getMinY(), zone.getMaxY());
                double tmpMinZ = Math.min(zone.getMinZ(), zone.getMaxZ());
                double tmpMaxZ = Math.max(zone.getMinZ(), zone.getMaxZ());
                zone.setMinX(tmpMinX); zone.setMaxX(tmpMaxX);
                zone.setMinY(tmpMinY); zone.setMaxY(tmpMaxY);
                zone.setMinZ(tmpMinZ); zone.setMaxZ(tmpMaxZ);
            }
        }

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

        public SpawnPoint() {}

        public SpawnPoint(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }

        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
        public void setZ(double z) { this.z = z; }
        public void setYaw(float yaw) { this.yaw = yaw; }
        public void setPitch(float pitch) { this.pitch = pitch; }
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

        public CaptureZone() {}

        public CaptureZone(String displayName, double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ) {
            this.displayName = displayName;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public String getDisplayName() { return displayName; }
        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }

        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public void setMinX(double minX) { this.minX = minX; }
        public void setMinY(double minY) { this.minY = minY; }
        public void setMinZ(double minZ) { this.minZ = minZ; }
        public void setMaxX(double maxX) { this.maxX = maxX; }
        public void setMaxY(double maxY) { this.maxY = maxY; }
        public void setMaxZ(double maxZ) { this.maxZ = maxZ; }

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

        public Bounds() {}

        public Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }

        /**
         * Normalizes bounds so min &lt; max on all axes.
         * Called after deserialization (Gson bypasses constructors).
         */
        public void normalize() {
            double tmpMinX = Math.min(minX, maxX), tmpMaxX = Math.max(minX, maxX);
            double tmpMinY = Math.min(minY, maxY), tmpMaxY = Math.max(minY, maxY);
            double tmpMinZ = Math.min(minZ, maxZ), tmpMaxZ = Math.max(minZ, maxZ);
            this.minX = tmpMinX; this.maxX = tmpMaxX;
            this.minY = tmpMinY; this.maxY = tmpMaxY;
            this.minZ = tmpMinZ; this.maxZ = tmpMaxZ;
        }

        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }

        public void setMinX(double minX) { this.minX = minX; }
        public void setMinY(double minY) { this.minY = minY; }
        public void setMinZ(double minZ) { this.minZ = minZ; }
        public void setMaxX(double maxX) { this.maxX = maxX; }
        public void setMaxY(double maxY) { this.maxY = maxY; }
        public void setMaxZ(double maxZ) { this.maxZ = maxZ; }

        /**
         * Checks if a position is within the XZ bounds (ignoring Y).
         * Used for arena cleanup where arenas in the same world never overlap vertically.
         */
        public boolean containsXZ(double x, double z) {
            double loX = Math.min(minX, maxX), hiX = Math.max(minX, maxX);
            double loZ = Math.min(minZ, maxZ), hiZ = Math.max(minZ, maxZ);
            return x >= loX && x <= hiX && z >= loZ && z <= hiZ;
        }

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
