package de.ragesith.hyarena2.shop;

import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.PlayerDataManager;
import de.ragesith.hyarena2.economy.PlayerEconomyData;
import de.ragesith.hyarena2.economy.TransactionRecord;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.utils.PermissionHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the shop: purchase logic, ownership checks, item lookups.
 */
public class ShopManager {

    private final ShopConfig config;
    private final EconomyManager economyManager;
    private final PlayerDataManager playerDataManager;
    private final EventBus eventBus;

    // Item lookup cache (built on init)
    private final Map<String, ShopItem> itemsById = new HashMap<>();

    public ShopManager(ShopConfig config, EconomyManager economyManager,
                       PlayerDataManager playerDataManager, EventBus eventBus) {
        this.config = config;
        this.economyManager = economyManager;
        this.playerDataManager = playerDataManager;
        this.eventBus = eventBus;

        // Build lookup cache
        for (ShopCategory category : config.getCategories()) {
            for (ShopItem item : category.getItems()) {
                itemsById.put(item.getId(), item);
            }
        }

        System.out.println("[ShopManager] Loaded " + itemsById.size() + " shop items across "
            + config.getCategories().size() + " categories");
    }

    // ========== Browse ==========

    public List<ShopCategory> getCategories() {
        return config.getCategories();
    }

    public ShopItem getItem(String itemId) {
        return itemsById.get(itemId);
    }

    public List<ShopItem> getItemsByCategory(String categoryId) {
        for (ShopCategory category : config.getCategories()) {
            if (category.getId().equals(categoryId)) {
                return category.getItems();
            }
        }
        return List.of();
    }

    // ========== Purchase ==========

    /**
     * Attempts to purchase a shop item for a player.
     */
    public PurchaseResult purchase(UUID uuid, String itemId) {
        ShopItem item = itemsById.get(itemId);
        if (item == null) {
            return PurchaseResult.ITEM_NOT_FOUND;
        }

        // Check if already owned (for one-time purchases)
        if (item.isOneTimePurchase() && ownsItem(uuid, itemId)) {
            return PurchaseResult.ALREADY_OWNED;
        }

        // Check balance
        if (economyManager.getArenaPoints(uuid) < item.getCost()) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Deduct AP
        boolean spent = economyManager.spendArenaPoints(uuid, item.getCost(), "Purchase: " + item.getDisplayName());
        if (!spent) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Grant permission
        if (item.getPermissionGranted() != null && !item.getPermissionGranted().isEmpty()) {
            PermissionHelper.grantPermission(uuid, item.getPermissionGranted());
        }

        // Grant group
        if (item.getGroupGranted() != null && !item.getGroupGranted().isEmpty()) {
            PermissionHelper.addToGroup(uuid, item.getGroupGranted());
        }

        // Record purchase
        PlayerEconomyData data = playerDataManager.getData(uuid);
        if (data != null) {
            data.addPurchase(itemId);
        }

        // Log transaction
        playerDataManager.logTransaction(uuid,
            new TransactionRecord("PURCHASE", item.getCost(), "Purchased " + item.getDisplayName()));

        System.out.println("[ShopManager] " + (data != null ? data.getPlayerName() : uuid)
            + " purchased " + item.getDisplayName() + " for " + item.getCost() + " AP");

        return PurchaseResult.SUCCESS;
    }

    // ========== Ownership ==========

    /**
     * Checks if a player owns a specific shop item.
     */
    public boolean ownsItem(UUID uuid, String itemId) {
        PlayerEconomyData data = playerDataManager.getData(uuid);
        return data != null && data.hasPurchased(itemId);
    }
}
