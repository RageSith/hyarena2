package de.ragesith.hyarena2.stats;

import de.ragesith.hyarena2.bot.BotDifficulty;

import java.util.UUID;

/**
 * Mutable data class capturing one participant's stats for a match.
 * Lives independently of Match.participants so eliminated players' stats are preserved.
 */
public class ParticipantRecord {
    private final UUID uuid;          // null for bots
    private final String username;
    private final boolean isBot;
    private final BotDifficulty botDifficulty; // null for players
    private String kitId;
    private boolean isWinner;

    // PvP / PvE split stats
    private int pvpKills;
    private int pvpDeaths;
    private int pveKills;
    private int pveDeaths;
    private double damageDealt;
    private double damageTaken;

    // Economy snapshot (populated before API submission for non-bot participants)
    private int arenaPoints;
    private double honor;
    private String honorRank;

    public ParticipantRecord(UUID uuid, String username, boolean isBot, BotDifficulty botDifficulty, String kitId) {
        this.uuid = uuid;
        this.username = username;
        this.isBot = isBot;
        this.botDifficulty = botDifficulty;
        this.kitId = kitId;
    }

    // Kill/death recording
    public void recordPvpKill() { pvpKills++; }
    public void recordPvpDeath() { pvpDeaths++; }
    public void recordPveKill() { pveKills++; }
    public void recordPveDeath() { pveDeaths++; }

    // Damage recording
    public void addDamageDealt(double amount) { damageDealt += amount; }
    public void addDamageTaken(double amount) { damageTaken += amount; }

    // Getters
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public boolean isBot() { return isBot; }
    public BotDifficulty getBotDifficulty() { return botDifficulty; }
    public String getKitId() { return kitId; }
    public void setKitId(String kitId) { this.kitId = kitId; }
    public boolean isWinner() { return isWinner; }
    public void setWinner(boolean winner) { isWinner = winner; }
    public int getPvpKills() { return pvpKills; }
    public int getPvpDeaths() { return pvpDeaths; }
    public int getPveKills() { return pveKills; }
    public int getPveDeaths() { return pveDeaths; }
    public double getDamageDealt() { return damageDealt; }
    public double getDamageTaken() { return damageTaken; }

    public int getTotalKills() { return pvpKills + pveKills; }
    public int getTotalDeaths() { return pvpDeaths + pveDeaths; }

    // Economy snapshot getters/setters
    public int getArenaPoints() { return arenaPoints; }
    public void setArenaPoints(int arenaPoints) { this.arenaPoints = arenaPoints; }
    public double getHonor() { return honor; }
    public void setHonor(double honor) { this.honor = honor; }
    public String getHonorRank() { return honorRank; }
    public void setHonorRank(String honorRank) { this.honorRank = honorRank; }
}
