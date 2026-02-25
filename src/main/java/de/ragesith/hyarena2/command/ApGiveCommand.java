
package de.ragesith.hyarena2.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.PlayerEconomyData;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Give AP to a player. Works from both player and server console.
 * Usage: /hyapgive <playername> <amount>
 */
public class ApGiveCommand extends AbstractAsyncCommand {

    private final EconomyManager economyManager;

    private final RequiredArg<String> playerArg =
        withRequiredArg("player", "Target player name", ArgTypes.STRING);
    private final RequiredArg<Integer> amountArg =
        withRequiredArg("amount", "Amount of AP to give", ArgTypes.INTEGER);

    public ApGiveCommand(EconomyManager economyManager) {
        super("hyapgive", "Give ArenaPoints to a player");
        requirePermission(Permissions.ADMIN_ECONOMY);
        this.economyManager = economyManager;
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        String targetName = playerArg.get(context);
        int amount = amountArg.get(context);

        if (amount <= 0) {
            context.sendMessage(Message.raw("Amount must be a positive number."));
            return CompletableFuture.completedFuture(null);
        }

        // Look up player by name from the economy cache (thread-safe, no world thread needed)
        UUID targetUuid = null;
        for (PlayerEconomyData data : economyManager.getPlayerDataManager().getAllLoadedPlayers()) {
            if (data.getPlayerName() != null && data.getPlayerName().equalsIgnoreCase(targetName)) {
                targetUuid = data.getPlayerUuid();
                break;
            }
        }

        if (targetUuid == null) {
            context.sendMessage(Message.raw("Player '" + targetName + "' not found (must be online)."));
            return CompletableFuture.completedFuture(null);
        }

        // Add AP (ConcurrentHashMap-backed, safe from any thread)
        economyManager.addArenaPoints(targetUuid, amount, "admin grant via /hyapgive");
        int newBalance = economyManager.getArenaPoints(targetUuid);

        // Confirm to sender
        context.sendMessage(Message.raw("Gave " + amount + " AP to " + targetName + " (new balance: " + newBalance + ")"));

        // Notify target player (dispatch to their world thread for safe Player access)
        PlayerRef playerRef = Universe.get().getPlayer(targetUuid);
        if (playerRef != null) {
            for (World world : Universe.get().getWorlds().values()) {
                if (world.getPlayerRefs().contains(playerRef)) {
                    world.execute(() -> {
                        Ref<EntityStore> ref = playerRef.getReference();
                        if (ref == null) return;
                        Store<EntityStore> store = ref.getStore();
                        if (store == null) return;
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player == null) return;
                        player.sendMessage(TinyMsg.parse(
                            "<color:#2ecc71>+" + amount + " AP</color> <color:#b7cedd>(Balance: " + newBalance + ")</color>"));
                    });
                    break;
                }
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
