package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import fi.sulku.hytale.TinyMsg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.queue.QueueEntry;
import de.ragesith.hyarena2.queue.QueueManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Leave the current queue.
 * Usage: /tqleave
 */
public class TestQueueLeaveCommand extends AbstractPlayerCommand {

    private final QueueManager queueManager;

    public TestQueueLeaveCommand(QueueManager queueManager) {
        super("tqleave", "Leave your current queue");
        requirePermission(Permissions.QUEUE);
        this.queueManager = queueManager;
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

        UUID playerUuid = playerRef.getUuid();

        if (!queueManager.isInQueue(playerUuid)) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>You are not in a queue.</color>"));
            return;
        }

        QueueEntry entry = queueManager.leaveQueue(playerUuid, "Left manually");
        if (entry != null) {
            player.sendMessage(TinyMsg.parse("<color:#2ecc71>Left the queue for </color><color:#e8c872>" + entry.getArenaId() + "</color>"));
        } else {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to leave queue.</color>"));
        }
    }
}
