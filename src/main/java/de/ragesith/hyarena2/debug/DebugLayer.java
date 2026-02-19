package de.ragesith.hyarena2.debug;

/**
 * Toggleable debug visualization layers.
 */
public enum DebugLayer {
    /** Barrier blocks in nearby area */
    BARRIERS,
    /** Arena bounds, hub boundary, KOTH capture zones, speedrun zones, kill planes */
    ZONES,
    /** Player spawn points, wave spawn points */
    SPAWNS
}
