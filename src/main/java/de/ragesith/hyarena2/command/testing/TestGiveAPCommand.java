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
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;

/**
 * Grants AP to self.
 * Usage: /tgiveap <amount>
 */
public class TestGiveAPCommand extends AbstractPlayerCommand {
    private final EconomyManager economyManager;

    private final com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg<Integer> amountArg =
        withRequiredArg("amount", "Amount of AP to give", ArgTypes.INTEGER);

    public TestGiveAPCommand(EconomyManager economyManager) {
        super("tgiveap", "Give yourself ArenaPoints");
        requirePermission(Permissions.ADMIN_ECONOMY);
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

        int amount = amountArg.get(context);
        if (amount <= 0) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Amount must be positive.</color>"));
            return;
        }

        economyManager.addArenaPoints(playerRef.getUuid(), amount, "admin grant");
        int newBalance = economyManager.getArenaPoints(playerRef.getUuid());
        player.sendMessage(TinyMsg.parse("<color:#2ecc71>+" + amount + " AP</color> <color:#b7cedd>(Balance: " + newBalance + ")</color>"));
    }
}
