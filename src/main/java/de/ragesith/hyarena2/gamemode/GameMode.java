package de.ragesith.hyarena2.gamemode;

import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.participant.Participant;

import java.util.List;
import java.util.UUID;

/**
 * Interface for pluggable game modes.
 * Defines game-specific rules, win conditions, and behavior.
 */
public interface GameMode {
    /**
     * Gets the unique identifier for this game mode (e.g., "duel", "ffa")
     */
    String getId();

    /**
     * Gets the display name for this game mode
     */
    String getDisplayName();

    /**
     * Gets the UI markup description for this game mode.
     * Returned string is raw UI code that will be injected via appendInline
     * into a scrollable container on the GameModeInfoPage.
     */
    String getDescription();

    /**
     * Called when the match starts (transitions to STARTING state)
     * @param config The arena configuration
     * @param participants All participants in the match
     */
    void onMatchStart(ArenaConfig config, List<Participant> participants);

    /**
     * Called when gameplay begins (transitions to IN_PROGRESS state)
     * @param config The arena configuration
     * @param participants All participants in the match
     */
    void onGameplayBegin(ArenaConfig config, List<Participant> participants);

    /**
     * Called every tick while the match is in progress
     * @param config The arena configuration
     * @param participants All participants in the match
     * @param tickCount Number of ticks since gameplay began
     */
    void onTick(ArenaConfig config, List<Participant> participants, int tickCount);

    /**
     * Called when a participant is killed
     * @param config The arena configuration
     * @param victim The participant who was killed
     * @param killer The participant who got the kill (null for environmental)
     * @param participants All participants in the match
     * @return true if this kill ends the match
     */
    boolean onParticipantKilled(ArenaConfig config, Participant victim, Participant killer, List<Participant> participants);

    /**
     * Called when a participant takes damage
     * @param config The arena configuration
     * @param victim The participant who took damage
     * @param attacker The participant who dealt damage (null for environmental)
     * @param damage Amount of damage dealt
     */
    void onParticipantDamaged(ArenaConfig config, Participant victim, Participant attacker, double damage);

    /**
     * Checks if the match should end based on current state
     * @param config The arena configuration
     * @param participants All participants in the match
     * @return true if the match should end
     */
    boolean shouldMatchEnd(ArenaConfig config, List<Participant> participants);

    /**
     * Gets the winners of the match
     * @param config The arena configuration
     * @param participants All participants in the match
     * @return List of winner UUIDs (empty if no winners, e.g., tie or all eliminated)
     */
    List<UUID> getWinners(ArenaConfig config, List<Participant> participants);

    /**
     * Gets the victory message to broadcast
     * @param config The arena configuration
     * @param winners List of winner participants
     * @return The victory message
     */
    String getVictoryMessage(ArenaConfig config, List<Participant> winners);

    /**
     * Returns the kit ID to assign on spawn/respawn.
     * Returning null means the participant keeps their selected kit (default behavior).
     * Game modes like Kit Roulette override this to assign random kits.
     * @param config The arena configuration
     * @param participant The participant being spawned
     * @return kit ID to override with, or null for default behavior
     */
    default String getNextKitId(ArenaConfig config, Participant participant) {
        return null;
    }

    /**
     * Returns a participant's game-mode-specific score (e.g., control seconds in KOTH).
     * Returns -1 if the game mode doesn't use a score system.
     */
    default int getParticipantScore(UUID participantId) {
        return -1;
    }

    /**
     * Returns the score target for the match, or -1 if no target.
     */
    default int getScoreTarget(ArenaConfig config) {
        return -1;
    }

    /**
     * Returns the label for the score column (e.g., "Pts"), or null if no score system.
     */
    default String getScoreLabel() {
        return null;
    }

    /**
     * Called when the match finishes or is cancelled, before players are teleported back.
     * Game modes can use this for cleanup (e.g., clearing debug shapes).
     * @param participants All participants in the match
     */
    default void onMatchFinished(List<Participant> participants) {}

    /**
     * Determines if a participant should respawn after death
     * @param config The arena configuration
     * @param participant The participant who died
     * @return true if they should respawn
     */
    boolean shouldRespawn(ArenaConfig config, Participant participant);

    /**
     * Gets the respawn delay in ticks
     * @param config The arena configuration
     * @return Number of ticks to wait before respawning (20 = 1 second)
     */
    int getRespawnDelayTicks(ArenaConfig config);
}
