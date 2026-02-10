package de.ragesith.hyarena2.shop;

/**
 * A single purchasable item in the shop.
 */
public class ShopItem {
    private String id;
    private String displayName;
    private String description;
    private int cost;
    private String permissionGranted;
    private String groupGranted;
    private boolean oneTimePurchase = true;

    public ShopItem() {}

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getCost() {
        return cost;
    }

    public String getPermissionGranted() {
        return permissionGranted;
    }

    public String getGroupGranted() {
        return groupGranted;
    }

    public boolean isOneTimePurchase() {
        return oneTimePurchase;
    }
}
