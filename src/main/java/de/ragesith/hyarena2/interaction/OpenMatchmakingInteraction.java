package de.ragesith.hyarena2.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.HyArena2;
import de.ragesith.hyarena2.ui.page.ArenaMenuPage;
import de.ragesith.hyarena2.ui.page.QueueStatusPage;

import javax.annotation.Nonnull;

/**
 * Custom interaction for opening the HyArena2 matchmaking UI when using the statue.
 */
public class OpenMatchmakingInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<OpenMatchmakingInteraction> CODEC =
        BuilderCodec.builder(OpenMatchmakingInteraction.class,
            OpenMatchmakingInteraction::new, SimpleInstantInteraction.CODEC).build();

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
            System.err.println("[OpenMatchmakingInteraction] Plugin instance not set!");
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

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        // Check if player is in a match
        if (pluginInstance.getMatchManager().isPlayerInMatch(playerRef.getUuid())) {
            player.sendMessage(Message.raw("<color:#e74c3c>You cannot open the arena menu while in a match.</color>"));
            return;
        }

        // Close any existing page first
        pluginInstance.getHudManager().closeActivePage(playerRef.getUuid());

        // Check if player is already in queue - show queue status page
        if (pluginInstance.getQueueManager().isInQueue(playerRef.getUuid())) {
            QueueStatusPage queuePage = new QueueStatusPage(
                playerRef,
                playerRef.getUuid(),
                pluginInstance.getQueueManager(),
                pluginInstance.getMatchmaker(),
                pluginInstance.getMatchManager(),
                pluginInstance.getKitManager(),
                pluginInstance.getHudManager(),
                pluginInstance.getScheduler()
            );
            player.getPageManager().openCustomPage(ref, store, queuePage);
            return;
        }

        // Open the arena selection page
        ArenaMenuPage page = new ArenaMenuPage(
            playerRef,
            playerRef.getUuid(),
            pluginInstance.getMatchManager(),
            pluginInstance.getQueueManager(),
            pluginInstance.getKitManager(),
            pluginInstance.getHudManager(),
            pluginInstance.getScheduler()
        );

        player.getPageManager().openCustomPage(ref, store, page);
    }
}
