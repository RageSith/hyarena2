package de.ragesith.hyarena2.command.testing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import fi.sulku.hytale.TinyMsg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Joins a match.
 * Usage: /tmjoin [matchId]
 * If no matchId provided, joins the first waiting match.
 */
public class TestMatchJoinCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;

    private final OptionalArg<String> matchIdArg =
        withOptionalArg("matchId", "The match UUID to join (optional)", ArgTypes.STRING);

    public TestMatchJoinCommand(MatchManager matchManager) {
        super("tmjoin", "Join a match");
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

        UUID matchId = null;
        String matchIdStr = matchIdArg.get(context);

        if (matchIdStr != null && !matchIdStr.isEmpty()) {
            // Try to parse provided UUID
            try {
                matchId = UUID.fromString(matchIdStr);
            } catch (IllegalArgumentException e) {
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>Invalid match ID format.</color>"));
                return;
            }
        } else {
            // No UUID provided - find first waiting match
            Match waitingMatch = matchManager.getFirstWaitingMatch();
            if (waitingMatch == null) {
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>No waiting matches found. Create one with /tmcreate <arena></color>"));
                return;
            }
            matchId = waitingMatch.getMatchId();
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>Auto-joining match: </color><color:#f1c40f>" + matchId + "</color>"));
        }

        if (matchManager.addPlayerToMatch(matchId, player)) {
            player.sendMessage(TinyMsg.parse("<color:#2ecc71>Joined match!</color>"));
        } else {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to join. Match not found, full, or already started.</color>"));
        }
    }
}
