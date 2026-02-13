package de.ragesith.hyarena2.bot;

/**
 * Decisions output by BotBrain each tick.
 * Listed in priority order (highest first).
 */
public enum BrainDecision {
    IDLE,
    ROAM,
    OBJECTIVE,
    DEFEND_ZONE,
    COMBAT,
    BLOCK,
    STRAFE_EVADE
}
