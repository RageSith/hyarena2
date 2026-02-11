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
import de.ragesith.hyarena2.economy.EconomyManager;
import de.ragesith.hyarena2.economy.HonorManager;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test command for HyML pages.
 * Usage: /thyml <filename>
 */
public class TestHyMLCommand extends AbstractPlayerCommand {
    private final HudManager hudManager;
    private final EconomyManager economyManager;
    private final HonorManager honorManager;

    private final RequiredArg<String> fileArg =
        withRequiredArg("file", "HyML file name (e.g. test.hyml)", ArgTypes.STRING);

    public TestHyMLCommand(HudManager hudManager, EconomyManager economyManager, HonorManager honorManager) {
        super("thyml", "Open a HyML page");
        requirePermission(Permissions.DEBUG);
        this.hudManager = hudManager;
        this.economyManager = economyManager;
        this.honorManager = honorManager;
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

        String file = fileArg.get(context);

        // Build vars map with player info
        UUID uuid = playerRef.getUuid();
        Map<String, String> vars = new HashMap<>();
        vars.put("player_name", player.getDisplayName());
        vars.put("ap", String.valueOf(economyManager.getArenaPoints(uuid)));
        vars.put("rank", honorManager.getRankDisplayName(uuid));

        player.sendMessage(TinyMsg.parse("<color:#2ecc71>Opening HyML page: " + file + "</color>"));
        hudManager.showHyMLPage(playerRef.getUuid(), file, vars);
    }
}
