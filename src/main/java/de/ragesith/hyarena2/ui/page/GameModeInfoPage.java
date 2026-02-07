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
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Page that displays a game mode's description.
 * Uses appendInline to inject the mode's UI markup into a scrollable container.
 */
public class GameModeInfoPage extends InteractiveCustomUIPage<GameModeInfoPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final GameMode gameMode;
    private final HudManager hudManager;
    private final Runnable onBack;

    public GameModeInfoPage(PlayerRef playerRef, UUID playerUuid, GameMode gameMode,
                            HudManager hudManager, Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.gameMode = gameMode;
        this.hudManager = hudManager;
        this.onBack = onBack;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/GameModeInfoPage.ui");

        // Set title
        cmd.set("#InfoTitle.Text", gameMode.getDisplayName());

        // Inject game mode description UI into scrollable container
        cmd.appendInline("#DescriptionContent", gameMode.getDescription());

        // Event bindings
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackButton",
            EventData.of("Action", "back"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        // Register with HudManager for proper cleanup
        hudManager.registerPage(playerUuid, this);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null || data.action == null) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());

            switch (data.action) {
                case "back":
                    shutdown();
                    if (onBack != null) {
                        onBack.run();
                    }
                    break;

                case "close":
                    shutdown();
                    if (player != null) {
                        player.getPageManager().setPage(ref, store, Page.None);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[GameModeInfoPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
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
