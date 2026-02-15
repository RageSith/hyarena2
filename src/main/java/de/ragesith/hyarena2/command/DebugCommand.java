package de.ragesith.hyarena2.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.debug.DebugLayer;
import de.ragesith.hyarena2.debug.DebugViewManager;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;
import java.util.EnumSet;

/**
 * Debug visualization command.
 * Usage: /hydebug [barriers|zones|spawns|color|rainbow|mode] [value]
 */
public class DebugCommand extends AbstractPlayerCommand {

    private final DebugViewManager debugViewManager;

    private final OptionalArg<String> subcommandArg =
        withOptionalArg("subcommand", "barriers, zones, spawns, color, rainbow, mode", ArgTypes.STRING);
    private final OptionalArg<String> valueArg =
        withOptionalArg("value", "Color hex or preset name", ArgTypes.STRING);

    public DebugCommand(DebugViewManager debugViewManager) {
        super("hydebug", "Toggle debug visualization overlays");
        requirePermission(Permissions.DEBUG);
        this.debugViewManager = debugViewManager;
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

        String subcommand = subcommandArg.get(context);

        if (subcommand == null || subcommand.isEmpty()) {
            // Toggle all layers
            boolean on = debugViewManager.toggleAll(playerRef.getUuid());
            player.sendMessage(TinyMsg.parse(on
                ? "<color:#2ecc71>Debug: all layers ON</color>"
                : "<color:#e74c3c>Debug: all layers OFF</color>"));
            sendStatus(player, playerRef.getUuid());
            return;
        }

        switch (subcommand.toLowerCase()) {
            case "barriers": {
                boolean on = debugViewManager.toggleLayer(playerRef.getUuid(), DebugLayer.BARRIERS);
                player.sendMessage(TinyMsg.parse(on
                    ? "<color:#2ecc71>Barriers layer ON</color>"
                    : "<color:#e74c3c>Barriers layer OFF</color>"));
                sendStatus(player, playerRef.getUuid());
                break;
            }
            case "zones": {
                boolean on = debugViewManager.toggleLayer(playerRef.getUuid(), DebugLayer.ZONES);
                player.sendMessage(TinyMsg.parse(on
                    ? "<color:#2ecc71>Zones layer ON</color>"
                    : "<color:#e74c3c>Zones layer OFF</color>"));
                sendStatus(player, playerRef.getUuid());
                break;
            }
            case "spawns": {
                boolean on = debugViewManager.toggleLayer(playerRef.getUuid(), DebugLayer.SPAWNS);
                player.sendMessage(TinyMsg.parse(on
                    ? "<color:#2ecc71>Spawns layer ON</color>"
                    : "<color:#e74c3c>Spawns layer OFF</color>"));
                sendStatus(player, playerRef.getUuid());
                break;
            }
            case "color": {
                String value = valueArg.get(context);
                if (value == null || value.isEmpty()) {
                    player.sendMessage(TinyMsg.parse(
                        "<color:#e74c3c>Usage: /hydebug color <hex|preset></color>\n" +
                        "<color:#b7cedd>Presets: red, green, blue, yellow, cyan, magenta, orange, white</color>\n" +
                        "<color:#b7cedd>Hex: #FF0000</color>"));
                    return;
                }
                com.hypixel.hytale.protocol.Vector3f color = DebugViewManager.resolveColor(value);
                if (color == null) {
                    player.sendMessage(TinyMsg.parse("<color:#e74c3c>Invalid color: " + value + "</color>"));
                    return;
                }
                debugViewManager.setBarrierColor(playerRef.getUuid(), color);
                player.sendMessage(TinyMsg.parse("<color:#2ecc71>Barrier color set to " + value + "</color>"));
                break;
            }
            case "rainbow": {
                boolean on = debugViewManager.toggleRainbow(playerRef.getUuid());
                player.sendMessage(TinyMsg.parse(on
                    ? "<color:#2ecc71>Rainbow mode ON</color>"
                    : "<color:#e74c3c>Rainbow mode OFF</color>"));
                break;
            }
            default: {
                player.sendMessage(TinyMsg.parse(
                    "<color:#e74c3c>Unknown subcommand: " + subcommand + "</color>\n" +
                    "<color:#b7cedd>Usage: /hydebug [barriers|zones|spawns|color|rainbow]</color>"));
                break;
            }
        }
    }

    private void sendStatus(Player player, java.util.UUID playerId) {
        EnumSet<DebugLayer> layers = debugViewManager.getEnabledLayers(playerId);
        String barriers = layers.contains(DebugLayer.BARRIERS) ? "<color:#2ecc71>ON</color>" : "<color:#e74c3c>OFF</color>";
        String zones = layers.contains(DebugLayer.ZONES) ? "<color:#2ecc71>ON</color>" : "<color:#e74c3c>OFF</color>";
        String spawns = layers.contains(DebugLayer.SPAWNS) ? "<color:#2ecc71>ON</color>" : "<color:#e74c3c>OFF</color>";

        player.sendMessage(TinyMsg.parse(
            "<color:#b7cedd>Barriers: </color>" + barriers +
            " <color:#b7cedd>| Zones: </color>" + zones +
            " <color:#b7cedd>| Spawns: </color>" + spawns));
    }
}
