package de.ragesith.hyarena2.kit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.event.EventBus;
import de.ragesith.hyarena2.event.kit.KitAppliedEvent;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages kit loading, application, and access control.
 */
public class KitManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path kitsDirectory;
    private final EventBus eventBus;
    private final Map<String, KitConfig> kits = new ConcurrentHashMap<>();

    // Track current hotbar slot during kit application (thread-local for safety)
    private final ThreadLocal<Short> currentHotbarSlot = ThreadLocal.withInitial(() -> (short) 0);

    // Per-player, per-arena kit preferences: playerUuid -> arenaId -> kitId
    private final Map<UUID, Map<String, String>> preferredKits = new ConcurrentHashMap<>();

    // MatchManager reference for arena lookups (set after construction)
    private MatchManager matchManager;

    public KitManager(Path configRoot, EventBus eventBus) {
        this.kitsDirectory = configRoot.resolve("kits");
        this.eventBus = eventBus;
    }

    /**
     * Sets the MatchManager reference for arena lookups.
     */
    public void setMatchManager(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    /**
     * Saves a player's preferred kit for an arena.
     */
    public void setPreferredKit(UUID playerUuid, String arenaId, String kitId) {
        preferredKits.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(arenaId, kitId);
    }

    /**
     * Gets a player's preferred kit for an arena, or null if none saved.
     */
    public String getPreferredKit(UUID playerUuid, String arenaId) {
        Map<String, String> playerPrefs = preferredKits.get(playerUuid);
        return playerPrefs != null ? playerPrefs.get(arenaId) : null;
    }

    /**
     * Loads all kits from the kits directory.
     * @return this KitManager for method chaining
     */
    public KitManager loadKits() {
        kits.clear();

        // Create kits directory if it doesn't exist
        try {
            Files.createDirectories(kitsDirectory);
        } catch (IOException e) {
            System.err.println("[KitManager] Failed to create kits directory: " + e.getMessage());
            return this;
        }

        // Load all .json files from kits directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(kitsDirectory, "*.json")) {
            for (Path file : stream) {
                loadKit(file);
            }
        } catch (IOException e) {
            System.err.println("[KitManager] Failed to read kits directory: " + e.getMessage());
        }

        System.out.println("[KitManager] Loaded " + kits.size() + " kits");
        return this;
    }

    /**
     * Loads a single kit from a file.
     */
    private void loadKit(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            KitConfig kit = GSON.fromJson(reader, KitConfig.class);
            if (kit != null && kit.isValid()) {
                kits.put(kit.getId(), kit);
                String permInfo = kit.requiresPermission()
                    ? "permission=" + kit.getPermission()
                    : "no permission required";
                System.out.println("[KitManager] Loaded kit: " + kit.getId() +
                    " (" + kit.getDisplayName() + ") [" + permInfo + "]");
            } else {
                System.err.println("[KitManager] Invalid kit file: " + file.getFileName());
            }
        } catch (IOException e) {
            System.err.println("[KitManager] Failed to load kit " + file.getFileName() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[KitManager] Error parsing kit " + file.getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * Saves a kit config to disk and updates the runtime map.
     *
     * @param config the kit configuration to save
     * @return true if saved successfully
     */
    public boolean saveKit(KitConfig config) {
        if (config == null || !config.isValid()) {
            System.err.println("[KitManager] Cannot save invalid kit config");
            return false;
        }

        // Save to disk
        try {
            Files.createDirectories(kitsDirectory);
            Path file = kitsDirectory.resolve(config.getId() + ".json");
            try (java.io.Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[KitManager] Failed to save kit " + config.getId() + ": " + e.getMessage());
            return false;
        }

        // Update runtime map
        kits.put(config.getId(), config);
        System.out.println("[KitManager] Saved kit: " + config.getDisplayName() + " (" + config.getId() + ")");
        return true;
    }

    /**
     * Deletes a kit from disk and runtime.
     *
     * @param kitId the kit ID to delete
     * @return true if deleted successfully
     */
    public boolean deleteKit(String kitId) {
        Path file = kitsDirectory.resolve(kitId + ".json");
        try {
            boolean deleted = Files.deleteIfExists(file);
            kits.remove(kitId);
            if (deleted) {
                System.out.println("[KitManager] Deleted kit: " + kitId);
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("[KitManager] Failed to delete kit " + kitId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Reloads all kits from disk.
     */
    public void reloadKits() {
        loadKits();
    }

    /**
     * Gets a kit by ID.
     */
    public KitConfig getKit(String kitId) {
        return kits.get(kitId);
    }

    /**
     * Gets all available kits.
     */
    public List<KitConfig> getAllKits() {
        return new ArrayList<>(kits.values());
    }

    /**
     * Checks if a kit exists.
     */
    public boolean kitExists(String kitId) {
        return kits.containsKey(kitId);
    }

    /**
     * Checks if a kit is allowed in an arena.
     */
    public boolean isKitInArena(String kitId, String arenaId) {
        if (matchManager == null) return false;
        Arena arena = matchManager.getArena(arenaId);
        if (arena == null) return false;
        List<String> allowedKits = arena.getConfig().getAllowedKits();
        return allowedKits != null && allowedKits.contains(kitId);
    }

    /**
     * Checks if a player has permission to use a kit.
     */
    public boolean canPlayerUseKit(Player player, String kitId) {
        KitConfig kit = getKit(kitId);
        if (kit == null) return false;

        // No permission required
        if (!kit.requiresPermission()) {
            return true;
        }

        // Check bypass permission first
        if (player.hasPermission(de.ragesith.hyarena2.Permissions.BYPASS_KIT)) {
            return true;
        }

        // Check kit-specific permission
        String requiredPermission = kit.getPermission();
        boolean hasPermission = player.hasPermission(requiredPermission);

        // Debug logging
        System.out.println("[KitManager] Permission check for kit '" + kitId + "': " +
            "required='" + requiredPermission + "', hasPermission=" + hasPermission +
            ", player=" + player.getDisplayName());

        return hasPermission;
    }

    /**
     * Combined check: can player select this kit for this arena?
     */
    public boolean canSelectKit(Player player, String kitId, String arenaId) {
        return isKitInArena(kitId, arenaId) && canPlayerUseKit(player, kitId);
    }

    /**
     * Gets all kits available in an arena with access status for a player.
     * Used for UI display.
     */
    public List<KitAccessInfo> getKitsForArena(Player player, String arenaId) {
        if (matchManager == null) return Collections.emptyList();

        Arena arena = matchManager.getArena(arenaId);
        if (arena == null) return Collections.emptyList();

        List<String> allowedKits = arena.getConfig().getAllowedKits();
        if (allowedKits == null || allowedKits.isEmpty()) {
            return Collections.emptyList();
        }

        List<KitAccessInfo> result = new ArrayList<>();
        for (String kitId : allowedKits) {
            KitConfig kit = getKit(kitId);
            if (kit != null) {
                boolean isUnlocked = canPlayerUseKit(player, kitId);
                result.add(new KitAccessInfo(kit, isUnlocked));
            }
        }
        return result;
    }

    /**
     * Gets the first available kit for an arena that the player can use.
     * Used as default selection.
     */
    public KitConfig getDefaultKit(Player player, String arenaId) {
        List<KitAccessInfo> kitsForArena = getKitsForArena(player, arenaId);
        for (KitAccessInfo info : kitsForArena) {
            if (info.isUnlocked()) {
                return info.getKit();
            }
        }
        // Return first kit even if locked (UI will show it as locked)
        return kitsForArena.isEmpty() ? null : kitsForArena.get(0).getKit();
    }

    /**
     * Applies a kit to a player, clearing their inventory first.
     *
     * @param player the player to apply the kit to
     * @param kitId  the kit ID to apply
     * @return true if the kit was applied successfully
     */
    public boolean applyKit(Player player, String kitId) {
        KitConfig kit = getKit(kitId);
        if (kit == null) {
            System.err.println("[KitManager] Kit not found: " + kitId);
            return false;
        }

        // Clear player inventory first
        clearInventory(player);

        // Reset hotbar slot counter
        currentHotbarSlot.set((short) 0);

        // Apply items to hotbar
        for (String itemEntry : kit.getItems()) {
            giveItemToHotbar(player, itemEntry);
        }

        // Apply armor
        Map<String, String> armor = kit.getArmor();
        if (armor.containsKey("helmet")) {
            giveArmor(player, "helmet", armor.get("helmet"));
        }
        if (armor.containsKey("chest")) {
            giveArmor(player, "chest", armor.get("chest"));
        }
        if (armor.containsKey("legs")) {
            giveArmor(player, "legs", armor.get("legs"));
        }
        if (armor.containsKey("hands")) {
            giveArmor(player, "hands", armor.get("hands"));
        }

        // Apply offhand/utility item (shield, torch, etc.)
        String offhand = kit.getOffhand();
        if (offhand != null && !offhand.isEmpty()) {
            giveOffhand(player, offhand);
        }

        // Note: Healing is handled by Match after kit application to ensure armor stats are applied

        // Get player UUID for event
        UUID playerUuid = getPlayerUuid(player);
        if (playerUuid != null) {
            eventBus.publish(new KitAppliedEvent(playerUuid, kitId));
        }

        System.out.println("[KitManager] Applied kit '" + kitId + "' to " + player.getDisplayName());
        return true;
    }

    /**
     * Clears a player's inventory (hotbar, storage, armor, utility).
     * Called at match end before teleporting back to hub.
     */
    public void clearInventory(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar != null) {
            hotbar.clear();
        }

        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            storage.clear();
        }

        ItemContainer armor = inventory.getArmor();
        if (armor != null) {
            armor.clear();
        }

        ItemContainer utility = inventory.getUtility();
        if (utility != null) {
            utility.clear();
        }
    }

    /**
     * Alias for clearInventory - used when clearing kit at match end.
     */
    public void clearKit(Player player) {
        clearInventory(player);
    }

    /**
     * Gives an item to a player's hotbar.
     *
     * @param player    the player
     * @param itemEntry item format: "item_id" or "item_id:quantity"
     */
    private void giveItemToHotbar(Player player, String itemEntry) {
        String itemId;
        int quantity = 1;

        if (itemEntry.contains(":")) {
            String[] parts = itemEntry.split(":");
            itemId = parts[0];
            try {
                quantity = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                quantity = 1;
            }
        } else {
            itemId = itemEntry;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            return;
        }

        try {
            ItemStack item = new ItemStack(itemId, quantity);
            short slot = currentHotbarSlot.get();
            hotbar.setItemStackForSlot(slot, item);
            currentHotbarSlot.set((short) (slot + 1));
        } catch (Exception e) {
            System.err.println("[KitManager] Failed to give item " + itemId + ": " + e.getMessage());
        }
    }

    /**
     * Gives armor to a player.
     *
     * @param player the player
     * @param slot   the armor slot (helmet, chest, legs, hands)
     * @param itemId the item ID
     */
    private void giveArmor(Player player, String slot, String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemContainer armor = inventory.getArmor();
        if (armor == null) {
            return;
        }

        // Armor slots: 0=helmet, 1=chest, 2=hands, 3=legs
        short slotIndex;
        switch (slot) {
            case "helmet":
                slotIndex = 0;
                break;
            case "chest":
                slotIndex = 1;
                break;
            case "hands":
                slotIndex = 2;
                break;
            case "legs":
                slotIndex = 3;
                break;
            default:
                return;
        }

        try {
            ItemStack armorItem = new ItemStack(itemId);
            armor.setItemStackForSlot(slotIndex, armorItem);
        } catch (Exception e) {
            System.err.println("[KitManager] Failed to set armor " + itemId + ": " + e.getMessage());
        }
    }

    /**
     * Gives an offhand/utility item to a player (shield, torch, etc.).
     *
     * @param player the player
     * @param itemId the item ID
     */
    private void giveOffhand(Player player, String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemContainer utility = inventory.getUtility();
        if (utility == null) {
            return;
        }

        try {
            ItemStack item = new ItemStack(itemId);
            utility.setItemStackForSlot((short) 0, item);
        } catch (Exception e) {
            System.err.println("[KitManager] Failed to set offhand " + itemId + ": " + e.getMessage());
        }
    }

    /**
     * Heals a player to full health.
     */
    private void healPlayer(Player player) {
        try {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            EntityStatMap entityStatMap = store.getComponent(ref,
                EntityStatsModule.get().getEntityStatMapComponentType());

            if (entityStatMap != null) {
                int healthIndex = EntityStatType.getAssetMap().getIndex("health");
                EntityStatValue healthStat = entityStatMap.get(healthIndex);

                if (healthStat != null) {
                    float maxHealth = healthStat.getMax();
                    entityStatMap.setStatValue(healthIndex, maxHealth);
                }
            }
        } catch (Exception e) {
            System.err.println("[KitManager] Failed to heal player: " + e.getMessage());
        }
    }

    /**
     * Gets a player's UUID.
     */
    private UUID getPlayerUuid(Player player) {
        try {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null) return null;
            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return null;
            return playerRef.getUuid();
        } catch (Exception e) {
            return null;
        }
    }
}
