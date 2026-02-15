package de.ragesith.hyarena2.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.utils.PermissionHelper;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Toggle command to switch between admin and adminplayer groups.
 * Usage: /hyadminplay
 */
public class AdminPlayCommand extends AbstractPlayerCommand {

    private final ScheduledExecutorService scheduler;

    public AdminPlayCommand(ScheduledExecutorService scheduler) {
        super("hyadminplay", "Toggle between admin and adminplayer groups");
        this.requirePermission(Permissions.SWITCH_ROLE);
        this.scheduler = scheduler;
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

        java.util.UUID uuid = playerRef.getUuid();

        if (player.hasPermission(Permissions.MODE_ADMIN)) {
            PermissionHelper.addToGroup(uuid, "adminplayer");
            scheduler.schedule(() -> PermissionHelper.removeFromGroup(uuid, "admin"), 50, TimeUnit.MILLISECONDS);
            player.sendMessage(TinyMsg.parse("<color:#2ecc71>Switched to <bold>adminplayer</bold></color>"));
        } else if (player.hasPermission(Permissions.MODE_ADMINPLAYER)) {
            PermissionHelper.addToGroup(uuid, "admin");
            scheduler.schedule(() -> PermissionHelper.removeFromGroup(uuid, "adminplayer"), 50, TimeUnit.MILLISECONDS);
            player.sendMessage(TinyMsg.parse("<color:#2ecc71>Switched to <bold>admin</bold></color>"));
        } else {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>You are not in a recognized group (admin or adminplayer)</color>"));
        }
    }
}
