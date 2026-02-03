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
import de.ragesith.hyarena2.queue.QueueManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Join a queue for an arena.
 * Usage: /tqjoin <arenaId>
 */
public class TestQueueJoinCommand extends AbstractPlayerCommand {

    private final QueueManager queueManager;
    private final MatchManager matchManager;

    private final RequiredArg<String> arenaIdArg =
        withRequiredArg("arenaId", "The arena ID to queue for", ArgTypes.STRING);

    public TestQueueJoinCommand(QueueManager queueManager, MatchManager matchManager) {
        super("tqjoin", "Join a queue for an arena");
        requirePermission(Permissions.QUEUE);
        this.queueManager = queueManager;
        this.matchManager = matchManager;
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

        // Try to join queue
        QueueManager.JoinResult result = queueManager.joinQueue(playerUuid, playerName, arenaId);

        switch (result) {
            case SUCCESS:
                int position = queueManager.getQueuePosition(playerUuid);
                int queueSize = queueManager.getQueueSize(arenaId);
                player.sendMessage(TinyMsg.parse(
                    "<color:#2ecc71>Joined queue for </color><color:#e8c872>" + arena.getDisplayName() + "</color>" +
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
        }
    }
}
