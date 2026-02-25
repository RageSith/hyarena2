package de.ragesith.hyarena2.ui.page.admin;

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
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import de.ragesith.hyarena2.arena.Arena;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Admin page listing all arenas with Edit/Delete/Create buttons.
 */
public class ArenaListPage extends InteractiveCustomUIPage<ArenaListPage.PageEventData> implements CloseablePage {

    // Per-player persistent state (survives page close/reopen, lost on server restart)
    private static final Map<UUID, String> playerSelectedArena = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, String> playerFilterGameMode = new java.util.concurrent.ConcurrentHashMap<>();

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final MatchManager matchManager;
    private final KitManager kitManager;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;
    private List<Arena> arenaList;

    // Track which arena is pending delete confirmation (null = none)
    private String pendingDeleteId;

    // Current game mode filter (null or "all" = show all)
    private String filterGameMode;

    // Currently selected arena for quick edit (null = none)
    private String selectedArenaId;

    public ArenaListPage(PlayerRef playerRef, UUID playerUuid,
                         MatchManager matchManager, KitManager kitManager,
                         HubManager hubManager, ConfigManager configManager,
                         HudManager hudManager, ScheduledExecutorService scheduler,
                         Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.matchManager = matchManager;
        this.kitManager = kitManager;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;
        this.filterGameMode = playerFilterGameMode.get(playerUuid);
        this.selectedArenaId = playerSelectedArena.get(playerUuid);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ArenaListPage.ui");

        // Build full arena list sorted alphabetically
        List<Arena> allArenas = new ArrayList<>(matchManager.getArenas());
        allArenas.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));

        // Populate filter dropdown: "All" + each game mode that has arenas
        var filterEntries = new ArrayList<DropdownEntryInfo>();
        filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString("All (" + allArenas.size() + ")"), "all"));
        Map<String, Integer> modeCounts = new java.util.LinkedHashMap<>();
        for (Arena a : allArenas) {
            modeCounts.merge(a.getGameMode(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : modeCounts.entrySet()) {
            GameMode gm = matchManager.getGameMode(entry.getKey());
            String label = (gm != null ? gm.getDisplayName() : entry.getKey()) + " (" + entry.getValue() + ")";
            filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString(label), entry.getKey()));
        }
        cmd.set("#FilterDropdown.Entries", filterEntries);
        cmd.set("#FilterDropdown.Value", filterGameMode != null ? filterGameMode : "all");

        // Apply filter
        boolean filtering = filterGameMode != null && !"all".equals(filterGameMode);
        arenaList = filtering
            ? allArenas.stream().filter(a -> filterGameMode.equals(a.getGameMode())).collect(java.util.stream.Collectors.toList())
            : allArenas;

        String countText = filtering
            ? arenaList.size() + " of " + allArenas.size() + " arenas"
            : allArenas.size() + " arenas loaded";
        cmd.set("#ArenaCountLabel.Text", countText);

        for (int i = 0; i < arenaList.size(); i++) {
            Arena arena = arenaList.get(i);

            cmd.append("#ArenaList", "Pages/AdminArenaRow.ui");
            String row = "#ArenaList[" + i + "]";

            cmd.set(row + " #RowArenaName.Text", arena.getDisplayName());
            String info = matchManager.getGameModeDisplayName(arena) + " | " + MatchManager.formatPlayerCount(arena);
            cmd.set(row + " #RowArenaInfo.Text", info);

            // Highlight selected row
            if (arena.getId().equals(selectedArenaId)) {
                cmd.set(row + ".Background", "#1a2a3a");
            }

            // Select button
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowSelectBtn",
                EventData.of("Action", "select").append("Arena", arena.getId()),
                false
            );

            // Teleport button
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowTpBtn",
                EventData.of("Action", "teleport").append("Arena", arena.getId()),
                false
            );

            // Edit button
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowEditBtn",
                EventData.of("Action", "edit").append("Arena", arena.getId()),
                false
            );

            // Delete button — set text directly on the button element
            if (arena.getId().equals(pendingDeleteId)) {
                cmd.set(row + " #RowDeleteBtn.Text", "Confirm?");
            }

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowDeleteBtn",
                EventData.of("Action", "delete").append("Arena", arena.getId()),
                false
            );
        }

        // Show selection label + Edit Arena button if an arena is selected
        if (selectedArenaId != null) {
            Arena selected = matchManager.getArena(selectedArenaId);
            if (selected != null) {
                cmd.set("#SelectionLabel.Text", "Selected: " + selected.getDisplayName());
                cmd.set("#EditArenaBtn.Visible", true);
            }
        }

        // Bind buttons
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CreateNewBtn",
            EventData.of("Action", "create"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackBtn",
            EventData.of("Action", "back"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SyncChunksBtn",
            EventData.of("Action", "sync_chunks"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        // Filter dropdown
        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#FilterDropdown",
            EventData.of("Action", "filter").append("@Value", "#FilterDropdown.Value"),
            false
        );

        // Edit Arena button (quick edit for selected arena)
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#EditArenaBtn",
            EventData.of("Action", "edit_selected"),
            false
        );

        hudManager.registerPage(playerUuid, this);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null || data.action == null) return;

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            switch (data.action) {
                case "close":
                    player.getPageManager().setPage(ref, store, Page.None);
                    break;

                case "back":
                    shutdown();
                    if (onBack != null) onBack.run();
                    break;

                case "create":
                    openEditor(ref, store, player, null);
                    break;

                case "teleport":
                    if (data.arena != null) {
                        handleTeleport(ref, store, player, data.arena);
                    }
                    break;

                case "edit":
                    if (data.arena != null) {
                        Arena arena = matchManager.getArena(data.arena);
                        if (arena != null) {
                            openEditor(ref, store, player, arena);
                        } else {
                            showStatus("Arena not found: " + data.arena, "#e74c3c");
                        }
                    }
                    break;

                case "delete":
                    handleDelete(data.arena);
                    break;

                case "sync_chunks":
                    handleSyncChunks();
                    break;

                case "select":
                    if (data.arena != null) {
                        // Toggle: clicking the already-selected arena deselects it
                        if (data.arena.equals(selectedArenaId)) {
                            selectedArenaId = null;
                            playerSelectedArena.remove(playerUuid);
                        } else {
                            selectedArenaId = data.arena;
                            playerSelectedArena.put(playerUuid, data.arena);
                        }
                        reopenSelf();
                    }
                    break;

                case "edit_selected":
                    if (selectedArenaId != null) {
                        Arena selected = matchManager.getArena(selectedArenaId);
                        if (selected != null) {
                            openEditor(ref, store, player, selected);
                        }
                    }
                    break;

                case "filter":
                    if (data.value != null) {
                        if ("all".equals(data.value)) {
                            filterGameMode = null;
                            playerFilterGameMode.remove(playerUuid);
                        } else {
                            filterGameMode = data.value;
                            playerFilterGameMode.put(playerUuid, data.value);
                        }
                        reopenSelf();
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ArenaListPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleTeleport(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String arenaId) {
        Arena arena = matchManager.getArena(arenaId);
        if (arena == null) {
            showStatus("Arena not found: " + arenaId, "#e74c3c");
            return;
        }

        ArenaConfig config = arena.getConfig();
        List<ArenaConfig.SpawnPoint> spawns = config.getSpawnPoints();
        if (spawns == null || spawns.isEmpty()) {
            showStatus("Arena has no spawn points", "#e74c3c");
            return;
        }

        World targetWorld = Universe.get().getWorld(config.getWorldName());
        if (targetWorld == null) {
            showStatus("World not found: " + config.getWorldName(), "#e74c3c");
            return;
        }

        ArenaConfig.SpawnPoint sp = spawns.get(0);
        Position pos = new Position(sp.getX(), sp.getY(), sp.getZ(), sp.getYaw(), sp.getPitch());

        // Close the page and teleport
        player.getPageManager().setPage(ref, store, Page.None);
        shutdown();
        hubManager.teleportPlayerToWorld(player, pos, targetWorld);
    }

    private void openEditor(Ref<EntityStore> ref, Store<EntityStore> store, Player player, Arena arena) {
        shutdown();
        ArenaEditorPage editorPage = new ArenaEditorPage(
            playerRef, playerUuid,
            arena != null ? arena.getConfig() : null,
            matchManager, kitManager, hubManager, configManager,
            hudManager, scheduler,
            this::reopenSelf
        );
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    private void handleDelete(String arenaId) {
        if (arenaId == null) return;

        if (!arenaId.equals(pendingDeleteId)) {
            // First click — show confirmation on the button
            pendingDeleteId = arenaId;
            for (int i = 0; i < arenaList.size(); i++) {
                if (arenaList.get(i).getId().equals(arenaId)) {
                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#ArenaList[" + i + "] #RowDeleteBtn.Text", "Confirm?");
                    safeSendUpdate(cmd);
                    break;
                }
            }
            return;
        }

        // Second click — actually delete
        if (matchManager.isArenaInUse(arenaId)) {
            showStatus("Cannot delete: arena is in use", "#e74c3c");
            pendingDeleteId = null;
            return;
        }

        boolean deleted = matchManager.deleteArena(arenaId);
        pendingDeleteId = null;

        if (deleted) {
            reopenSelf();
        } else {
            showStatus("Failed to delete arena", "#e74c3c");
        }
    }

    private void handleSyncChunks() {
        try {
            String hubWorld = configManager.getHubConfig().getEffectiveWorldName();

            // Group arenas by world, skipping the hub world
            Map<String, List<ArenaConfig.Bounds>> worldBounds = new HashMap<>();
            for (Arena arena : matchManager.getArenas()) {
                String world = arena.getConfig().getWorldName();
                if (world == null || world.equals(hubWorld)) continue;
                ArenaConfig.Bounds b = arena.getConfig().getBounds();
                if (b == null) continue;
                worldBounds.computeIfAbsent(world, k -> new ArrayList<>()).add(b);
            }

            if (worldBounds.isEmpty()) {
                showStatus("No arena worlds to sync", "#6b7d8e");
                return;
            }

            // Build commands first, then dispatch with delays to avoid race conditions
            List<String> commands = new ArrayList<>();
            for (Map.Entry<String, List<ArenaConfig.Bounds>> entry : worldBounds.entrySet()) {
                String worldName = entry.getKey();
                List<ArenaConfig.Bounds> boundsList = entry.getValue();

                double unionMinX = Double.MAX_VALUE, unionMinZ = Double.MAX_VALUE;
                double unionMaxX = -Double.MAX_VALUE, unionMaxZ = -Double.MAX_VALUE;
                for (ArenaConfig.Bounds b : boundsList) {
                    double loX = Math.min(b.getMinX(), b.getMaxX());
                    double hiX = Math.max(b.getMinX(), b.getMaxX());
                    double loZ = Math.min(b.getMinZ(), b.getMaxZ());
                    double hiZ = Math.max(b.getMinZ(), b.getMaxZ());
                    unionMinX = Math.min(unionMinX, loX);
                    unionMinZ = Math.min(unionMinZ, loZ);
                    unionMaxX = Math.max(unionMaxX, hiX);
                    unionMaxZ = Math.max(unionMaxZ, hiZ);
                }

                int minX = (int) Math.floor(unionMinX) - 2;
                int minZ = (int) Math.floor(unionMinZ) - 2;
                int maxX = (int) Math.ceil(unionMaxX) + 2;
                int maxZ = (int) Math.ceil(unionMaxZ) + 2;

                commands.add("world settings keeploaded set " + minX + " " + minZ + " " + maxX + " " + maxZ + " --world " + worldName);
            }

            // Dispatch commands staggered by 500ms each
            for (int i = 0; i < commands.size(); i++) {
                String command = commands.get(i);
                long delayMs = i * 500L;
                scheduler.schedule(() -> {
                    CommandManager.get().handleCommand(ConsoleSender.INSTANCE, command);
                }, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            showStatus("Synced " + commands.size() + " world(s)", "#2ecc71");
        } catch (Exception e) {
            System.err.println("[ArenaListPage] Sync chunks error: " + e.getMessage());
            e.printStackTrace();
            showStatus("Sync failed: " + e.getMessage(), "#e74c3c");
        }
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#ArenaCountLabel.Text", message);
        cmd.set("#ArenaCountLabel.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void reopenSelf() {
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        ArenaListPage page = new ArenaListPage(
            playerRef, playerUuid, matchManager, kitManager,
            hubManager, configManager, hudManager, scheduler, onBack
        );
        p.getPageManager().openCustomPage(pRef, pStore, page);
    }

    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) return;
        try {
            sendUpdate(cmd, false);
        } catch (Exception e) {
            active = false;
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        shutdown();
    }

    @Override
    public void shutdown() {
        active = false;
        hudManager.unregisterPage(playerUuid, this);
    }

    @Override
    public void close() {
        shutdown();
    }

    public static class PageEventData {
        public String action;
        public String arena;
        public String value;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Arena", Codec.STRING),
                    (d, v) -> d.arena = v, d -> d.arena).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING),
                    (d, v) -> d.value = v, d -> d.value).add()
                .build();
    }
}
