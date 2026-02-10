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
    private int sort;

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

    public void setId(String id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setPermissionGranted(String permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    public void setGroupGranted(String groupGranted) {
        this.groupGranted = groupGranted;
    }

    public void setOneTimePurchase(boolean oneTimePurchase) {
        this.oneTimePurchase = oneTimePurchase;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }
}
