package de.ragesith.hyarena2.command.testing;

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
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.kit.KitManager;

import javax.annotation.Nonnull;

/**
 * Applies a kit to yourself for testing.
 * Usage: /tkit <kitId>
 */
public class TestKitApplyCommand extends AbstractPlayerCommand {

    private final KitManager kitManager;

    private final RequiredArg<String> kitIdArg =
        withRequiredArg("kitId", "The kit ID to apply", ArgTypes.STRING);

    public TestKitApplyCommand(KitManager kitManager) {
        super("tkit", "Apply a kit to yourself for testing");
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

        String kitId = kitIdArg.get(context);

        // Check if kit exists
        KitConfig kit = kitManager.getKit(kitId);
        if (kit == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Kit not found: " + kitId + "</color>"));
            player.sendMessage(TinyMsg.parse("<color:#7f8c8d>Use /tkits to see available kits.</color>"));
            return;
        }

        // Check permission
        if (!kitManager.canPlayerUseKit(player, kitId)) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>You don't have permission to use this kit.</color>"));
            return;
        }

        // Apply kit (on world thread)
        world.execute(() -> {
            boolean success = kitManager.applyKit(player, kitId);
            if (success) {
                player.sendMessage(TinyMsg.parse(
                    "<color:#2ecc71>Applied kit: </color><color:#e8c872>" + kit.getDisplayName() + "</color>"
                ));
            } else {
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to apply kit.</color>"));
            }
        });
    }
}
