package de.ragesith.hyarena2.gamemode;

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
     * Called when the match starts (transitions to STARTING state)
     * @param participants All participants in the match
     */
    void onMatchStart(List<Participant> participants);

    /**
     * Called when gameplay begins (transitions to IN_PROGRESS state)
     * @param participants All participants in the match
     */
    void onGameplayBegin(List<Participant> participants);

    /**
     * Called every tick while the match is in progress
     * @param participants All participants in the match
     * @param tickCount Number of ticks since gameplay began
     */
    void onTick(List<Participant> participants, int tickCount);

    /**
     * Called when a participant is killed
     * @param victim The participant who was killed
     * @param killer The participant who got the kill (null for environmental)
     * @param participants All participants in the match
     * @return true if this kill ends the match
     */
    boolean onParticipantKilled(Participant victim, Participant killer, List<Participant> participants);

    /**
     * Called when a participant takes damage
     * @param victim The participant who took damage
     * @param attacker The participant who dealt damage (null for environmental)
     * @param damage Amount of damage dealt
     */
    void onParticipantDamaged(Participant victim, Participant attacker, double damage);

    /**
     * Checks if the match should end based on current state
     * @param participants All participants in the match
     * @return true if the match should end
     */
    boolean shouldMatchEnd(List<Participant> participants);

    /**
     * Gets the winners of the match
     * @param participants All participants in the match
     * @return List of winner UUIDs (empty if no winners, e.g., tie or all eliminated)
     */
    List<UUID> getWinners(List<Participant> participants);

    /**
     * Gets the victory message to broadcast
     * @param winners List of winner participants
     * @return The victory message
     */
    String getVictoryMessage(List<Participant> winners);

    /**
     * Determines if a participant should respawn after death
     * @param participant The participant who died
     * @return true if they should respawn
     */
    boolean shouldRespawn(Participant participant);

    /**
     * Gets the respawn delay in ticks
     * @return Number of ticks to wait before respawning (20 = 1 second)
     */
    int getRespawnDelayTicks();
}
