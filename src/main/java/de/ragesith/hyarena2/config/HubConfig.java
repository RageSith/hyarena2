package de.ragesith.hyarena2.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the hub spawn area.
 * Loaded from hub.json.
 */
public class HubConfig {

    /**
     * The world name where the hub is located.
     * If null or empty, uses the default world.
     */
    private String worldName = "default";

    /**
     * The spawn point where players appear when joining or returning from matches.
     */
    private Position spawnPoint;

    /**
     * The boundary area that players are kept within.
     */
    private BoundingBox bounds;

    /**
     * Floating text holograms in the hub (labels above NPC statues, etc.).
     */
    private List<HologramEntry> holograms = new ArrayList<>();

    /**
     * Default constructor with defaults from HyArena1 config.
     */
    public HubConfig() {
        // Hub spawn from HyArena1 config
        this.spawnPoint = new Position(-2.71, 99.0, -2.07, 2.36f, 0);
        // Hub bounds from HyArena1 config (corner1: 12.61, -17.53 to corner2: -17.58, 13.50)
        this.bounds = new BoundingBox(-17.58, 80, -17.53, 12.61, 120, 13.50);
    }

    // ========== Getters/Setters ==========

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Position getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(Position spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public void setBounds(BoundingBox bounds) {
        this.bounds = bounds;
    }

    public List<HologramEntry> getHolograms() {
        if (holograms == null) holograms = new ArrayList<>();
        return holograms;
    }

    public void setHolograms(List<HologramEntry> holograms) {
        this.holograms = holograms;
    }

    // ========== Convenience Methods ==========

    /**
     * Checks if a position is within the hub bounds.
     */
    public boolean isInBounds(double x, double y, double z) {
        return bounds != null && bounds.contains(x, y, z);
    }

    /**
     * Checks if a position is within the hub bounds (X/Z only).
     */
    public boolean isInBoundsXZ(double x, double z) {
        return bounds != null && bounds.containsXZ(x, z);
    }

    /**
     * Checks if the hub configuration is valid.
     */
    public boolean isValid() {
        return spawnPoint != null && bounds != null && bounds.isValid();
    }

    /**
     * Gets the effective world name (never null).
     */
    public String getEffectiveWorldName() {
        return (worldName != null && !worldName.isEmpty()) ? worldName : "default";
    }

    /**
     * A floating text hologram entry in the hub.
     */
    public static class HologramEntry {
        private double x, y, z;
        private String text;

        public HologramEntry() {}

        public HologramEntry(double x, double y, double z, String text) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
