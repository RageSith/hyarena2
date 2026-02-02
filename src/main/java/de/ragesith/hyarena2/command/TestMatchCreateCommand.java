package de.ragesith.hyarena2.command;

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
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;

import javax.annotation.Nonnull;

/**
 * Creates a new match.
 * Usage: /tmcreate <arenaId>
 */
public class TestMatchCreateCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;

    private final RequiredArg<String> arenaIdArg =
        withRequiredArg("arenaId", "The arena ID to use", ArgTypes.STRING);

    public TestMatchCreateCommand(MatchManager matchManager) {
        super("tmcreate", "Create a new match");
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

        String arenaId = arenaIdArg.get(context);
        Match match = matchManager.createMatch(arenaId);

        if (match == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to create match. Arena '" + arenaId + "' not found or world not loaded.</color>"));
            return;
        }

        // Auto-join the creator
        if (matchManager.addPlayerToMatch(match.getMatchId(), player)) {
            player.sendMessage(TinyMsg.parse("<color:#2ecc71>Created and joined match: </color><color:#f1c40f>" + match.getMatchId() + "</color>"));
        } else {
            player.sendMessage(TinyMsg.parse("<color:#2ecc71>Created match: </color><color:#f1c40f>" + match.getMatchId() + "</color>"));
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to auto-join.</color>"));
        }
    }
}
