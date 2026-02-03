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
import de.ragesith.hyarena2.kit.KitManager;

import javax.annotation.Nonnull;

/**
 * Clears your inventory (removes kit).
 * Usage: /tkitclear
 */
public class TestKitClearCommand extends AbstractPlayerCommand {

    private final KitManager kitManager;

    public TestKitClearCommand(KitManager kitManager) {
        super("tkitclear", "Clear your inventory (remove kit)");
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

        world.execute(() -> {
            kitManager.clearKit(player);
            player.sendMessage(TinyMsg.parse("<color:#2ecc71>Inventory cleared.</color>"));
        });
    }
}
