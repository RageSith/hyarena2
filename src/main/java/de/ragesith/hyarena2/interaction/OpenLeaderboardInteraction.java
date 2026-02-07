package de.ragesith.hyarena2.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.HyArena2;
import fi.sulku.hytale.TinyMsg;

import javax.annotation.Nonnull;

/**
 * Custom interaction for opening the leaderboard UI when using the statue.
 */
public class OpenLeaderboardInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<OpenLeaderboardInteraction> CODEC =
        BuilderCodec.builder(OpenLeaderboardInteraction.class,
            OpenLeaderboardInteraction::new, SimpleInstantInteraction.CODEC).build();

    private static HyArena2 pluginInstance;

    /**
     * Sets the plugin instance for accessing the UI methods.
     */
    public static void setPluginInstance(HyArena2 plugin) {
        pluginInstance = plugin;
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler handler) {
        if (pluginInstance == null) {
            System.err.println("[OpenLeaderboardInteraction] Plugin instance not set!");
            return;
        }

        // Get player from context
        Ref<EntityStore> ref = context.getOwningEntity();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        // TODO: Implement leaderboard page in Phase 7
        player.sendMessage(TinyMsg.parse("<color:#f39c12>Leaderboard coming soon!</color>"));
    }
}
