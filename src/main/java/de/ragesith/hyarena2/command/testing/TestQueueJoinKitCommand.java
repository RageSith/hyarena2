package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import fi.sulku.hytale.TinyMsg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.queue.QueueManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Join a queue for an arena with kit selection.
 * Usage: /tqjoinkit <arenaId> <kitId>
 */
public class TestQueueJoinKitCommand extends AbstractPlayerCommand {

    private final QueueManager queueManager;
    private final MatchManager matchManager;
    private final KitManager kitManager;

    private final RequiredArg<String> arenaIdArg =
        withRequiredArg("arenaId", "The arena ID to queue for", ArgTypes.STRING);

    private final RequiredArg<String> kitIdArg =
        withRequiredArg("kitId", "The kit ID to use", ArgTypes.STRING);

    public TestQueueJoinKitCommand(QueueManager queueManager, MatchManager matchManager, KitManager kitManager) {
        super("tqjoinkit", "Join a queue for an arena with a kit");
        requirePermission(Permissions.QUEUE);
        this.queueManager = queueManager;
        this.matchManager = matchManager;
        this.kitManager = kitManager;
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

        String arenaId = arenaIdArg.get(context);
        String kitId = kitIdArg.get(context);
        UUID playerUuid = playerRef.getUuid();
        String playerName = player.getDisplayName();

        // Validate arena exists
        Arena arena = matchManager.getArena(arenaId);
        if (arena == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Arena not found: " + arenaId + "</color>"));
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>Available arenas: </color>"));
            for (Arena a : matchManager.getArenas()) {
                player.sendMessage(TinyMsg.parse("<color:#3498db>  - " + a.getId() + "</color> <color:#7f8c8d>(" + a.getDisplayName() + ")</color>"));
            }
            return;
        }

        // Validate kit exists
        if (!kitManager.kitExists(kitId)) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Kit not found: " + kitId + "</color>"));
            showAvailableKits(player, arenaId);
            return;
        }

        // Validate kit is allowed in arena
        if (!kitManager.isKitInArena(kitId, arenaId)) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Kit '" + kitId + "' is not allowed in this arena.</color>"));
            showAvailableKits(player, arenaId);
            return;
        }

        // Validate player has permission for kit
        if (!kitManager.canPlayerUseKit(player, kitId)) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>You don't have permission to use kit: " + kitId + "</color>"));
            return;
        }

        // Try to join queue with kit
        QueueManager.JoinResult result = queueManager.joinQueue(playerUuid, playerName, arenaId, kitId);

        switch (result) {
            case SUCCESS:
                int position = queueManager.getQueuePosition(playerUuid);
                int queueSize = queueManager.getQueueSize(arenaId);
                player.sendMessage(TinyMsg.parse(
                    "<color:#2ecc71>Joined queue for </color><color:#e8c872>" + arena.getDisplayName() + "</color>" +
                    "<color:#2ecc71> with kit </color><color:#e8c872>" + kitId + "</color>" +
                    "<color:#2ecc71>! Position: #" + position + " (" + queueSize + "/" + arena.getConfig().getMaxPlayers() + ")</color>"
                ));
                break;

            case ALREADY_IN_QUEUE:
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>You are already in a queue. Use /tqleave first.</color>"));
                break;

            case IN_MATCH:
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>You cannot queue while in a match.</color>"));
                break;

            case ON_COOLDOWN:
                int remaining = queueManager.getRemainingCooldownSeconds(playerUuid);
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>Please wait " + remaining + "s before rejoining a queue.</color>"));
                break;

            case NOT_IN_HUB:
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>You must be in the hub to join a queue.</color>"));
                break;

            case INVALID_KIT:
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>Invalid kit selection.</color>"));
                showAvailableKits(player, arenaId);
                break;
        }
    }

    private void showAvailableKits(Player player, String arenaId) {
        Arena arena = matchManager.getArena(arenaId);
        if (arena == null) return;

        List<String> allowedKits = arena.getConfig().getAllowedKits();
        if (allowedKits == null || allowedKits.isEmpty()) {
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>No kits configured for this arena.</color>"));
            return;
        }

        player.sendMessage(TinyMsg.parse("<color:#7f8c8d>Available kits for this arena:</color>"));
        for (String kit : allowedKits) {
            boolean hasAccess = kitManager.canPlayerUseKit(player, kit);
            String color = hasAccess ? "#2ecc71" : "#e74c3c";
            String status = hasAccess ? "" : " (locked)";
            player.sendMessage(TinyMsg.parse("<color:" + color + ">  - " + kit + "</color><color:#7f8c8d>" + status + "</color>"));
        }
    }
}
