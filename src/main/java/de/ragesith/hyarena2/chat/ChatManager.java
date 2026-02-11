package de.ragesith.hyarena2.chat;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.ragesith.hyarena2.Permissions;
import de.ragesith.hyarena2.economy.HonorManager;
import fi.sulku.hytale.TinyMsg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat formatting: rank prefixes, colors, and future extensions.
 * Intercepts PlayerChatEvent (async) to apply custom formatting.
 *
 * Name colors are cached on player join (world thread) because
 * player.hasPermission() cannot be called from the async chat thread.
 */
public class ChatManager {

    private final HonorManager honorManager;

    // Cached name color per player (set on join from world thread)
    private final Map<UUID, String> nameColorCache = new ConcurrentHashMap<>();

    private static final String NAME_COLOR_ADMIN = "#e74c3c";
    private static final String NAME_COLOR_PREMIUM = "#2ecc71";
    private static final String NAME_COLOR_DEFAULT = "#e0e8f0";

    // Rank colors matching HonorManager.getRankColor() indices
    private static final String[] RANK_COLORS = {
        "#7f8c8d", // 0: Novice - gray
        "#2ecc71", // 1: Apprentice - green
        "#3498db", // 2: Warrior - blue
        "#9b59b6", // 3: Gladiator - purple
        "#f1c40f", // 4: Champion - gold
    };

    // Rank bracket decorations per tier (escalating flair)
    private static final String[][] RANK_BRACKETS = {
        {"\u00AB",  "\u00BB"},    // Novice - plain
        {"\u00AB",  "\u00BB"},    // Apprentice - plain
        {"\u00AB", "\u00BB"},    // Warrior - angled
        {"\u00AB", "\u00BB"},  // Gladiator - guillemets « »
        {"\u00AB", "\u00BB"},  // Champion - stars ✦ ✦
    };

    public ChatManager(HonorManager honorManager) {
        this.honorManager = honorManager;
        System.out.println("[ChatManager] Chat formatter ready");
    }

    /**
     * Caches a player's name color based on their permissions.
     * Must be called from the world thread (e.g. onPlayerReady).
     */
    public void cachePlayerNameColor(UUID uuid, Player player) {
        String color;
        if (player.hasPermission(Permissions.ADMIN)) {
            color = NAME_COLOR_ADMIN;
        } else if (player.hasPermission(Permissions.PREMIUM)) {
            color = NAME_COLOR_PREMIUM;
        } else {
            color = NAME_COLOR_DEFAULT;
        }
        nameColorCache.put(uuid, color);
    }

    /**
     * Removes the cached name color for a disconnecting player.
     */
    public void removePlayer(UUID uuid) {
        nameColorCache.remove(uuid);
    }

    /**
     * Refreshes a player's cached name color by UUID.
     * Resolves the Player from Universe — call from world thread or scheduled task.
     */
    public void refreshPlayer(UUID uuid) {
        try {
            PlayerRef ref = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(uuid);
            if (ref == null) return;
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> entityRef = ref.getReference();
            if (entityRef == null) return;
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = entityRef.getStore();
            Player player = store.getComponent(entityRef, Player.getComponentType());
            if (player != null) {
                cachePlayerNameColor(uuid, player);
            }
        } catch (Exception e) {
            System.err.println("[ChatManager] Failed to refresh name color for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Handles a chat event by applying rank prefix formatting.
     * Called from async event registration in HyArena2.
     */
    public void onChat(PlayerChatEvent event) {
        try {
            PlayerRef sender = event.getSender();
            if (sender == null) return;

            UUID uuid = sender.getUuid();
            String username = sender.getUsername();

            // Get rank info (HonorManager data is thread-safe via PlayerDataManager)
            String rankName = honorManager.getRankDisplayName(uuid);
            String rankColor = honorManager.getRankColor(uuid);
            int rankIndex = getRankIndex(rankColor);

            String open = RANK_BRACKETS[rankIndex][0];
            String close = RANK_BRACKETS[rankIndex][1];

            // Look up cached name color (set on join from world thread)
            final String nameColor = nameColorCache.getOrDefault(uuid, NAME_COLOR_DEFAULT);

            // Build formatted message:
            // <rankColor><bold>«Gladiator»</bold></rankColor> <nameColor>PlayerName</nameColor><gray> : message</gray>
            event.setFormatter((playerRef, message) ->
                TinyMsg.parse(
                    "<color:" + rankColor + "><bold>" + open + rankName + close + "</bold></color> "
                    + "<color:" + nameColor + ">" + username + "</color>"
                    + "<color:#7f8c8d> : </color>"
                    + "<color:#c8d6e0>" + message + "</color>"
                )
            );
        } catch (Exception e) {
            System.err.println("[ChatManager] Error formatting chat: " + e.getMessage());
        }
    }

    /**
     * Maps a rank color hex to an index for bracket lookup.
     */
    private int getRankIndex(String color) {
        for (int i = 0; i < RANK_COLORS.length; i++) {
            if (RANK_COLORS[i].equals(color)) return i;
        }
        return 0;
    }
}
