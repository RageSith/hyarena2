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
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.kit.KitManager;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Lists all available kits.
 * Usage: /tkits
 */
public class TestKitListCommand extends AbstractPlayerCommand {

    private final KitManager kitManager;

    public TestKitListCommand(KitManager kitManager) {
        super("tkits", "List all available kits");
        requirePermission(Permissions.DEBUG);
        this.kitManager = kitManager;
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

        List<KitConfig> kits = kitManager.getAllKits();

        if (kits.isEmpty()) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>No kits loaded. Check config/kits/ directory.</color>"));
            return;
        }

        player.sendMessage(TinyMsg.parse("<color:#e8c872>Available Kits (" + kits.size() + "):</color>"));
        for (KitConfig kit : kits) {
            String permission = kit.getPermission();
            boolean hasPermission = permission == null || player.hasPermission(permission);
            String statusColor = hasPermission ? "#2ecc71" : "#e74c3c";
            String status = hasPermission ? "" : " <color:#7f8c8d>(locked)</color>";

            player.sendMessage(TinyMsg.parse(
                "<color:" + statusColor + ">  " + kit.getId() + "</color>" +
                " <color:#3498db>- " + kit.getDisplayName() + "</color>" + status
            ));
            player.sendMessage(TinyMsg.parse(
                "<color:#7f8c8d>    " + kit.getDescription() + "</color>"
            ));
        }
    }
}
