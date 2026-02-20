package de.ragesith.hyarena2.config;

/**
 * Represents an axis-aligned bounding box defined by two corner positions.
 * Used for hub boundaries and arena bounds.
 */
public class BoundingBox {
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    /**
     * Default constructor for Gson deserialization.
     */
    public BoundingBox() {
        this(0, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a bounding box from min/max coordinates.
     */
    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Creates a bounding box from two corner positions.
     */
    public BoundingBox(Position corner1, Position corner2) {
        this(corner1.getX(), corner1.getY(), corner1.getZ(),
             corner2.getX(), corner2.getY(), corner2.getZ());
    }

    /**
     * Checks if a position is inside this bounding box.
     * Handles either corner order.
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
     * Checks if a position is inside this bounding box (X/Z only, ignores Y).
     * Handles either corner order.
     */
    public boolean containsXZ(double x, double z) {
        double loX = Math.min(minX, maxX), hiX = Math.max(minX, maxX);
        double loZ = Math.min(minZ, maxZ), hiZ = Math.max(minZ, maxZ);
        return x >= loX && x <= hiX &&
               z >= loZ && z <= hiZ;
    }

    /**
     * Checks if a Position object is inside this bounding box.
     */
    public boolean contains(Position pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Gets the center position of this bounding box.
     */
    public Position getCenter() {
        return new Position(
            (minX + maxX) / 2,
            (minY + maxY) / 2,
            (minZ + maxZ) / 2
        );
    }

    /**
     * Clamps a position to be inside this bounding box.
     * Returns a new Position clamped to the bounds.
     * Handles either corner order.
     */
    public Position clamp(Position pos) {
        double loX = Math.min(minX, maxX), hiX = Math.max(minX, maxX);
        double loY = Math.min(minY, maxY), hiY = Math.max(minY, maxY);
        double loZ = Math.min(minZ, maxZ), hiZ = Math.max(minZ, maxZ);
        return new Position(
            Math.max(loX, Math.min(hiX, pos.getX())),
            Math.max(loY, Math.min(hiY, pos.getY())),
            Math.max(loZ, Math.min(hiZ, pos.getZ())),
            pos.getYaw(),
            pos.getPitch()
        );
    }

    /**
     * Gets the nearest point on the boundary to the given position.
     * Useful for teleporting players back when they go out of bounds.
     * Handles either corner order.
     */
    public Position getNearestEdge(Position pos) {
        double loX = Math.min(minX, maxX), hiX = Math.max(minX, maxX);
        double loY = Math.min(minY, maxY), hiY = Math.max(minY, maxY);
        double loZ = Math.min(minZ, maxZ), hiZ = Math.max(minZ, maxZ);

        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double resultX = x;
        double resultY = y;
        double resultZ = z;

        if (x < loX) resultX = loX + 0.5;
        else if (x > hiX) resultX = hiX - 0.5;

        if (y < loY) resultY = loY + 0.5;
        else if (y > hiY) resultY = hiY - 0.5;

        if (z < loZ) resultZ = loZ + 0.5;
        else if (z > hiZ) resultZ = hiZ - 0.5;

        return new Position(resultX, resultY, resultZ, pos.getYaw(), pos.getPitch());
    }

    // Getters and setters

    public double getMinX() {
        return minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public void setMinZ(double minZ) {
        this.minZ = minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(double maxZ) {
        this.maxZ = maxZ;
    }

    /**
     * Checks if this bounding box is valid (has non-zero volume).
     */
    public boolean isValid() {
        return maxX != minX && maxY != minY && maxZ != minZ;
    }

    @Override
    public String toString() {
        return String.format("BoundingBox[(%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f)]",
            minX, minY, minZ, maxX, maxY, maxZ);
    }
}
