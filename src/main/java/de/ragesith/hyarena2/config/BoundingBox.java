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
        // Ensure min is actually min and max is actually max
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
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
     */
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    /**
     * Checks if a position is inside this bounding box (X/Z only, ignores Y).
     */
    public boolean containsXZ(double x, double z) {
        return x >= minX && x <= maxX &&
               z >= minZ && z <= maxZ;
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
     */
    public Position clamp(Position pos) {
        return new Position(
            Math.max(minX, Math.min(maxX, pos.getX())),
            Math.max(minY, Math.min(maxY, pos.getY())),
            Math.max(minZ, Math.min(maxZ, pos.getZ())),
            pos.getYaw(),
            pos.getPitch()
        );
    }

    /**
     * Gets the nearest point on the boundary to the given position.
     * Useful for teleporting players back when they go out of bounds.
     */
    public Position getNearestEdge(Position pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        // Find nearest edge for each axis
        double nearestX = (Math.abs(x - minX) < Math.abs(x - maxX)) ? minX : maxX;
        double nearestY = (Math.abs(y - minY) < Math.abs(y - maxY)) ? minY : maxY;
        double nearestZ = (Math.abs(z - minZ) < Math.abs(z - maxZ)) ? minZ : maxZ;

        // Find which axis is closest to an edge
        double distX = Math.min(Math.abs(x - minX), Math.abs(x - maxX));
        double distY = Math.min(Math.abs(y - minY), Math.abs(y - maxY));
        double distZ = Math.min(Math.abs(z - minZ), Math.abs(z - maxZ));

        // Clamp to the boundary while staying close to original position
        double resultX = x;
        double resultY = y;
        double resultZ = z;

        if (x < minX) resultX = minX + 0.5;
        else if (x > maxX) resultX = maxX - 0.5;

        if (y < minY) resultY = minY + 0.5;
        else if (y > maxY) resultY = maxY - 0.5;

        if (z < minZ) resultZ = minZ + 0.5;
        else if (z > maxZ) resultZ = maxZ - 0.5;

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
        return maxX > minX && maxY > minY && maxZ > minZ;
    }

    @Override
    public String toString() {
        return String.format("BoundingBox[(%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f)]",
            minX, minY, minZ, maxX, maxY, maxZ);
    }
}
