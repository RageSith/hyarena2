package de.ragesith.hyarena2.bot;

import de.ragesith.hyarena2.config.Position;

import java.util.List;
import java.util.UUID;

/**
 * Describes a spatial objective that bots should move toward.
 * Game modes provide this via getBotObjective(); non-objective modes return null.
 * Includes full zone bounds and live zone status for bot decision-making.
 */
public record BotObjective(
    Position position,      // Zone center (walk-to target)
    double radius,          // "close enough" to consider bot at zone
    String type,            // "capture", "defend", etc.
    double minX, double minY, double minZ,
    double maxX, double maxY, double maxZ,
    // Zone status (updated each game mode tick)
    UUID currentController,             // Who is scoring (null if unclaimed/contested)
    boolean contested,                  // True if 2+ participants on zone
    List<UUID> participantsInZone       // All alive participants currently inside the zone
) {
    private static final double Y_TOLERANCE = 0.5;

    /**
     * Checks if a position is inside the zone bounds (with Y tolerance).
     */
    public boolean isInsideZone(Position pos) {
        return pos.getX() >= minX && pos.getX() <= maxX
            && pos.getY() >= minY - Y_TOLERANCE && pos.getY() <= maxY + Y_TOLERANCE
            && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }
}
