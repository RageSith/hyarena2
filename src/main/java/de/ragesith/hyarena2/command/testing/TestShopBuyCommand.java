package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.shop.PurchaseResult;
import de.ragesith.hyarena2.shop.ShopItem;
import de.ragesith.hyarena2.shop.ShopManager;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;

/**
 * Purchases a shop item.
 * Usage: /tshopbuy <itemId>
 */
public class TestShopBuyCommand extends AbstractPlayerCommand {
    private final ShopManager shopManager;
    private final EconomyManager economyManager;

    private final com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg<String> itemIdArg =
        withRequiredArg("itemId", "Shop item ID to purchase", ArgTypes.STRING);

    public TestShopBuyCommand(ShopManager shopManager, EconomyManager economyManager) {
        super("tshopbuy", "Purchase a shop item");
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

        String itemId = itemIdArg.get(context);
        PurchaseResult result = shopManager.purchase(playerRef.getUuid(), itemId);

        switch (result) {
            case SUCCESS -> {
                ShopItem item = shopManager.getItem(itemId);
                int balance = economyManager.getArenaPoints(playerRef.getUuid());
                player.sendMessage(TinyMsg.parse(
                    "<color:#2ecc71>Purchased " + (item != null ? item.getDisplayName() : itemId) + "!</color>"
                    + " <color:#b7cedd>(Remaining AP: " + balance + ")</color>"
                ));
            }
            case INSUFFICIENT_FUNDS -> {
                ShopItem item = shopManager.getItem(itemId);
                int balance = economyManager.getArenaPoints(playerRef.getUuid());
                int cost = item != null ? item.getCost() : 0;
                player.sendMessage(TinyMsg.parse(
                    "<color:#e74c3c>Insufficient funds! Need " + cost + " AP, have " + balance + ".</color>"
                ));
            }
            case ALREADY_OWNED ->
                player.sendMessage(TinyMsg.parse("<color:#f39c12>You already own this item.</color>"));
            case ITEM_NOT_FOUND ->
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>Item not found: " + itemId + "</color>"));
        }
    }
}
