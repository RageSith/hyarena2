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
import de.ragesith.hyarena2.economy.HonorManager;
import de.ragesith.hyarena2.economy.PlayerEconomyData;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;

/**
 * Shows own AP, honor, rank, and purchases.
 * Usage: /tecon
 */
public class TestEconomyInfoCommand extends AbstractPlayerCommand {
    private final EconomyManager economyManager;
    private final HonorManager honorManager;

    public TestEconomyInfoCommand(EconomyManager economyManager, HonorManager honorManager) {
        super("tecon", "Show your economy info");
        requirePermission(Permissions.PLAYER);
        this.economyManager = economyManager;
        this.honorManager = honorManager;
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

        PlayerEconomyData data = economyManager.getPlayerData(playerRef.getUuid());
        if (data == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Economy data not loaded.</color>"));
            return;
        }

        String rankName = honorManager.getRankDisplayName(playerRef.getUuid());
        String purchases = data.getPurchasedItems().isEmpty() ? "none" : String.join(", ", data.getPurchasedItems());

        player.sendMessage(TinyMsg.parse("<color:#f1c40f>--- Economy Info ---</color>"));
        player.sendMessage(TinyMsg.parse("<color:#b7cedd>AP: </color><color:#f1c40f>" + data.getArenaPoints() + "</color>"));
        player.sendMessage(TinyMsg.parse("<color:#b7cedd>Honor: </color><color:#3498db>" + String.format("%.1f", data.getHonor()) + "</color>"));
        player.sendMessage(TinyMsg.parse("<color:#b7cedd>Rank: </color><color:#2ecc71>" + rankName + "</color>"));
        player.sendMessage(TinyMsg.parse("<color:#b7cedd>Purchases: </color><color:#96a9be>" + purchases + "</color>"));
    }
}
