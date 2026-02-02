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

import javax.annotation.Nonnull;

/**
 * Command to open the arena UI.
 * Usage: /arena
 *
 * Phase 1: Placeholder implementation - shows message.
 * Phase 6: Will open full UI page.
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

        // Phase 1: Placeholder - show info message
        // Phase 6: Will open UI page here
        player.sendMessage(Message.raw("[HyArena2] Arena menu coming soon!"));
        player.sendMessage(Message.raw("Hub: " + plugin.getHubConfig().getEffectiveWorldName()));
        player.sendMessage(Message.raw("Spawn: " + plugin.getHubConfig().getSpawnPoint()));
    }
}
