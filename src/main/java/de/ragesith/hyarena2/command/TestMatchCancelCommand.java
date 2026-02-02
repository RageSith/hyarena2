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
import java.util.UUID;

/**
 * Cancel a match.
 * Usage: /tmcancel <matchId>
 */
public class TestMatchCancelCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;

    private final RequiredArg<String> matchIdArg =
        withRequiredArg("matchId", "The match UUID to cancel", ArgTypes.STRING);

    public TestMatchCancelCommand(MatchManager matchManager) {
        super("tmcancel", "Cancel a match");
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

        String matchIdStr = matchIdArg.get(context);
        UUID matchId;
        try {
            matchId = UUID.fromString(matchIdStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Invalid match ID format.</color>"));
            return;
        }

        Match match = matchManager.getMatch(matchId);
        if (match == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Match not found.</color>"));
            return;
        }

        match.cancel("Cancelled by admin");
        player.sendMessage(TinyMsg.parse("<color:#2ecc71>Match cancelled.</color>"));
    }
}
