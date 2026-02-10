package de.ragesith.hyarena2.shop;

import java.util.ArrayList;
import java.util.List;

/**
 * A category of shop items (e.g., "Services", "Kits").
 */
public class ShopCategory {
    private String id;
    private String displayName;
    private List<ShopItem> items = new ArrayList<>();

    public ShopCategory() {}

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<ShopItem> getItems() {
        if (items == null) items = new ArrayList<>();
        return items;
    }
}
