package de.ragesith.hyarena2.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.hub.HubManager;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;

/**
 * Toggle command to teleport to/from the arena_test build world.
 * Usage: /hybuild
 * Permission: hyarena.admin
 */
public class BuildCommand extends AbstractPlayerCommand {

    public static final String BUILD_WORLD_NAME = "arena_test";
    private static final Position BUILD_SPAWN = new Position(0, 80, 0);

    private final HubManager hubManager;

    public BuildCommand(HubManager hubManager) {
        super("hybuild", "Teleport to/from the build world");
        this.requirePermission(Permissions.ADMIN);
        this.hubManager = hubManager;
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

        if (player.getWorld().getName().equals(BUILD_WORLD_NAME)) {
            // Already in build world — teleport back to hub
            hubManager.teleportToHub(player, () -> {
                player.sendMessage(TinyMsg.parse("<color:#2ecc71>Returned to hub.</color>"));
            });
        } else {
            // Teleport to build world
            World buildWorld = Universe.get().getWorld(BUILD_WORLD_NAME);
            if (buildWorld == null) {
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>World '" + BUILD_WORLD_NAME + "' not found.</color>"));
                return;
            }
            hubManager.teleportPlayerToWorld(player, BUILD_SPAWN, buildWorld, () -> {
                player.sendMessage(TinyMsg.parse("<color:#2ecc71>Teleported to " + BUILD_WORLD_NAME + ". Use /hybuild to return.</color>"));
            });
        }
    }
}
