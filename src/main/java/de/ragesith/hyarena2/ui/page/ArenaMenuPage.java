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
import de.ragesith.hyarena2.arena.Match;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.queue.QueueManager;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Arena selection page with two-column layout:
 * - Left: scrollable list of game mode buttons (dynamic templates)
 * - Right: arena cards filtered by selected game mode
 */
public class ArenaMenuPage extends InteractiveCustomUIPage<ArenaMenuPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final MatchManager matchManager;
    private final QueueManager queueManager;
    private final KitManager kitManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;

    // Stored for page transitions
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;

    // Game mode entries: index 0 = "All", 1+ = specific game modes
    private List<GameModeEntry> gameModeEntries;

    // Current state
    private int selectedModeIndex;
    private List<Arena> filteredArenas;

    // Auto-refresh task
    private ScheduledFuture<?> refreshTask;
    private volatile boolean active = true;

    // Previous refresh values for diff-based updates
    private boolean[] lastInUse;
    private int[] lastQueueCount;
    private int[] lastInGameCount;

    public ArenaMenuPage(PlayerRef playerRef, UUID playerUuid,
                         MatchManager matchManager, QueueManager queueManager,
                         KitManager kitManager, HudManager hudManager,
                         ScheduledExecutorService scheduler) {
        this(playerRef, playerUuid, matchManager, queueManager, kitManager, hudManager, scheduler, 0);
    }

    public ArenaMenuPage(PlayerRef playerRef, UUID playerUuid,
                         MatchManager matchManager, QueueManager queueManager,
                         KitManager kitManager, HudManager hudManager,
                         ScheduledExecutorService scheduler, int selectedModeIndex) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.matchManager = matchManager;
        this.queueManager = queueManager;
        this.kitManager = kitManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.selectedModeIndex = selectedModeIndex;

        // Build game mode entries: "All" + each registered game mode
        this.gameModeEntries = new ArrayList<>();
        this.gameModeEntries.add(new GameModeEntry("all", "All"));
        for (GameMode gm : matchManager.getGameModes()) {
            this.gameModeEntries.add(new GameModeEntry(gm.getId(), gm.getDisplayName()));
        }

        // Clamp index
        if (this.selectedModeIndex < 0 || this.selectedModeIndex >= gameModeEntries.size()) {
            this.selectedModeIndex = 0;
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        this.storedRef = ref;
        this.storedStore = store;

        cmd.append("Pages/ArenaMenuPage.ui");

        // Populate game mode list (left column)
        for (int i = 0; i < gameModeEntries.size(); i++) {
            GameModeEntry entry = gameModeEntries.get(i);

            cmd.append("#GameModeList", "Pages/GameModeButton.ui");
            String row = "#GameModeList[" + i + "]";

            cmd.set(row + " #GMBtnName.Text", entry.displayName);

            // Highlight selected mode with gold text
            if (i == selectedModeIndex) {
                cmd.set(row + " #GMBtnName.Style.TextColor", "#e8c872");
            }

            // Bind click event
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row,
                EventData.of("Action", "mode").append("Index", String.valueOf(i)),
                false
            );
        }

        // Filter arenas by selected game mode
        filteredArenas = filterArenas();

        // Update header
        if (selectedModeIndex == 0) {
            cmd.set("#SelectedModeName.Text", "All Arenas");
        } else {
            cmd.set("#SelectedModeName.Text", gameModeEntries.get(selectedModeIndex).displayName);
        }

        // Show/hide no arenas notice
        cmd.set("#NoArenasNotice.Visible", filteredArenas.isEmpty());

        // Populate arena cards (right column)
        for (int i = 0; i < filteredArenas.size(); i++) {
            Arena arena = filteredArenas.get(i);

            cmd.append("#ArenaList", "Pages/ArenaCard.ui");
            String row = "#ArenaList[" + i + "]";

            // Arena name
            cmd.set(row + " #ArenaName.Text", arena.getDisplayName());

            // Player count
            cmd.set(row + " #PlayerCount.Text", MatchManager.formatPlayerCount(arena));

            // Status tag
            boolean inUse = matchManager.isArenaInUse(arena.getId());
            if (inUse) {
                cmd.set(row + " #StatusTag.Text", "IN USE");
                cmd.set(row + " #StatusTag.Style.TextColor", "#e74c3c");
            } else {
                cmd.set(row + " #StatusTag.Text", "AVAILABLE");
                cmd.set(row + " #StatusTag.Style.TextColor", "#2ecc71");
            }

            // Queue count
            int queueCount = queueManager.getQueueSize(arena.getId());
            cmd.set(row + " #InQueue.Text", "Queue: " + queueCount);

            // In-game count
            int inGameCount = getPlayersInArenaMatch(arena.getId());
            cmd.set(row + " #InGame.Text", "In Game: " + inGameCount);

            // Bind click event
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row,
                EventData.of("Action", "arena").append("Arena", arena.getId()),
                false
            );
        }

        // Bind close button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        // Register with HudManager for proper cleanup
        hudManager.registerPage(playerUuid, this);

        // Start auto-refresh for queue counts
        startAutoRefresh();
    }

    /**
     * Filters arenas by the currently selected game mode.
     * Index 0 = all arenas, 1+ = specific game mode.
     */
    private List<Arena> filterArenas() {
        Collection<Arena> allArenas = matchManager.getArenas();
        if (selectedModeIndex == 0) {
            return new ArrayList<>(allArenas);
        }

        String modeId = gameModeEntries.get(selectedModeIndex).id;
        List<Arena> filtered = new ArrayList<>();
        for (Arena arena : allArenas) {
            if (modeId.equals(arena.getGameMode())) {
                filtered.add(arena);
            }
        }
        return filtered;
    }

    /**
     * Gets the number of players in active matches for an arena.
     */
    private int getPlayersInArenaMatch(String arenaId) {
        int count = 0;
        for (Match match : matchManager.getActiveMatches()) {
            if (match.getArena().getId().equals(arenaId) && !match.isFinished()) {
                count += match.getParticipants().size();
            }
        }
        return count;
    }

    /**
     * Starts auto-refresh for queue/game counts.
     * Only sends an update when values actually change to avoid stealing UI focus.
     */
    private void startAutoRefresh() {
        if (refreshTask != null || scheduler == null) {
            return;
        }

        int count = filteredArenas.size();
        lastInUse = new boolean[count];
        lastQueueCount = new int[count];
        lastInGameCount = new int[count];

        // Initialize with current values so first tick doesn't send a redundant update
        for (int i = 0; i < count; i++) {
            Arena arena = filteredArenas.get(i);
            lastInUse[i] = matchManager.isArenaInUse(arena.getId());
            lastQueueCount[i] = queueManager.getQueueSize(arena.getId());
            lastInGameCount[i] = getPlayersInArenaMatch(arena.getId());
        }

        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            if (!active) {
                return;
            }

            try {
                UICommandBuilder cmd = new UICommandBuilder();
                boolean changed = false;

                int size = filteredArenas.size();
                for (int i = 0; i < size; i++) {
                    Arena arena = filteredArenas.get(i);
                    String row = "#ArenaList[" + i + "]";

                    boolean inUse = matchManager.isArenaInUse(arena.getId());
                    int queueCount = queueManager.getQueueSize(arena.getId());
                    int inGameCount = getPlayersInArenaMatch(arena.getId());

                    if (inUse != lastInUse[i]) {
                        lastInUse[i] = inUse;
                        changed = true;
                        if (inUse) {
                            cmd.set(row + " #StatusTag.Text", "IN USE");
                            cmd.set(row + " #StatusTag.Style.TextColor", "#e74c3c");
                        } else {
                            cmd.set(row + " #StatusTag.Text", "AVAILABLE");
                            cmd.set(row + " #StatusTag.Style.TextColor", "#2ecc71");
                        }
                    }

                    if (queueCount != lastQueueCount[i]) {
                        lastQueueCount[i] = queueCount;
                        changed = true;
                        cmd.set(row + " #InQueue.Text", "Queue: " + queueCount);
                    }

                    if (inGameCount != lastInGameCount[i]) {
                        lastInGameCount[i] = inGameCount;
                        changed = true;
                        cmd.set(row + " #InGame.Text", "In Game: " + inGameCount);
                    }
                }

                if (changed) {
                    safeSendUpdate(cmd);
                }
            } catch (Exception e) {
                // Page might be closed
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Safely sends a UI update, checking active flag first.
     */
    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) {
            return;
        }
        try {
            sendUpdate(cmd, false);
        } catch (Exception e) {
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
            if (data == null || data.action == null) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());

            switch (data.action) {
                case "close":
                    stopAutoRefresh();
                    if (player != null) {
                        player.getPageManager().setPage(ref, store, Page.None);
                    }
                    break;

                case "mode":
                    if (data.index != null) {
                        try {
                            int newIndex = Integer.parseInt(data.index);
                            if (newIndex >= 0 && newIndex < gameModeEntries.size() && newIndex != selectedModeIndex) {
                                selectedModeIndex = newIndex;
                                stopAutoRefresh();
                                active = true;
                                rebuild();
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid index
                        }
                    }
                    break;

                case "arena":
                    if (data.arena != null) {
                        Arena arena = matchManager.getArena(data.arena);
                        if (arena == null) {
                            return;
                        }

                        stopAutoRefresh();

                        if (player != null) {
                            ArenaDetailPage detailPage = new ArenaDetailPage(
                                playerRef, playerUuid, arena,
                                matchManager, queueManager, kitManager, hudManager, scheduler,
                                this::onBackFromDetail
                            );
                            player.getPageManager().openCustomPage(ref, store, detailPage);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ArenaMenuPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Callback when returning from detail page.
     * Preserves the previously selected game mode index.
     */
    private void onBackFromDetail(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            ArenaMenuPage newPage = new ArenaMenuPage(
                playerRef, playerUuid, matchManager, queueManager, kitManager, hudManager,
                scheduler, selectedModeIndex
            );
            player.getPageManager().openCustomPage(ref, store, newPage);
        }
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

    /**
     * Simple holder for game mode list entries.
     */
    private static class GameModeEntry {
        final String id;
        final String displayName;

        GameModeEntry(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    public static class PageEventData {
        public String action;
        public String index;
        public String arena;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Index", Codec.STRING),
                    (d, v) -> d.index = v, d -> d.index).add()
                .append(new KeyedCodec<>("Arena", Codec.STRING),
                    (d, v) -> d.arena = v, d -> d.arena).add()
                .build();
    }
}
