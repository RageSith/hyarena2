package de.ragesith.hyarena2.config;

/**
 * Represents a position in the world with coordinates and rotation.
 * Used for spawn points, teleportation targets, and boundary definitions.
 */
public class Position {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    /**
     * Default constructor for Gson deserialization.
     */
    public Position() {
        this(0, 0, 0, 0, 0);
    }

    /**
     * Creates a position with coordinates only (no rotation).
     */
    public Position(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    /**
     * Creates a position with full coordinates and rotation.
     */
    public Position(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * Calculates the distance to another position.
     */
    public double distanceTo(Position other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates the horizontal (X/Z) distance to another position.
     */
    public double distanceXZ(Position other) {
        double dx = this.x - other.x;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Creates a copy of this position.
     */
    public Position copy() {
        return new Position(x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return String.format("Position(%.2f, %.2f, %.2f, yaw=%.1f, pitch=%.1f)", x, y, z, yaw, pitch);
    }
}
