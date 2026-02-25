package de.ragesith.hyarena2.ui.page;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.queue.QueueManager;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Category selection page: players choose between PVP and MiniGames
 * before entering the arena browser.
 */
public class ArenaCategoryPage extends InteractiveCustomUIPage<ArenaCategoryPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final MatchManager matchManager;
    private final QueueManager queueManager;
    private final KitManager kitManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;

    public ArenaCategoryPage(PlayerRef playerRef, UUID playerUuid,
                             MatchManager matchManager, QueueManager queueManager,
                             KitManager kitManager, HudManager hudManager,
                             ScheduledExecutorService scheduler) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.matchManager = matchManager;
        this.queueManager = queueManager;
        this.kitManager = kitManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ArenaCategoryPage.ui");

        // Bind PVP card
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PvpCard",
            EventData.of("Action", "pvp"),
            false
        );

        // Bind MiniGames card
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#MinigameCard",
            EventData.of("Action", "minigame"),
            false
        );

        // Bind close button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        hudManager.registerPage(playerUuid, this);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        if (data == null || data.action == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        switch (data.action) {
            case "close":
                player.getPageManager().setPage(ref, store, Page.None);
                break;

            case "pvp":
                openArenaMenu(ref, store, player, GameMode.GameModeCategory.PVP);
                break;

            case "minigame":
                openArenaMenu(ref, store, player, GameMode.GameModeCategory.MINIGAME);
                break;
        }
    }

    private void openArenaMenu(Ref<EntityStore> ref, Store<EntityStore> store,
                               Player player, GameMode.GameModeCategory category) {
        ArenaMenuPage menuPage = new ArenaMenuPage(
            playerRef, playerUuid, matchManager, queueManager,
            kitManager, hudManager, scheduler, category
        );
        player.getPageManager().openCustomPage(ref, store, menuPage);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        shutdown();
    }

    @Override
    public void shutdown() {
        hudManager.unregisterPage(playerUuid, this);
    }

    @Override
    public void close() {
        shutdown();
    }

    public static class PageEventData {
        public String action;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .build();
    }
}
