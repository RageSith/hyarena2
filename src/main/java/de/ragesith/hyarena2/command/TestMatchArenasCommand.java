package de.ragesith.hyarena2.command;

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
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.MatchManager;

import javax.annotation.Nonnull;

/**
 * Lists available arenas.
 * Usage: /tmarenas
 */
public class TestMatchArenasCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;

    public TestMatchArenasCommand(MatchManager matchManager) {
        super("tmarenas", "List available arenas");
        requirePermission(Permissions.ADMIN_MATCH);
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

        player.sendMessage(TinyMsg.parse("<color:#f39c12>Available arenas:</color>"));
        if (matchManager.getArenas().isEmpty()) {
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>No arenas loaded. Check config/arenas/</color>"));
            return;
        }
        for (Arena arena : matchManager.getArenas()) {
            player.sendMessage(TinyMsg.parse("<color:#f1c40f>- " + arena.getId() + "</color> <color:#7f8c8d>(" + arena.getDisplayName() +
                    ", " + arena.getMinPlayers() + "-" + arena.getMaxPlayers() + " players)</color>"));
        }
    }
}
