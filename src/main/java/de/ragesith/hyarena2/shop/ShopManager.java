package de.ragesith.hyarena2.shop;

import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.PlayerDataManager;
import de.ragesith.hyarena2.economy.PlayerEconomyData;
import de.ragesith.hyarena2.economy.TransactionRecord;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the shop: purchase logic, ownership checks, item lookups.
 */
public class ShopManager {

    private ShopConfig config;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final PlayerDataManager playerDataManager;
    private final EventBus eventBus;

    // Item lookup cache (built on init)
    private final Map<String, ShopItem> itemsById = new HashMap<>();

    public ShopManager(ShopConfig config, ConfigManager configManager,
                       EconomyManager economyManager,
                       PlayerDataManager playerDataManager, EventBus eventBus) {
        this.config = config;
        this.configManager = configManager;
        this.economyManager = economyManager;
        this.playerDataManager = playerDataManager;
        this.eventBus = eventBus;

        rebuildCache();

        System.out.println("[ShopManager] Loaded " + itemsById.size() + " shop items across "
            + config.getCategories().size() + " categories");
    }

    private void rebuildCache() {
        itemsById.clear();
        for (ShopCategory category : config.getCategories()) {
            for (ShopItem item : category.getItems()) {
                itemsById.put(item.getId(), item);
            }
        }
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

    // ========== Admin CRUD ==========

    /**
     * Saves the entire shop config to disk.
     */
    public void saveShopConfig() {
        configManager.saveConfig("shop.json", config);
    }

    /**
     * Adds or updates an item in the given category.
     * Category must already exist (selected from dropdown).
     */
    public void saveItem(ShopItem item, String categoryId) {
        // Remove item from any existing category first
        for (ShopCategory cat : config.getCategories()) {
            cat.getItems().removeIf(i -> i.getId().equals(item.getId()));
        }

        // Find category
        ShopCategory targetCategory = null;
        for (ShopCategory cat : config.getCategories()) {
            if (cat.getId().equals(categoryId)) {
                targetCategory = cat;
                break;
            }
        }

        if (targetCategory == null) {
            System.err.println("[ShopManager] Cannot save item: category not found: " + categoryId);
            return;
        }

        targetCategory.getItems().add(item);

        rebuildCache();
        saveShopConfig();

        System.out.println("[ShopManager] Saved item: " + item.getId() + " in category: " + categoryId);
    }

    /**
     * Deletes an item by ID.
     */
    public boolean deleteItem(String itemId) {
        boolean removed = false;
        for (ShopCategory cat : config.getCategories()) {
            if (cat.getItems().removeIf(i -> i.getId().equals(itemId))) {
                removed = true;
            }
        }

        if (removed) {
            rebuildCache();
            saveShopConfig();
            System.out.println("[ShopManager] Deleted item: " + itemId);
        }

        return removed;
    }

    /**
     * Checks if an item ID exists.
     */
    public boolean itemExists(String itemId) {
        return itemsById.containsKey(itemId);
    }

    // ========== Category CRUD ==========

    /**
     * Saves or updates a category (by id).
     */
    public void saveCategory(ShopCategory category) {
        ShopCategory existing = null;
        for (ShopCategory cat : config.getCategories()) {
            if (cat.getId().equals(category.getId())) {
                existing = cat;
                break;
            }
        }

        if (existing != null) {
            existing.setDisplayName(category.getDisplayName());
            existing.setSort(category.getSort());
        } else {
            config.getCategories().add(category);
        }

        saveShopConfig();
        System.out.println("[ShopManager] Saved category: " + category.getId());
    }

    /**
     * Deletes a category by id. Only succeeds if the category has no items.
     */
    public boolean deleteCategory(String categoryId) {
        ShopCategory target = null;
        for (ShopCategory cat : config.getCategories()) {
            if (cat.getId().equals(categoryId)) {
                target = cat;
                break;
            }
        }

        if (target == null) return false;

        if (!target.getItems().isEmpty()) return false;

        config.getCategories().remove(target);
        saveShopConfig();
        System.out.println("[ShopManager] Deleted category: " + categoryId);
        return true;
    }

    /**
     * Checks if a category ID exists.
     */
    public boolean categoryExists(String categoryId) {
        for (ShopCategory cat : config.getCategories()) {
            if (cat.getId().equals(categoryId)) return true;
        }
        return false;
    }

    /**
     * Reloads shop config from disk.
     */
    public void reloadConfig() {
        ShopConfig loaded = configManager.loadConfig("shop.json", ShopConfig.class);
        if (loaded != null) {
            this.config = loaded;
        }
        rebuildCache();
        System.out.println("[ShopManager] Reloaded " + itemsById.size() + " shop items");
    }

    /**
     * Gets a flat list of all items across all categories.
     */
    public List<ShopItem> getAllItems() {
        List<ShopItem> all = new ArrayList<>();
        for (ShopCategory category : config.getCategories()) {
            all.addAll(category.getItems());
        }
        return all;
    }

    /**
     * Finds which category contains an item.
     */
    public String getCategoryForItem(String itemId) {
        for (ShopCategory category : config.getCategories()) {
            for (ShopItem item : category.getItems()) {
                if (item.getId().equals(itemId)) {
                    return category.getId();
                }
            }
        }
        return null;
    }

    /**
     * Gets a category by ID.
     */
    public ShopCategory getCategory(String categoryId) {
        for (ShopCategory category : config.getCategories()) {
            if (category.getId().equals(categoryId)) {
                return category;
            }
        }
        return null;
    }
}
