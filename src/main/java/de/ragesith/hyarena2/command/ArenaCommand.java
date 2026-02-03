package de.ragesith.hyarena2.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.HyArena2;
import de.ragesith.hyarena2.ui.page.ArenaMenuPage;
import de.ragesith.hyarena2.ui.page.QueueStatusPage;

import javax.annotation.Nonnull;

/**
 * Command to open the arena UI.
 * Usage: /arena
 */
public class ArenaCommand extends AbstractPlayerCommand {

    private final HyArena2 plugin;

    public ArenaCommand(HyArena2 plugin) {
        super("arena", "Opens the arena selection menu");
        this.plugin = plugin;
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
        if (player == null) {
            return;
        }

        // Check if player is in a match - they shouldn't open the menu
        if (plugin.getMatchManager().isPlayerInMatch(playerRef.getUuid())) {
            player.sendMessage(Message.raw("<color:#e74c3c>You cannot open the arena menu while in a match.</color>"));
            return;
        }

        // Close any existing page first
        plugin.getHudManager().closeActivePage(playerRef.getUuid());

        // Check if player is already in queue - show queue status page
        if (plugin.getQueueManager().isInQueue(playerRef.getUuid())) {
            QueueStatusPage queuePage = new QueueStatusPage(
                playerRef,
                playerRef.getUuid(),
                plugin.getQueueManager(),
                plugin.getMatchmaker(),
                plugin.getMatchManager(),
                plugin.getKitManager(),
                plugin.getHudManager(),
                plugin.getScheduler()
            );
            player.getPageManager().openCustomPage(ref, store, queuePage);
            return;
        }

        // Open the arena selection page
        ArenaMenuPage page = new ArenaMenuPage(
            playerRef,
            playerRef.getUuid(),
            plugin.getMatchManager(),
            plugin.getQueueManager(),
            plugin.getKitManager(),
            plugin.getHudManager(),
            plugin.getScheduler()
        );

        player.getPageManager().openCustomPage(ref, store, page);
    }
}
