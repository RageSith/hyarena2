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
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.kit.KitAccessInfo;
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.queue.QueueManager;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Arena detail page showing arena info, kit selection, and join queue button.
 */
public class ArenaDetailPage extends InteractiveCustomUIPage<ArenaDetailPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final Arena arena;
    private final MatchManager matchManager;
    private final QueueManager queueManager;
    private final KitManager kitManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final BiConsumer<Ref<EntityStore>, Store<EntityStore>> onBack;

    // Current state
    private String selectedKitId;
    private List<KitAccessInfo> availableKits;

    // Auto-refresh
    private ScheduledFuture<?> refreshTask;
    private volatile boolean active = true;

    // Previous refresh values for diff-based updates (avoid focus steal)
    private int lastQueueCount = -1;
    private boolean lastInUse;

    public ArenaDetailPage(PlayerRef playerRef, UUID playerUuid, Arena arena,
                           MatchManager matchManager, QueueManager queueManager,
                           KitManager kitManager, HudManager hudManager,
                           ScheduledExecutorService scheduler,
                           BiConsumer<Ref<EntityStore>, Store<EntityStore>> onBack) {
        this(playerRef, playerUuid, arena, matchManager, queueManager,
             kitManager, hudManager, scheduler, onBack, null);
    }

    public ArenaDetailPage(PlayerRef playerRef, UUID playerUuid, Arena arena,
                           MatchManager matchManager, QueueManager queueManager,
                           KitManager kitManager, HudManager hudManager,
                           ScheduledExecutorService scheduler,
                           BiConsumer<Ref<EntityStore>, Store<EntityStore>> onBack,
                           String selectedKitId) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.arena = arena;
        this.matchManager = matchManager;
        this.queueManager = queueManager;
        this.kitManager = kitManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;
        this.selectedKitId = selectedKitId;
        this.availableKits = new ArrayList<>();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ArenaDetailPage.ui");

        Player player = store.getComponent(ref, Player.getComponentType());

        // Arena title
        cmd.set("#ArenaTitle.Text", arena.getDisplayName());

        // Arena description
        String description = String.format("%s | %d-%d Players",
            arena.getGameMode(),
            arena.getMinPlayers(),
            arena.getMaxPlayers());
        cmd.set("#ArenaDescription.Text", description);

        // Info section
        cmd.set("#GameModeValue.Text", arena.getGameMode());
        cmd.set("#PlayersValue.Text", arena.getMinPlayers() + "-" + arena.getMaxPlayers());

        // Queue status
        updateQueueInfo(cmd);

        // Kit selection label
        if (player != null) {
            availableKits = kitManager.getKitsForArena(player, arena.getId());

            // Resolve kit: explicit selection > saved preference > first unlocked
            if (selectedKitId == null) {
                String preferred = kitManager.getPreferredKit(playerUuid, arena.getId());
                if (preferred != null && kitManager.kitExists(preferred)) {
                    selectedKitId = preferred;
                }
            }
            if (selectedKitId == null && !availableKits.isEmpty()) {
                // Sort same as KitSelectionPage to pick the first alphabetically
                List<KitAccessInfo> sorted = new ArrayList<>(availableKits);
                sorted.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
                for (KitAccessInfo info : sorted) {
                    if (info.isUnlocked()) {
                        selectedKitId = info.getKitId();
                        break;
                    }
                }
            }
        }

        if (selectedKitId != null) {
            KitConfig kit = kitManager.getKit(selectedKitId);
            if (kit != null) {
                cmd.set("#SelectedKitLabel.Text", "Kit: " + kit.getDisplayName());
            } else {
                cmd.set("#SelectedKitLabel.Text", "Kit: None selected");
            }
        } else {
            cmd.set("#SelectedKitLabel.Text", availableKits.isEmpty() ? "No kits available" : "Kit: None selected");
        }

        // Event bindings
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackButton",
            EventData.of("Action", "back"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#JoinQueueButton",
            EventData.of("Action", "join"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SelectKitButton",
            EventData.of("Action", "selectkit"),
            false
        );

        // Register with HudManager for proper cleanup
        hudManager.registerPage(playerUuid, this);

        // Start auto-refresh
        startAutoRefresh();
    }

    /**
     * Updates queue status info.
     */
    private void updateQueueInfo(UICommandBuilder cmd) {
        int queueCount = queueManager.getQueueSize(arena.getId());
        cmd.set("#QueueValue.Text", String.valueOf(queueCount));

        // Color based on queue size
        if (queueCount >= arena.getMinPlayers()) {
            cmd.set("#QueueValue.Style.TextColor", "#2ecc71"); // Green - enough for match
        } else if (queueCount > 0) {
            cmd.set("#QueueValue.Style.TextColor", "#f39c12"); // Orange - some players
        } else {
            cmd.set("#QueueValue.Style.TextColor", "#3498db"); // Blue - empty
        }

        // Arena status
        boolean inUse = matchManager.isArenaInUse(arena.getId());
        if (inUse) {
            cmd.set("#StatusValue.Text", "In Use");
            cmd.set("#StatusValue.Style.TextColor", "#e74c3c");
        } else {
            cmd.set("#StatusValue.Text", "Available");
            cmd.set("#StatusValue.Style.TextColor", "#2ecc71");
        }
    }

    /**
     * Starts auto-refresh for queue counts.
     * Only sends an update when values actually change to avoid stealing UI focus.
     */
    private void startAutoRefresh() {
        if (refreshTask != null || scheduler == null) {
            return;
        }

        // Initialize with current values
        lastQueueCount = queueManager.getQueueSize(arena.getId());
        lastInUse = matchManager.isArenaInUse(arena.getId());

        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            if (!active) {
                return;
            }

            try {
                int queueCount = queueManager.getQueueSize(arena.getId());
                boolean inUse = matchManager.isArenaInUse(arena.getId());

                if (queueCount == lastQueueCount && inUse == lastInUse) {
                    return;
                }

                UICommandBuilder cmd = new UICommandBuilder();

                if (queueCount != lastQueueCount) {
                    lastQueueCount = queueCount;
                    cmd.set("#QueueValue.Text", String.valueOf(queueCount));

                    if (queueCount >= arena.getMinPlayers()) {
                        cmd.set("#QueueValue.Style.TextColor", "#2ecc71");
                    } else if (queueCount > 0) {
                        cmd.set("#QueueValue.Style.TextColor", "#f39c12");
                    } else {
                        cmd.set("#QueueValue.Style.TextColor", "#3498db");
                    }
                }

                if (inUse != lastInUse) {
                    lastInUse = inUse;
                    if (inUse) {
                        cmd.set("#StatusValue.Text", "In Use");
                        cmd.set("#StatusValue.Style.TextColor", "#e74c3c");
                    } else {
                        cmd.set("#StatusValue.Text", "Available");
                        cmd.set("#StatusValue.Style.TextColor", "#2ecc71");
                    }
                }

                safeSendUpdate(cmd);
            } catch (Exception e) {
                // Page might be closed
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Safely sends a UI update, checking active flag first.
     * Prevents "CustomUI command not found" errors when page is closed.
     */
    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) {
            return;
        }
        try {
            sendUpdate(cmd, false);
        } catch (Exception e) {
            // Page might be closed, stop further updates
            active = false;
        }
    }

    /**
     * Stops auto-refresh.
     */
    private void stopAutoRefresh() {
        active = false;
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());

            // Handle actions
            if (data.action != null) {
                switch (data.action) {
                    case "close":
                        stopAutoRefresh();
                        if (player != null) {
                            player.getPageManager().setPage(ref, store, Page.None);
                        }
                        break;

                    case "back":
                        stopAutoRefresh();
                        if (onBack != null) {
                            onBack.accept(ref, store);
                        }
                        break;

                    case "join":
                        handleJoinQueue(ref, store, player);
                        break;

                    case "selectkit":
                        handleOpenKitSelection(ref, store, player);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ArenaDetailPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens the KitSelectionPage.
     */
    private void handleOpenKitSelection(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        if (player == null || availableKits.isEmpty()) {
            return;
        }

        stopAutoRefresh();

        KitSelectionPage kitPage = new KitSelectionPage(
            playerRef, playerUuid, availableKits, selectedKitId, hudManager,
            // onKitSelected callback
            (kitId) -> {
                selectedKitId = kitId;
                kitManager.setPreferredKit(playerUuid, arena.getId(), kitId);
                // Reopen this page with the selected kit
                ArenaDetailPage newPage = new ArenaDetailPage(
                    playerRef, playerUuid, arena,
                    matchManager, queueManager, kitManager, hudManager, scheduler,
                    onBack, selectedKitId
                );
                Ref<EntityStore> pRef = playerRef.getReference();
                if (pRef != null) {
                    Store<EntityStore> pStore = pRef.getStore();
                    Player p = pStore.getComponent(pRef, Player.getComponentType());
                    if (p != null) {
                        p.getPageManager().openCustomPage(pRef, pStore, newPage);
                    }
                }
            },
            // onBack callback
            () -> {
                // Return to this ArenaDetailPage preserving selected kit
                ArenaDetailPage newPage = new ArenaDetailPage(
                    playerRef, playerUuid, arena,
                    matchManager, queueManager, kitManager, hudManager, scheduler,
                    onBack, selectedKitId
                );
                Ref<EntityStore> pRef = playerRef.getReference();
                if (pRef != null) {
                    Store<EntityStore> pStore = pRef.getStore();
                    Player p = pStore.getComponent(pRef, Player.getComponentType());
                    if (p != null) {
                        p.getPageManager().openCustomPage(pRef, pStore, newPage);
                    }
                }
            }
        );

        player.getPageManager().openCustomPage(ref, store, kitPage);
    }

    /**
     * Handles join queue action.
     */
    private void handleJoinQueue(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        if (player == null) {
            return;
        }

        // Validate kit selection if kits are available
        if (!availableKits.isEmpty() && selectedKitId != null) {
            // Check if player can use the selected kit
            if (!kitManager.canPlayerUseKit(player, selectedKitId)) {
                showError("You don't have access to this kit");
                return;
            }
        }

        // Try to join queue
        QueueManager.JoinResult result = queueManager.joinQueue(
            playerUuid,
            player.getDisplayName(),
            arena.getId(),
            selectedKitId
        );

        switch (result) {
            case SUCCESS:
                // Close page on success
                stopAutoRefresh();
                player.getPageManager().setPage(ref, store, Page.None);
                break;

            case ALREADY_IN_QUEUE:
                showError("You are already in a queue");
                break;

            case IN_MATCH:
                showError("You are already in a match");
                break;

            case ON_COOLDOWN:
                int cooldown = queueManager.getRemainingCooldownSeconds(playerUuid);
                showError("Please wait " + cooldown + "s before rejoining");
                break;

            case NOT_IN_HUB:
                showError("You must be in the hub to join a queue");
                break;

            case INVALID_KIT:
                showError("Invalid kit selection");
                break;
        }
    }

    /**
     * Shows an error message on the page.
     */
    private void showError(String message) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#StatusMessage.Text", message);
        cmd.set("#StatusMessage.Visible", true);
        cmd.set("#StatusMessage.Style.TextColor", "#e74c3c");
        safeSendUpdate(cmd);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        shutdown();
    }

    @Override
    public void shutdown() {
        stopAutoRefresh();
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
