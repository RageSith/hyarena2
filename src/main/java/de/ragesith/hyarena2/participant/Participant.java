package de.ragesith.hyarena2.participant;

import java.util.UUID;

/**
 * Unified interface for both players and bots in matches.
 * Provides abstraction over different participant types.
 */
public interface Participant {
    /**
     * Gets the unique identifier for this participant
     */
    UUID getUniqueId();

    /**
     * Gets the display name of this participant
     */
    String getName();

    /**
     * Gets the type of this participant (PLAYER or BOT)
     */
    ParticipantType getType();

    /**
     * Checks if this participant is currently alive
     */
    boolean isAlive();

    /**
     * Sets the alive status of this participant
     */
    void setAlive(boolean alive);

    /**
     * Gets the number of kills this participant has
     */
    int getKills();

    /**
     * Increments the kill count by 1
     */
    void addKill();

    /**
     * Gets the number of deaths this participant has
     */
    int getDeaths();

    /**
     * Increments the death count by 1
     */
    void addDeath();

    /**
     * Gets the total damage dealt by this participant
     */
    double getDamageDealt();

    /**
     * Adds damage to the total damage dealt
     */
    void addDamageDealt(double damage);

    /**
     * Gets the total damage taken by this participant
     */
    double getDamageTaken();

    /**
     * Adds damage to the total damage taken
     */
    void addDamageTaken(double damage);

    /**
     * Sends a message to this participant
     */
    void sendMessage(String message);

    /**
     * Checks if this participant is still online/valid
     * @return true if the participant is still valid (player online or bot active)
     */
    boolean isValid();

    /**
     * Grants temporary immunity from damage.
     * @param durationMs Duration of immunity in milliseconds
     */
    void grantImmunity(long durationMs);

    /**
     * Checks if this participant currently has immunity.
     * @return true if immune to damage
     */
    boolean isImmune();

    /**
     * Gets the selected kit ID for this participant.
     * @return kit ID, or null if no kit selected
     */
    String getSelectedKitId();

    /**
     * Sets the selected kit ID for this participant.
     * @param kitId the kit ID to set
     */
    void setSelectedKitId(String kitId);

    /**
     * Gets the UUID of the last attacker who damaged this participant.
     * Used for kill attribution on environmental deaths.
     * @return UUID of last attacker, or null if none
     */
    UUID getLastAttackerUuid();

    /**
     * Gets the timestamp of the last damage received.
     * @return timestamp in milliseconds, or 0 if never damaged
     */
    long getLastDamageTimestamp();

    /**
     * Records damage from an attacker for kill attribution.
     * @param attackerUuid UUID of the attacker (can be null for environmental)
     */
    void setLastAttacker(UUID attackerUuid);
}
