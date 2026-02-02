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
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;

import javax.annotation.Nonnull;

/**
 * Lists active matches.
 * Usage: /tmlist
 */
public class TestMatchListCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;

    public TestMatchListCommand(MatchManager matchManager) {
        super("tmlist", "List active matches");
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

        player.sendMessage(TinyMsg.parse("<color:#f39c12>Active matches:</color>"));
        if (matchManager.getActiveMatches().isEmpty()) {
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>No active matches.</color>"));
            return;
        }

        for (Match match : matchManager.getActiveMatches()) {
            player.sendMessage(TinyMsg.parse("<color:#f1c40f>" + match.getMatchId() + "</color> <color:#7f8c8d>- " +
                    match.getArena().getDisplayName() + " (" + match.getState() + ") - " +
                    match.getParticipants().size() + " players</color>"));
        }
    }
}
