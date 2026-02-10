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
import de.ragesith.hyarena2.economy.HonorManager;
import de.ragesith.hyarena2.economy.PlayerEconomyData;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;

/**
 * Grants honor to self.
 * Usage: /tgivehonor <amount>
 */
public class TestGiveHonorCommand extends AbstractPlayerCommand {
    private final EconomyManager economyManager;
    private final HonorManager honorManager;

    private final com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg<Integer> amountArg =
        withRequiredArg("amount", "Amount of honor to give", ArgTypes.INTEGER);

    public TestGiveHonorCommand(EconomyManager economyManager, HonorManager honorManager) {
        super("tgivehonor", "Give yourself honor");
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

        int amount = amountArg.get(context);
        if (amount <= 0) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Amount must be positive.</color>"));
            return;
        }

        honorManager.addHonor(playerRef.getUuid(), amount);
        PlayerEconomyData data = economyManager.getPlayerData(playerRef.getUuid());
        String rankName = honorManager.getRankDisplayName(playerRef.getUuid());
        double honor = data != null ? data.getHonor() : 0;

        player.sendMessage(TinyMsg.parse("<color:#2ecc71>+" + amount + " Honor</color> <color:#b7cedd>(Honor: " + String.format("%.1f", honor) + ", Rank: " + rankName + ")</color>"));
    }
}
