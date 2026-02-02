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
import de.ragesith.hyarena2.arena.MatchManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Leave current match.
 * Usage: /tmleave
 */
public class TestMatchLeaveCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;

    public TestMatchLeaveCommand(MatchManager matchManager) {
        super("tmleave", "Leave your current match");
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

        UUID playerUuid = playerRef.getUuid();
        if (!matchManager.isPlayerInMatch(playerUuid)) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>You are not in a match.</color>"));
            return;
        }

        matchManager.removePlayerFromMatch(playerUuid, "Left manually");
        player.sendMessage(TinyMsg.parse("<color:#2ecc71>You left the match.</color>"));
    }
}
