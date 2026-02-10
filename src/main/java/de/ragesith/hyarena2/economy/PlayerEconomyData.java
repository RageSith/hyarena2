package de.ragesith.hyarena2.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per-player economy state persisted to data/players/<uuid>.json.
 * Tracks AP balance, honor, current rank, and purchased items.
 */
public class PlayerEconomyData {
    private UUID playerUuid;
    private String playerName;
    private int arenaPoints;
    private double honor;
    private String currentRankId;
    private long lastHonorDecayTimestamp;
    private long lastOnlineTimestamp;
    private List<String> purchasedItems;

    public PlayerEconomyData() {
        this.arenaPoints = 0;
        this.honor = 0;
        this.currentRankId = "novice";
        this.lastHonorDecayTimestamp = System.currentTimeMillis();
        this.lastOnlineTimestamp = System.currentTimeMillis();
        this.purchasedItems = new ArrayList<>();
    }

    public PlayerEconomyData(UUID playerUuid, String playerName) {
        this();
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    // ========== Getters & Setters ==========

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getArenaPoints() {
        return arenaPoints;
    }

    public void setArenaPoints(int arenaPoints) {
        this.arenaPoints = arenaPoints;
    }

    public double getHonor() {
        return honor;
    }

    public void setHonor(double honor) {
        this.honor = honor;
    }

    public String getCurrentRankId() {
        return currentRankId;
    }

    public void setCurrentRankId(String currentRankId) {
        this.currentRankId = currentRankId;
    }

    public long getLastHonorDecayTimestamp() {
        return lastHonorDecayTimestamp;
    }

    public void setLastHonorDecayTimestamp(long lastHonorDecayTimestamp) {
        this.lastHonorDecayTimestamp = lastHonorDecayTimestamp;
    }

    public long getLastOnlineTimestamp() {
        return lastOnlineTimestamp;
    }

    public void setLastOnlineTimestamp(long lastOnlineTimestamp) {
        this.lastOnlineTimestamp = lastOnlineTimestamp;
    }

    public List<String> getPurchasedItems() {
        if (purchasedItems == null) purchasedItems = new ArrayList<>();
        return purchasedItems;
    }

    public void setPurchasedItems(List<String> purchasedItems) {
        this.purchasedItems = purchasedItems;
    }

    public boolean hasPurchased(String itemId) {
        return getPurchasedItems().contains(itemId);
    }

    public void addPurchase(String itemId) {
        getPurchasedItems().add(itemId);
    }
}
