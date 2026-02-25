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
    private boolean kitRouletteSwapOnKill = true; // Swap kit when getting a kill
    private boolean kitRouletteSwapOnRespawn = false; // Swap kit on respawn
    private List<SpawnPoint> waveSpawnPoints; // Spawn points for wave defense enemy bots
    private List<SpawnPoint> navWaypoints; // Nav waypoints for bot pathfinding on multi-level maps
    private int waveBonusSecondsPerKill = 2; // Bonus seconds added per wave mob killed
    private int waveBonusSecondsPerWaveClear = 60; // Bonus seconds added per wave cleared

    // SpeedRun fields
    private CaptureZone startZone;
    private CaptureZone finishZone;
    private List<CaptureZone> checkpoints;
    private double killPlaneY = -64;
    private int maxRespawns = 3;

    // Spleef fields
    private List<SpleefFloor> spleefFloors;
    private double spleefEliminationY = -64;

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
    public boolean isKitRouletteSwapOnKill() { return kitRouletteSwapOnKill; }
    public boolean isKitRouletteSwapOnRespawn() { return kitRouletteSwapOnRespawn; }
    public List<SpawnPoint> getWaveSpawnPoints() { return waveSpawnPoints; }
    public List<SpawnPoint> getNavWaypoints() { return navWaypoints; }
    public int getWaveBonusSecondsPerKill() { return waveBonusSecondsPerKill; }
    public int getWaveBonusSecondsPerWaveClear() { return waveBonusSecondsPerWaveClear; }
    public CaptureZone getStartZone() { return startZone; }
    public CaptureZone getFinishZone() { return finishZone; }
    public List<CaptureZone> getCheckpoints() { return checkpoints; }
    public double getKillPlaneY() { return killPlaneY; }
    public int getMaxRespawns() { return maxRespawns; }
    public List<SpleefFloor> getSpleefFloors() { return spleefFloors; }
    public double getSpleefEliminationY() { return spleefEliminationY; }

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
    public void setKitRouletteSwapOnKill(boolean kitRouletteSwapOnKill) { this.kitRouletteSwapOnKill = kitRouletteSwapOnKill; }
    public void setKitRouletteSwapOnRespawn(boolean kitRouletteSwapOnRespawn) { this.kitRouletteSwapOnRespawn = kitRouletteSwapOnRespawn; }
    public void setWaveSpawnPoints(List<SpawnPoint> waveSpawnPoints) { this.waveSpawnPoints = waveSpawnPoints; }
    public void setNavWaypoints(List<SpawnPoint> navWaypoints) { this.navWaypoints = navWaypoints; }
    public void setWaveBonusSecondsPerKill(int waveBonusSecondsPerKill) { this.waveBonusSecondsPerKill = waveBonusSecondsPerKill; }
    public void setWaveBonusSecondsPerWaveClear(int waveBonusSecondsPerWaveClear) { this.waveBonusSecondsPerWaveClear = waveBonusSecondsPerWaveClear; }
    public void setStartZone(CaptureZone startZone) { this.startZone = startZone; }
    public void setFinishZone(CaptureZone finishZone) { this.finishZone = finishZone; }
    public void setCheckpoints(List<CaptureZone> checkpoints) { this.checkpoints = checkpoints; }
    public void setKillPlaneY(double killPlaneY) { this.killPlaneY = killPlaneY; }
    public void setMaxRespawns(int maxRespawns) { this.maxRespawns = maxRespawns; }
    public void setSpleefFloors(List<SpleefFloor> spleefFloors) { this.spleefFloors = spleefFloors; }
    public void setSpleefEliminationY(double spleefEliminationY) { this.spleefEliminationY = spleefEliminationY; }

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
        // Speed run only needs 1 spawn point; wave defense/spleef need minPlayers; others need maxPlayers
        int requiredSpawnPoints;
        if ("speed_run".equals(gameMode)) {
            requiredSpawnPoints = 1;
        } else if ("wave_defense".equals(gameMode) || "spleef".equals(gameMode)) {
            requiredSpawnPoints = minPlayers;
        } else {
            requiredSpawnPoints = maxPlayers;
        }
        if (spawnPoints == null || spawnPoints.size() < requiredSpawnPoints) return false;
        if (bounds == null) return false;

        // Speed run validation
        if ("speed_run".equals(gameMode)) {
            if (startZone == null || finishZone == null) return false;
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
            double loX = Math.min(minX, maxX), hiX = Math.max(minX, maxX);
            double loY = Math.min(minY, maxY), hiY = Math.max(minY, maxY);
            double loZ = Math.min(minZ, maxZ), hiZ = Math.max(minZ, maxZ);
            return x >= loX && x <= hiX &&
                   y >= loY && y <= hiY &&
                   z >= loZ && z <= hiZ;
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
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
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
         * Checks if a position is within the bounds.
         * Handles either corner order (min/max are just corner 1/corner 2).
         */
        public boolean contains(double x, double y, double z) {
            double loX = Math.min(minX, maxX), hiX = Math.max(minX, maxX);
            double loY = Math.min(minY, maxY), hiY = Math.max(minY, maxY);
            double loZ = Math.min(minZ, maxZ), hiZ = Math.max(minZ, maxZ);
            return x >= loX && x <= hiX &&
                   y >= loY && y <= hiY &&
                   z >= loZ && z <= hiZ;
        }

        /**
         * Checks if a position is within the bounds shrunk by margin on each side.
         * Handles either corner order.
         */
        public boolean containsWithMargin(double x, double y, double z, double margin) {
            double loX = Math.min(minX, maxX), hiX = Math.max(minX, maxX);
            double loY = Math.min(minY, maxY), hiY = Math.max(minY, maxY);
            double loZ = Math.min(minZ, maxZ), hiZ = Math.max(minZ, maxZ);
            return x >= loX + margin && x <= hiX - margin &&
                   y >= loY + margin && y <= hiY - margin &&
                   z >= loZ + margin && z <= hiZ - margin;
        }

        /**
         * Clamps a position to be inside the bounds shrunk by margin.
         * Returns a double[3] with {clampedX, clampedY, clampedZ}.
         * Handles either corner order.
         */
        public double[] clampInside(double x, double y, double z, double margin) {
            double loX = Math.min(minX, maxX) + margin;
            double hiX = Math.max(minX, maxX) - margin;
            double loY = Math.min(minY, maxY) + margin;
            double hiY = Math.max(minY, maxY) - margin;
            double loZ = Math.min(minZ, maxZ) + margin;
            double hiZ = Math.max(minZ, maxZ) - margin;
            return new double[] {
                Math.max(loX, Math.min(x, hiX)),
                Math.max(loY, Math.min(y, hiY)),
                Math.max(loZ, Math.min(z, hiZ))
            };
        }
    }

    /**
     * Represents a spleef floor region with a specific block type.
     * Blocks within this region can be broken during a spleef match
     * and are regenerated when the match ends.
     */
    public static class SpleefFloor {
        private double minX;
        private double minY;
        private double minZ;
        private double maxX;
        private double maxY;
        private double maxZ;
        private String blockId; // e.g. "hytale:snow"

        public SpleefFloor() {}

        public SpleefFloor(double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ, String blockId) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.blockId = blockId;
        }

        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }
        public String getBlockId() { return blockId; }

        public void setMinX(double minX) { this.minX = minX; }
        public void setMinY(double minY) { this.minY = minY; }
        public void setMinZ(double minZ) { this.minZ = minZ; }
        public void setMaxX(double maxX) { this.maxX = maxX; }
        public void setMaxY(double maxY) { this.maxY = maxY; }
        public void setMaxZ(double maxZ) { this.maxZ = maxZ; }
        public void setBlockId(String blockId) { this.blockId = blockId; }

        /**
         * Block-coordinate containment check.
         * Converts fractional world positions to block coords via Math.floor(),
         * then uses inclusive range on both ends so single-layer floors (minY==maxY) work.
         */
        public boolean contains(int bx, int by, int bz) {
            int loX = (int) Math.floor(Math.min(minX, maxX));
            int hiX = (int) Math.floor(Math.max(minX, maxX));
            int loY = (int) Math.floor(Math.min(minY, maxY));
            int hiY = (int) Math.floor(Math.max(minY, maxY));
            int loZ = (int) Math.floor(Math.min(minZ, maxZ));
            int hiZ = (int) Math.floor(Math.max(minZ, maxZ));
            return bx >= loX && bx <= hiX &&
                   by >= loY && by <= hiY &&
                   bz >= loZ && bz <= hiZ;
        }
    }
}
