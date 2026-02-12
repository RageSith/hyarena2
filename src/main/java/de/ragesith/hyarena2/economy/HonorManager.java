package de.ragesith.hyarena2.economy;

import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.economy.HonorEarnedEvent;
import de.ragesith.hyarena2.event.economy.HonorRankChangedEvent;
import de.ragesith.hyarena2.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manages honor, ranks, and decay.
 * Handles rank promotion/demotion with permission grants/revokes via EtherealPerms.
 */
public class HonorManager {

    private final EconomyConfig config;
    private final PlayerDataManager playerDataManager;
    private final EventBus eventBus;
    private final List<HonorRankConfig> sortedRanks;

    public HonorManager(EconomyConfig config, PlayerDataManager playerDataManager, EventBus eventBus) {
        this.config = config;
        this.playerDataManager = playerDataManager;
        this.eventBus = eventBus;

        // Sort ranks ascending by threshold
        this.sortedRanks = new ArrayList<>(config.getHonorRanks());
        this.sortedRanks.sort(Comparator.comparingDouble(HonorRankConfig::getThreshold));
    }

    // ========== Honor Mutation ==========

    /**
     * Adds honor to a player, clamped to the configured cap.
     * Checks for rank changes after mutation.
     */
    public void addHonor(UUID uuid, double amount) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null || amount <= 0) return;

        double newHonor = Math.min(data.getHonor() + amount, config.getHonorMaxCap());
        data.setHonor(newHonor);

        eventBus.publish(new HonorEarnedEvent(uuid, amount, "match reward"));

        checkRankChange(uuid, data);
    }

    /**
     * Sets honor directly (for admin/test commands).
     */
    public void setHonor(UUID uuid, double amount) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null) return;

        data.setHonor(Math.max(0, Math.min(amount, config.getHonorMaxCap())));
        checkRankChange(uuid, data);
    }

    // ========== Decay ==========

    /**
     * Processes honor decay for a single player based on elapsed time since last decay.
     */
    public void decayHonor(UUID uuid) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null || data.getHonor() <= 0) return;

        long now = System.currentTimeMillis();
        long lastDecay = data.getLastHonorDecayTimestamp();
        if (lastDecay <= 0) {
            data.setLastHonorDecayTimestamp(now);
            return;
        }

        double hoursElapsed = (now - lastDecay) / (1000.0 * 60 * 60);
        if (hoursElapsed <= 0) return;

        double decay = hoursElapsed * config.getHonorDecayPerHour();
        if (decay <= 0) return;

        double newHonor = Math.max(0, data.getHonor() - decay);
        if (newHonor != data.getHonor()) {
            data.setHonor(newHonor);
            checkRankChange(uuid, data);
        }

        data.setLastHonorDecayTimestamp(now);
    }

    /**
     * Processes decay for all loaded players. Called periodically by scheduler.
     */
    public void tickDecay() {
        for (PlayerEconomyData data : playerDataManager.getAllLoadedPlayers()) {
            decayHonor(data.getPlayerUuid());
        }
    }

    // ========== Rank Logic ==========

    /**
     * Gets the appropriate rank for a given honor value.
     */
    public HonorRankConfig getCurrentRank(double honor) {
        HonorRankConfig result = sortedRanks.get(0); // Default to lowest rank
        for (HonorRankConfig rank : sortedRanks) {
            if (honor >= rank.getThreshold()) {
                result = rank;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Updates a player's rank based on current honor. Grants/revokes permissions as needed.
     */
    public void updatePlayerRank(UUID uuid) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null) return;

        checkRankChange(uuid, data);
    }

    /**
     * Gets the display name of a player's current rank.
     */
    public String getRankDisplayName(UUID uuid) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null) return "Unknown";

        HonorRankConfig rank = getCurrentRank(data.getHonor());
        return rank.getDisplayName();
    }

    /**
     * Gets the color code for a rank (for UI display).
     */
    public String getRankColor(UUID uuid) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data == null) return "#7f8c8d";

        HonorRankConfig rank = getCurrentRank(data.getHonor());
        int index = sortedRanks.indexOf(rank);
        return switch (index) {
            case 0 -> "#7f8c8d"; // Novice - gray
            case 1 -> "#2ecc71"; // Apprentice - green
            case 2 -> "#3498db"; // Warrior - blue
            case 3 -> "#9b59b6"; // Gladiator - purple
            case 4 -> "#f1c40f"; // Champion - gold
            default -> "#b7cedd";
        };
    }

    /**
     * Checks if a player's rank has changed and handles permission grants/revokes.
     */
    private void checkRankChange(UUID uuid, PlayerEconomyData data) {
        HonorRankConfig newRank = getCurrentRank(data.getHonor());
        String oldRankId = data.getCurrentRankId();

        if (oldRankId != null && oldRankId.equals(newRank.getId())) {
            return; // No change
        }

        String oldRankDisplayName = "None";

        // Revoke old rank permission
        if (oldRankId != null) {
            HonorRankConfig oldRank = getRankById(oldRankId);
            if (oldRank != null) {
                oldRankDisplayName = oldRank.getDisplayName();
                PermissionHelper.revokePermission(uuid, oldRank.getPermission());
            }
        }

        // Grant new rank permission
        PermissionHelper.grantPermission(uuid, newRank.getPermission());
        data.setCurrentRankId(newRank.getId());

        System.out.println("[HonorManager] Rank changed for " + data.getPlayerName()
            + ": " + oldRankId + " -> " + newRank.getId()
            + " (honor: " + String.format("%.1f", data.getHonor()) + ")");

        eventBus.publish(new HonorRankChangedEvent(uuid, oldRankId, newRank.getId(), newRank.getDisplayName()));
    }

    /**
     * Finds a rank config by its ID.
     */
    private HonorRankConfig getRankById(String id) {
        for (HonorRankConfig rank : sortedRanks) {
            if (rank.getId().equals(id)) {
                return rank;
            }
        }
        return null;
    }
}
