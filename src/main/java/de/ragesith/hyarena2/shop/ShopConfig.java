package de.ragesith.hyarena2.shop;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level shop configuration loaded from shop.json.
 */
public class ShopConfig {
    private List<ShopCategory> categories = new ArrayList<>();

    public ShopConfig() {}

    public List<ShopCategory> getCategories() {
        if (categories == null) categories = new ArrayList<>();
        return categories;
    }
}
