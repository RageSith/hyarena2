package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.shop.ShopCategory;
import de.ragesith.hyarena2.shop.ShopItem;
import de.ragesith.hyarena2.shop.ShopManager;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;

/**
 * Lists all shop items with prices and owned status.
 * Usage: /tshoplist
 */
public class TestShopListCommand extends AbstractPlayerCommand {
    private final ShopManager shopManager;
    private final EconomyManager economyManager;

    public TestShopListCommand(ShopManager shopManager, EconomyManager economyManager) {
        super("tshoplist", "List all shop items");
        requirePermission(Permissions.PLAYER);
        this.shopManager = shopManager;
        this.economyManager = economyManager;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        int balance = economyManager.getArenaPoints(playerRef.getUuid());
        player.sendMessage(TinyMsg.parse("<color:#f1c40f>--- Shop (AP: " + balance + ") ---</color>"));

        if (shopManager.getCategories().isEmpty()) {
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>Shop is empty. Configure shop.json.</color>"));
            return;
        }

        for (ShopCategory category : shopManager.getCategories()) {
            player.sendMessage(TinyMsg.parse("<color:#e8c872>[" + category.getDisplayName() + "]</color>"));
            for (ShopItem item : category.getItems()) {
                boolean owned = shopManager.ownsItem(playerRef.getUuid(), item.getId());
                String status = owned ? "<color:#2ecc71>[OWNED]</color>" : "<color:#f1c40f>" + item.getCost() + " AP</color>";
                player.sendMessage(TinyMsg.parse(
                    "  <color:#b7cedd>" + item.getDisplayName() + "</color> - " + status
                    + " <color:#7f8c8d>(" + item.getId() + ")</color>"
                ));
            }
        }
    }
}
