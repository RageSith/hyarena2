package de.ragesith.hyarena2.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.HyArena2;
import de.ragesith.hyarena2.Permissions;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command to generate an account link code.
 * Usage: /link
 *
 * Calls the web API to generate a 6-char code that the player
 * can enter on the website to link their game account.
 */
public class LinkCommand extends AbstractPlayerCommand {

    private static final long COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

    private final HyArena2 plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public LinkCommand(HyArena2 plugin) {
        super("link", "Link your game account to the website");
        this.requirePermission(Permissions.LINK);
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
        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(playerId);
        if (lastUsed != null) {
            long remaining = (COOLDOWN_MS - (now - lastUsed)) / 1000;
            if (remaining > 0) {
                long mins = remaining / 60;
                long secs = remaining % 60;
                String time = mins > 0 ? mins + "m " + secs + "s" : secs + "s";
                player.sendMessage(TinyMsg.parse("<color:#e74c3c>Please wait " + time + " before requesting a new code.</color>"));
                return;
            }
        }

        if (plugin.getApiClient() == null) {
            player.sendMessage(TinyMsg.parse("<color:#e74c3c>Account linking is not available (API not configured).</color>"));
            return;
        }

        cooldowns.put(playerId, now);

        String uuid = playerId.toString();
        String username = player.getDisplayName();

        // Build JSON payload
        String json = "{\"uuid\":\"" + uuid + "\",\"username\":\"" + username.replace("\"", "\\\"") + "\"}";

        player.sendMessage(TinyMsg.parse("<color:#f39c12>Generating link code...</color>"));

        plugin.getApiClient().postAsync("/api/link/generate", json).thenAccept(response -> {
            // Dispatch back to world thread for player messaging
            world.execute(() -> {
                if (response == null) {
                    player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to connect to the web API. Please try again later.</color>"));
                    return;
                }

                int status = response.statusCode();
                if (status != 200) {
                    player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to generate link code (HTTP " + status + ").</color>"));
                    return;
                }

                try {
                    JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!body.get("success").getAsBoolean()) {
                        String error = body.getAsJsonObject("error").get("message").getAsString();
                        player.sendMessage(TinyMsg.parse("<color:#e74c3c>Error: " + error + "</color>"));
                        return;
                    }

                    String code = body.getAsJsonObject("data").get("code").getAsString();
                    String linkUrl = plugin.getApiClient().getBaseUrl() + "/link?code=" + code;

                    player.sendMessage(TinyMsg.parse("<color:#2ecc71>Your link code: <b>" + code + "</b></color>"));
                    player.sendMessage(TinyMsg.parse("<link:" + linkUrl + "><gradient:#3498db:#9b59b6><u>Click here to link your account</u></gradient></link>"));
                    player.sendMessage(TinyMsg.parse("<color:#95a5a6>The code expires in 10 minutes.</color>"));
                } catch (Exception e) {
                    player.sendMessage(TinyMsg.parse("<color:#e74c3c>Failed to parse API response.</color>"));
                    System.err.println("[HyArena2] /link response parse error: " + e.getMessage());
                }
            });
        });
    }
}
