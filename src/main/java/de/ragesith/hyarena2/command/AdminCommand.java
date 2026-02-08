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
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.ui.page.admin.AdminPanelPage;

import javax.annotation.Nonnull;

/**
 * Command to open the admin panel UI.
 * Usage: /admin
 */
public class AdminCommand extends AbstractPlayerCommand {

    private final HyArena2 plugin;

    public AdminCommand(HyArena2 plugin) {
        super("hyadmin", "Opens the admin panel");
        this.requirePermission(Permissions.ADMIN);
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

        // Close any existing page first
        plugin.getHudManager().closeActivePage(playerRef.getUuid());

        // Open the admin panel page
        AdminPanelPage page = new AdminPanelPage(
            playerRef,
            playerRef.getUuid(),
            plugin.getMatchManager(),
            plugin.getKitManager(),
            plugin.getHubManager(),
            plugin.getConfigManager(),
            plugin.getHudManager(),
            plugin.getScheduler()
        );

        player.getPageManager().openCustomPage(ref, store, page);
    }
}
