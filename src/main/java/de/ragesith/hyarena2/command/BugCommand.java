package de.ragesith.hyarena2.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.HyArena2;
import de.ragesith.hyarena2.ui.page.BugReportPage;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command to open the bug report form.
 * Usage: /bug
 *
 * No permission required - available to all players.
 * 5-minute cooldown per player.
 */
public class BugCommand extends AbstractPlayerCommand {

    private static final long COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

    private final HyArena2 plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public BugCommand(HyArena2 plugin) {
        super("bug", "Report a bug");
        this.requirePermission("hyarena.use");
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

        UUID playerId = playerRef.getUuid();

        // Check cooldown
        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(playerId);
        if (lastUsed != null) {
            long remaining = (COOLDOWN_MS - (now - lastUsed)) / 1000;
            if (remaining > 0) {
                long mins = remaining / 60;
                long secs = remaining % 60;
                String time = mins > 0 ? mins + "m " + secs + "s" : secs + "s";
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>Please wait " + time + " before submitting another bug report.</color>"));
                return;
            }
        }

        // Check API availability
        if (plugin.getApiClient() == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Bug reporting is not available (API not configured).</color>"));
            return;
        }

        // Set cooldown
        cooldowns.put(playerId, now);

        // Close any existing page first
        plugin.getHudManager().closeActivePage(playerId);

        // Open bug report page
        BugReportPage page = new BugReportPage(
            playerRef,
            playerId,
            player.getDisplayName(),
            plugin.getApiClient(),
            plugin.getHudManager(),
            world
        );

        player.getPageManager().openCustomPage(ref, store, page);
    }
}
