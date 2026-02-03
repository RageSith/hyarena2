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
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.arena.MatchState;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Force-starts the match the player is currently in.
 * Usage: /tmstart
 */
public class TestMatchStartCommand extends AbstractPlayerCommand {
    private final MatchManager matchManager;

    public TestMatchStartCommand(MatchManager matchManager) {
        super("tmstart", "Force-start your current match");
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
        System.out.println("[TestMatchStartCommand] Player UUID: " + playerUuid);
        Match match = matchManager.getPlayerMatch(playerUuid);
        System.out.println("[TestMatchStartCommand] Found match: " + (match != null ? match.getMatchId() : "null"));

        if (match == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>You are not in a match.</color>"));
            return;
        }

        System.out.println("[TestMatchStartCommand] Match state: " + match.getState());
        if (match.getState() != MatchState.WAITING) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Match already started or finished.</color>"));
            return;
        }

        System.out.println("[TestMatchStartCommand] Calling match.start()");
        match.start();
        player.sendMessage(TinyMsg.parse("<color:#2ecc71>Match force-started!</color>"));
    }
}
