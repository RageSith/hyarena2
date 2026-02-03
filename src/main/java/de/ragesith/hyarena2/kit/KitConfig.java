package de.ragesith.hyarena2.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration data for a kit, loaded from JSON files in config/kits/
 */
public class KitConfig {
    private String id;
    private String displayName;
    private String description;
    private String permission;      // Optional permission for access control
    private List<String> items;     // "item_id" or "item_id:quantity"
    private Map<String, String> armor;  // helmet, chest, hands, legs
    private String offhand;

    public KitConfig() {
        this.id = "default";
        this.displayName = "Default Kit";
        this.description = "A basic starting kit";
        this.items = new ArrayList<>();
        this.armor = new HashMap<>();
    }

    public KitConfig(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.description = "";
        this.items = new ArrayList<>();
        this.armor = new HashMap<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the permission required to use this kit.
     * @return permission string, or null if no permission required
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the list of item identifiers.
     * Format: "item_id" or "item_id:quantity" (e.g., "arrow:64")
     */
    public List<String> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    /**
     * Gets the armor configuration.
     * Keys: "helmet", "chest", "hands", "legs"
     * Values: item identifiers
     */
    public Map<String, String> getArmor() {
        return armor != null ? armor : new HashMap<>();
    }

    /**
     * Gets the offhand/utility slot item (e.g., shield, torch).
     */
    public String getOffhand() {
        return offhand;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public void setArmor(Map<String, String> armor) {
        this.armor = armor;
    }

    public void setOffhand(String offhand) {
        this.offhand = offhand;
    }

    // Convenience armor setters
    public void setHelmet(String helmet) {
        if (armor == null) armor = new HashMap<>();
        armor.put("helmet", helmet);
    }

    public void setChestplate(String chest) {
        if (armor == null) armor = new HashMap<>();
        armor.put("chest", chest);
    }

    public void setLeggings(String legs) {
        if (armor == null) armor = new HashMap<>();
        armor.put("legs", legs);
    }

    public void setHands(String hands) {
        if (armor == null) armor = new HashMap<>();
        armor.put("hands", hands);
    }

    /**
     * Checks if this kit is properly configured.
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && displayName != null && !displayName.isEmpty();
    }

    /**
     * Checks if this kit requires a permission.
     */
    public boolean requiresPermission() {
        return permission != null && !permission.isEmpty();
    }
}
