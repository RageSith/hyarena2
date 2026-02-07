package de.ragesith.hyarena2.config;

/**
 * Global configuration settings for HyArena2.
 * These settings apply across all arenas and the entire plugin.
 * Loaded from global.json.
 */
public class GlobalConfig {

    // ========== Threading Settings ==========

    /**
     * Size of the main scheduler thread pool.
     * Default: 1
     */
    private int schedulerPoolSize = 1;

    /**
     * Interval in milliseconds between HUD refreshes.
     * Default: 1000ms (1 second)
     */
    private int hudRefreshIntervalMs = 1000;

    /**
     * Interval in milliseconds between bot AI ticks.
     * Default: 1000ms (1 second)
     */
    private int botTickIntervalMs = 1000;

    /**
     * Interval in milliseconds between hub boundary checks.
     * Default: 500ms
     */
    private int boundaryCheckIntervalMs = 500;

    /**
     * Interval in milliseconds between arena boundary checks.
     * Lower than hub checks for smoother invisible-wall feel.
     * Default: 50ms
     */
    private int arenaBoundaryCheckIntervalMs = 50;

    /**
     * Margin in blocks inward from arena walls for boundary clamping.
     * Default: 0.5
     */
    private double arenaBoundaryMargin = 0.5;

    // ========== Default Settings ==========

    /**
     * Default kit assigned when no kit is selected.
     * Default: "warrior"
     */
    private String defaultKit = "warrior";

    // ========== Feature Flags ==========

    /**
     * Whether bot players are enabled.
     * Default: true
     */
    private boolean botsEnabled = true;

    /**
     * Whether player statistics tracking is enabled.
     * Default: true
     */
    private boolean statsEnabled = true;

    /**
     * Whether boundary enforcement is enabled.
     * Default: true
     */
    private boolean boundaryEnforcementEnabled = true;

    /**
     * Whether debug logging is enabled.
     * Default: false
     */
    private boolean debugLogging = false;

    // ========== Match Settings ==========

    /**
     * Duration in seconds of the countdown before a match starts.
     * Default: 5 seconds
     */
    private int matchCountdownSeconds = 5;

    /**
     * Duration in seconds of invulnerability after respawning.
     * Default: 3 seconds
     */
    private int respawnImmunitySeconds = 3;

    /**
     * Default match duration in seconds if not specified by game mode.
     * Default: 300 seconds (5 minutes)
     */
    private int defaultMatchDurationSeconds = 300;

    // ========== Teleport Settings ==========

    /**
     * Delay in milliseconds for cross-world teleportation.
     * Default: 1500ms (1.5 seconds)
     */
    private int crossWorldTeleportDelayMs = 1500;

    /**
     * Delay in milliseconds for post-combat teleportation.
     * Default: 1000ms (1 second)
     */
    private int postCombatTeleportDelayMs = 1000;

    // ========== Constructors ==========

    /**
     * Default constructor with all default values.
     * Required for Gson deserialization.
     */
    public GlobalConfig() {
    }

    // ========== Threading Getters/Setters ==========

    public int getSchedulerPoolSize() {
        return schedulerPoolSize;
    }

    public void setSchedulerPoolSize(int schedulerPoolSize) {
        this.schedulerPoolSize = schedulerPoolSize;
    }

    public int getHudRefreshIntervalMs() {
        return hudRefreshIntervalMs;
    }

    public void setHudRefreshIntervalMs(int hudRefreshIntervalMs) {
        this.hudRefreshIntervalMs = hudRefreshIntervalMs;
    }

    public int getBotTickIntervalMs() {
        return botTickIntervalMs;
    }

    public void setBotTickIntervalMs(int botTickIntervalMs) {
        this.botTickIntervalMs = botTickIntervalMs;
    }

    public int getBoundaryCheckIntervalMs() {
        return boundaryCheckIntervalMs;
    }

    public void setBoundaryCheckIntervalMs(int boundaryCheckIntervalMs) {
        this.boundaryCheckIntervalMs = boundaryCheckIntervalMs;
    }

    public int getArenaBoundaryCheckIntervalMs() {
        return arenaBoundaryCheckIntervalMs;
    }

    public void setArenaBoundaryCheckIntervalMs(int arenaBoundaryCheckIntervalMs) {
        this.arenaBoundaryCheckIntervalMs = arenaBoundaryCheckIntervalMs;
    }

    public double getArenaBoundaryMargin() {
        return arenaBoundaryMargin;
    }

    public void setArenaBoundaryMargin(double arenaBoundaryMargin) {
        this.arenaBoundaryMargin = arenaBoundaryMargin;
    }

    // ========== Default Settings Getters/Setters ==========

    public String getDefaultKit() {
        return defaultKit;
    }

    public void setDefaultKit(String defaultKit) {
        this.defaultKit = defaultKit;
    }

    // ========== Feature Flag Getters/Setters ==========

    public boolean isBotsEnabled() {
        return botsEnabled;
    }

    public void setBotsEnabled(boolean botsEnabled) {
        this.botsEnabled = botsEnabled;
    }

    public boolean isStatsEnabled() {
        return statsEnabled;
    }

    public void setStatsEnabled(boolean statsEnabled) {
        this.statsEnabled = statsEnabled;
    }

    public boolean isBoundaryEnforcementEnabled() {
        return boundaryEnforcementEnabled;
    }

    public void setBoundaryEnforcementEnabled(boolean boundaryEnforcementEnabled) {
        this.boundaryEnforcementEnabled = boundaryEnforcementEnabled;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    // ========== Match Settings Getters/Setters ==========

    public int getMatchCountdownSeconds() {
        return matchCountdownSeconds;
    }

    public void setMatchCountdownSeconds(int matchCountdownSeconds) {
        this.matchCountdownSeconds = matchCountdownSeconds;
    }

    public int getRespawnImmunitySeconds() {
        return respawnImmunitySeconds;
    }

    public void setRespawnImmunitySeconds(int respawnImmunitySeconds) {
        this.respawnImmunitySeconds = respawnImmunitySeconds;
    }

    public int getDefaultMatchDurationSeconds() {
        return defaultMatchDurationSeconds;
    }

    public void setDefaultMatchDurationSeconds(int defaultMatchDurationSeconds) {
        this.defaultMatchDurationSeconds = defaultMatchDurationSeconds;
    }

    // ========== Teleport Settings Getters/Setters ==========

    public int getCrossWorldTeleportDelayMs() {
        return crossWorldTeleportDelayMs;
    }

    public void setCrossWorldTeleportDelayMs(int crossWorldTeleportDelayMs) {
        this.crossWorldTeleportDelayMs = crossWorldTeleportDelayMs;
    }

    public int getPostCombatTeleportDelayMs() {
        return postCombatTeleportDelayMs;
    }

    public void setPostCombatTeleportDelayMs(int postCombatTeleportDelayMs) {
        this.postCombatTeleportDelayMs = postCombatTeleportDelayMs;
    }
}
