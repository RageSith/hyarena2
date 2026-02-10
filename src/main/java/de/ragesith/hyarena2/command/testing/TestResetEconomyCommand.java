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
import java.util.ArrayList;

/**
 * Resets own AP, honor, and purchases to zero.
 * Usage: /treseteco
 */
public class TestResetEconomyCommand extends AbstractPlayerCommand {
    private final EconomyManager economyManager;
    private final HonorManager honorManager;

    public TestResetEconomyCommand(EconomyManager economyManager, HonorManager honorManager) {
        super("treseteco", "Reset your economy data");
        requirePermission(Permissions.ADMIN_ECONOMY);
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

        data.setArenaPoints(0);
        data.setHonor(0);
        data.setPurchasedItems(new ArrayList<>());
        data.setLastHonorDecayTimestamp(System.currentTimeMillis());
        honorManager.updatePlayerRank(playerRef.getUuid());

        player.sendMessage(TinyMsg.parse("<color:#f39c12>Economy data reset to zero.</color>"));
    }
}
