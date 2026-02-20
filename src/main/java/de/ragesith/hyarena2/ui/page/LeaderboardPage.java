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
import de.ragesith.hyarena2.stats.LeaderboardEntry;
import de.ragesith.hyarena2.stats.LeaderboardResult;
import de.ragesith.hyarena2.stats.StatsManager;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Leaderboard page — two-column layout matching ArenaMenuPage pattern.
 * Left: scope tabs (Global + game modes). Right: leaderboard entries.
 */
public class LeaderboardPage extends InteractiveCustomUIPage<LeaderboardPage.PageEventData> implements CloseablePage {

    private static final int MAX_COLUMNS = 5;

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final StatsManager statsManager;
    private final MatchManager matchManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;

    private final List<ScopeEntry> scopeEntries;
    private int selectedScopeIndex;
    private String currentSort;
    private int currentPage;
    private int totalPages;

    private volatile boolean active = true;

    private static final ColumnDef[] PVP_COLUMNS = {
        new ColumnDef("Kills", "pvp_kills"),
        new ColumnDef("Deaths", "pvp_deaths"),
        new ColumnDef("K/D", "pvp_kd_ratio"),
        new ColumnDef("Wins", "matches_won"),
        new ColumnDef("Win%", "win_rate"),
    };

    private static final ColumnDef[] WAVE_COLUMNS = {
        new ColumnDef("Waves", "best_waves_survived"),
        new ColumnDef("PvE Kills", "pve_kills"),
        new ColumnDef("PvE Deaths", "pve_deaths"),
        new ColumnDef("Matches", "matches_played"),
    };

    private static final ColumnDef[] SPEEDRUN_COLUMNS = {
        new ColumnDef("Best Time", "best_time_ms"),
        new ColumnDef("Matches", "matches_played"),
        new ColumnDef("Wins", "matches_won"),
    };

    public LeaderboardPage(PlayerRef playerRef, UUID playerUuid,
                           StatsManager statsManager, MatchManager matchManager,
                           HudManager hudManager, ScheduledExecutorService scheduler) {
        this(playerRef, playerUuid, statsManager, matchManager, hudManager, scheduler, 0, null, 1);
    }

    public LeaderboardPage(PlayerRef playerRef, UUID playerUuid,
                           StatsManager statsManager, MatchManager matchManager,
                           HudManager hudManager, ScheduledExecutorService scheduler,
                           int selectedScopeIndex, String currentSort, int currentPage) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.statsManager = statsManager;
        this.matchManager = matchManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.selectedScopeIndex = selectedScopeIndex;
        this.currentPage = currentPage;

        this.scopeEntries = new ArrayList<>();
        this.scopeEntries.add(new ScopeEntry("global", "Global"));
        for (GameMode gm : matchManager.getGameModes()) {
            this.scopeEntries.add(new ScopeEntry(gm.getId(), gm.getDisplayName()));
        }

        if (this.selectedScopeIndex < 0 || this.selectedScopeIndex >= scopeEntries.size()) {
            this.selectedScopeIndex = 0;
        }

        this.currentSort = (currentSort != null) ? currentSort : getDefaultSort();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/LeaderboardPage.ui");

        // Scope tabs — reuses GameModeButton.ui (same as ArenaMenuPage)
        for (int i = 0; i < scopeEntries.size(); i++) {
            ScopeEntry entry = scopeEntries.get(i);

            cmd.append("#ScopeList", "Pages/GameModeButton.ui");
            String row = "#ScopeList[" + i + "]";

            cmd.set(row + " #GMBtnName.Text", entry.displayName);

            if (i == selectedScopeIndex) {
                cmd.set(row + " #GMBtnName.Style.TextColor", "#e8c872");
            }

            cmd.set(row + " #GMBtnInfo.Visible", false);

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #GMBtnSelect",
                EventData.of("Action", "scope").append("Index", String.valueOf(i)),
                false
            );
        }

        // Header
        cmd.set("#ScopeName.Text", scopeEntries.get(selectedScopeIndex).displayName);

        // Column headers
        ColumnDef[] columns = getColumnsForScope();
        for (int c = 0; c < MAX_COLUMNS; c++) {
            if (c < columns.length) {
                cmd.set("#HdrStat" + c + ".Text", columns[c].displayName);
            }
        }

        // Loading state
        cmd.set("#LoadingLabel.Visible", true);

        // Close button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            EventData.of("Action", "close"),
            false
        );

        hudManager.registerPage(playerUuid, this);

        // Fetch data
        String scope = scopeEntries.get(selectedScopeIndex).id;
        ColumnDef[] fetchColumns = columns;
        statsManager.fetchLeaderboard(scope, currentSort, currentPage)
            .thenAccept(result -> {
                if (!active) return;
                try {
                    populateResults(result, fetchColumns);
                } catch (Exception e) {
                    System.err.println("[LeaderboardPage] Error populating: " + e.getMessage());
                }
            });
    }

    private void populateResults(LeaderboardResult result, ColumnDef[] columns) {
        UICommandBuilder cmd = new UICommandBuilder();

        cmd.set("#LoadingLabel.Visible", false);

        if (result.isError()) {
            cmd.set("#ErrorLabel.Visible", true);
            safeSendUpdate(cmd);
            return;
        }

        if (result.isEmpty()) {
            cmd.set("#EmptyNotice.Visible", true);
            safeSendUpdate(cmd);
            return;
        }

        cmd.set("#TotalPlayers.Text", result.getTotal() + " players");

        List<LeaderboardEntry> entries = result.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);

            cmd.append("#EntryList", "Pages/LeaderboardRow.ui");
            String row = "#EntryList[" + i + "]";

            cmd.set(row + " #Rank.Text", String.valueOf(entry.getRank()));
            switch (entry.getRank()) {
                case 1 -> cmd.set(row + " #Rank.Style.TextColor", "#ffd700");
                case 2 -> cmd.set(row + " #Rank.Style.TextColor", "#c0c0c0");
                case 3 -> cmd.set(row + " #Rank.Style.TextColor", "#cd7f32");
            }

            cmd.set(row + " #PlayerName.Text", entry.getUsername());

            for (int c = 0; c < MAX_COLUMNS; c++) {
                if (c < columns.length) {
                    cmd.set(row + " #Stat" + c + ".Text", getStatValue(entry, columns[c].sortField));
                    if (columns[c].sortField.equals(currentSort)) {
                        cmd.set(row + " #Stat" + c + ".Style.TextColor", "#e8c872");
                    }
                }
            }
        }

        safeSendUpdate(cmd);
    }

    private String getStatValue(LeaderboardEntry entry, String field) {
        return switch (field) {
            case "pvp_kills" -> String.valueOf(entry.getPvpKills());
            case "pvp_deaths" -> String.valueOf(entry.getPvpDeaths());
            case "pvp_kd_ratio" -> formatDouble(entry.getKdRatio());
            case "matches_won" -> String.valueOf(entry.getMatchesWon());
            case "win_rate" -> formatDouble(entry.getWinRate()) + "%";
            case "pve_kills" -> String.valueOf(entry.getPveKills());
            case "pve_deaths" -> String.valueOf(entry.getPveDeaths());
            case "best_waves_survived" -> String.valueOf(entry.getBestWavesSurvived());
            case "matches_played" -> String.valueOf(entry.getMatchesPlayed());
            case "best_time_ms" -> formatTimeMs(entry.getBestTimeMs());
            default -> "0";
        };
    }

    private String formatTimeMs(int ms) {
        if (ms <= 0) return "-";
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        int millis = ms % 1000;
        return String.format("%d:%02d.%03d", minutes, seconds, millis);
    }

    private String formatDouble(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    private ColumnDef[] getColumnsForScope() {
        if (selectedScopeIndex > 0) {
            String scopeId = scopeEntries.get(selectedScopeIndex).id;
            if ("wave_defense".equals(scopeId)) {
                return WAVE_COLUMNS;
            }
            if ("speed_run".equals(scopeId)) {
                return SPEEDRUN_COLUMNS;
            }
        }
        return PVP_COLUMNS;
    }

    private String getDefaultSort() {
        if (selectedScopeIndex > 0) {
            String scopeId = scopeEntries.get(selectedScopeIndex).id;
            if ("wave_defense".equals(scopeId)) {
                return "best_waves_survived";
            }
            if ("speed_run".equals(scopeId)) {
                return "best_time_ms";
            }
        }
        return "pvp_kills";
    }

    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) return;
        try {
            sendUpdate(cmd, false);
        } catch (Exception e) {
            active = false;
        }
    }

    private void rebuildPage() {
        if (!active) return;
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        LeaderboardPage newPage = new LeaderboardPage(
            playerRef, playerUuid, statsManager, matchManager,
            hudManager, scheduler, selectedScopeIndex, currentSort, currentPage
        );
        p.getPageManager().openCustomPage(pRef, pStore, newPage);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null || data.action == null) return;

            Player player = store.getComponent(ref, Player.getComponentType());

            switch (data.action) {
                case "close":
                    active = false;
                    if (player != null) {
                        player.getPageManager().setPage(ref, store, Page.None);
                    }
                    break;

                case "scope":
                    if (data.index != null) {
                        try {
                            int newIndex = Integer.parseInt(data.index);
                            if (newIndex >= 0 && newIndex < scopeEntries.size() && newIndex != selectedScopeIndex) {
                                selectedScopeIndex = newIndex;
                                currentSort = getDefaultSort();
                                currentPage = 1;
                                rebuildPage();
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[LeaderboardPage] Error: " + e.getMessage());
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

    private static class ScopeEntry {
        final String id;
        final String displayName;

        ScopeEntry(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    private static class ColumnDef {
        final String displayName;
        final String sortField;

        ColumnDef(String displayName, String sortField) {
            this.displayName = displayName;
            this.sortField = sortField;
        }
    }

    public static class PageEventData {
        public String action;
        public String index;
        public String field;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Index", Codec.STRING),
                    (d, v) -> d.index = v, d -> d.index).add()
                .append(new KeyedCodec<>("Field", Codec.STRING),
                    (d, v) -> d.field = v, d -> d.field).add()
                .build();
    }
}
