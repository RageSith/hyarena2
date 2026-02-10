package de.ragesith.hyarena2.economy;

import java.util.ArrayList;
import java.util.List;

/**
 * Economy configuration loaded from economy.json.
 * Defines AP reward rates, honor reward/decay rates, and honor rank definitions.
 */
public class EconomyConfig {

    // ArenaPoints reward rates
    private int apWinReward = 15;
    private int apLossReward = 10;
    private int apPerKill = 0;

    // Honor reward/decay rates
    private double honorWinReward = 12;
    private double honorLossReward = 8;
    private double honorDecayPerHour = 1;
    private double honorMaxCap = 1200;

    // Honor ranks (sorted ascending by threshold)
    private List<HonorRankConfig> honorRanks = new ArrayList<>();

    public EconomyConfig() {
        // Default ranks
        honorRanks.add(new HonorRankConfig(0, "novice", "Novice", "hyarena.classes.rank.novice"));
        honorRanks.add(new HonorRankConfig(100, "apprentice", "Apprentice", "hyarena.classes.rank.apprentice"));
        honorRanks.add(new HonorRankConfig(300, "warrior", "Warrior", "hyarena.classes.rank.warrior"));
        honorRanks.add(new HonorRankConfig(600, "gladiator", "Gladiator", "hyarena.classes.rank.gladiator"));
        honorRanks.add(new HonorRankConfig(1000, "champion", "Champion", "hyarena.classes.rank.champion"));
    }

    // ========== AP Getters ==========

    public int getApWinReward() {
        return apWinReward;
    }

    public int getApLossReward() {
        return apLossReward;
    }

    public int getApPerKill() {
        return apPerKill;
    }

    // ========== Honor Getters ==========

    public double getHonorWinReward() {
        return honorWinReward;
    }

    public double getHonorLossReward() {
        return honorLossReward;
    }

    public double getHonorDecayPerHour() {
        return honorDecayPerHour;
    }

    public double getHonorMaxCap() {
        return honorMaxCap;
    }

    public List<HonorRankConfig> getHonorRanks() {
        return honorRanks;
    }
}
