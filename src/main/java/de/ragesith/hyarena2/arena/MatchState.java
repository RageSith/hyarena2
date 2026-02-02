package de.ragesith.hyarena2.arena;

/**
 * Represents the current state of a match.
 *
 * State transitions:
 * WAITING → STARTING → IN_PROGRESS → ENDING → FINISHED
 */
public enum MatchState {
    /**
     * Match created, waiting for players to be teleported
     */
    WAITING,

    /**
     * Players teleported, countdown in progress
     */
    STARTING,

    /**
     * Countdown complete, gameplay active
     */
    IN_PROGRESS,

    /**
     * Win condition met, showing victory screen
     */
    ENDING,

    /**
     * Match complete, ready for cleanup
     */
    FINISHED
}
