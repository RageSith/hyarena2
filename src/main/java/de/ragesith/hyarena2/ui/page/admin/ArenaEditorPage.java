package de.ragesith.hyarena2.ui.page.admin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import de.ragesith.hyarena2.HyArena2;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.arena.ArenaConfig;
import de.ragesith.hyarena2.arena.MatchManager;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.gamemode.GameMode;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Arena editor page for creating and editing arenas.
 * Full form with all arena config fields, position capture, and dynamic lists.
 */
public class ArenaEditorPage extends InteractiveCustomUIPage<ArenaEditorPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final boolean isCreate;
    private final MatchManager matchManager;
    private final KitManager kitManager;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;

    // Form state
    private String formId;
    private String formDisplayName;
    private String formWorldName;
    private String formGameMode;
    private int formMinPlayers;
    private int formMaxPlayers;
    private int formWaitTime;
    private int formDuration;
    private int formKillTarget;
    private int formRespawnDelay;
    private int formScoreTarget;
    private int formZoneRotation;
    private String formBotDifficulty;
    private String formBotModelId;
    private boolean formAutoFill;
    private int formAutoFillDelay;
    private int formMinRealPlayers;
    private int formWaveBonusPerKill;
    private int formWaveBonusPerWave;

    private List<String> formAllowedKits;
    private boolean formSwapOnKill;
    private boolean formSwapOnRespawn;
    private List<String> formRandomKitPool;
    private List<ArenaConfig.SpawnPoint> formSpawnPoints;
    private double[] formBoundsMin; // [x, y, z]
    private double[] formBoundsMax;
    private List<ArenaConfig.CaptureZone> formCaptureZones;
    private List<ArenaConfig.SpawnPoint> formWaveSpawnPoints;
    private List<ArenaConfig.SpawnPoint> formNavWaypoints;

    private List<String> gameModeIds;
    private static final String[] BOT_DIFFICULTIES = {"EASY", "MEDIUM", "HARD", "EXTREME", "EASY_TANK", "MEDIUM_TANK", "HARD_TANK", "EXTREME_TANK"};

    public ArenaEditorPage(PlayerRef playerRef, UUID playerUuid,
                           ArenaConfig existingConfig,
                           MatchManager matchManager, KitManager kitManager,
                           HubManager hubManager, ConfigManager configManager,
                           HudManager hudManager, ScheduledExecutorService scheduler,
                           Runnable onBack) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.isCreate = (existingConfig == null);
        this.matchManager = matchManager;
        this.kitManager = kitManager;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;

        // Build game mode list
        this.gameModeIds = new ArrayList<>();
        for (GameMode gm : matchManager.getGameModes()) {
            gameModeIds.add(gm.getId());
        }

        // Initialize form state
        if (existingConfig != null) {
            formId = existingConfig.getId();
            formDisplayName = existingConfig.getDisplayName();
            formWorldName = existingConfig.getWorldName();
            formGameMode = existingConfig.getGameMode();
            formMinPlayers = existingConfig.getMinPlayers();
            formMaxPlayers = existingConfig.getMaxPlayers();
            formWaitTime = existingConfig.getWaitTimeSeconds();
            formDuration = existingConfig.getMatchDurationSeconds();
            formKillTarget = existingConfig.getKillTarget();
            formRespawnDelay = existingConfig.getRespawnDelaySeconds();
            formScoreTarget = existingConfig.getScoreTarget();
            formZoneRotation = existingConfig.getZoneRotationSeconds();
            formBotDifficulty = existingConfig.getBotDifficulty() != null ? existingConfig.getBotDifficulty() : "MEDIUM";
            formBotModelId = existingConfig.getBotModelId() != null ? existingConfig.getBotModelId() : "";
            formAutoFill = existingConfig.isAutoFillEnabled();
            formAutoFillDelay = existingConfig.getAutoFillDelaySeconds();
            formMinRealPlayers = existingConfig.getMinRealPlayers();
            formWaveBonusPerKill = existingConfig.getWaveBonusSecondsPerKill();
            formWaveBonusPerWave = existingConfig.getWaveBonusSecondsPerWaveClear();
            formAllowedKits = existingConfig.getAllowedKits() != null ? new ArrayList<>(existingConfig.getAllowedKits()) : new ArrayList<>();
            formSwapOnKill = existingConfig.isKitRouletteSwapOnKill();
            formSwapOnRespawn = existingConfig.isKitRouletteSwapOnRespawn();
            formRandomKitPool = existingConfig.getRandomKitPool() != null ? new ArrayList<>(existingConfig.getRandomKitPool()) : new ArrayList<>();
            formSpawnPoints = existingConfig.getSpawnPoints() != null ? new ArrayList<>(existingConfig.getSpawnPoints()) : new ArrayList<>();

            ArenaConfig.Bounds bounds = existingConfig.getBounds();
            if (bounds != null) {
                formBoundsMin = new double[]{bounds.getMinX(), bounds.getMinY(), bounds.getMinZ()};
                formBoundsMax = new double[]{bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ()};
            } else {
                formBoundsMin = new double[]{0, 0, 0};
                formBoundsMax = new double[]{0, 0, 0};
            }

            formCaptureZones = existingConfig.getCaptureZones() != null ? new ArrayList<>(existingConfig.getCaptureZones()) : new ArrayList<>();
            formWaveSpawnPoints = existingConfig.getWaveSpawnPoints() != null ? new ArrayList<>(existingConfig.getWaveSpawnPoints()) : new ArrayList<>();
            formNavWaypoints = existingConfig.getNavWaypoints() != null ? new ArrayList<>(existingConfig.getNavWaypoints()) : new ArrayList<>();
        } else {
            formId = "";
            formDisplayName = "";
            formWorldName = "default";
            formGameMode = gameModeIds.isEmpty() ? "duel" : gameModeIds.get(0);
            formMinPlayers = 2;
            formMaxPlayers = 2;
            formWaitTime = 30;
            formDuration = 300;
            formKillTarget = 0;
            formRespawnDelay = 3;
            formScoreTarget = 60;
            formZoneRotation = 60;
            formBotDifficulty = "MEDIUM";
            formBotModelId = "";
            formAutoFill = false;
            formAutoFillDelay = 30;
            formMinRealPlayers = 1;
            formWaveBonusPerKill = 2;
            formWaveBonusPerWave = 60;
            formAllowedKits = new ArrayList<>();
            formSwapOnKill = true;
            formSwapOnRespawn = false;
            formRandomKitPool = new ArrayList<>();
            formSpawnPoints = new ArrayList<>();
            formBoundsMin = new double[]{0, 0, 0};
            formBoundsMax = new double[]{0, 0, 0};
            formCaptureZones = new ArrayList<>();
            formWaveSpawnPoints = new ArrayList<>();
            formNavWaypoints = new ArrayList<>();
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ArenaEditorPage.ui");

        // Title
        cmd.set("#TitleLabel.Text", isCreate ? "Create Arena" : "Edit Arena: " + formId);

        // Basic info fields — use .Value for input elements
        cmd.set("#IdField.Value", formId);
        cmd.set("#DisplayNameField.Value", formDisplayName);
        cmd.set("#WorldNameField.Value", formWorldName);
        cmd.set("#MinPlayersField.Value", formMinPlayers);
        cmd.set("#MaxPlayersField.Value", formMaxPlayers);
        cmd.set("#WaitTimeField.Value", formWaitTime);
        cmd.set("#DurationField.Value", formDuration);

        // Game mode dropdown
        var gameModeEntries = new ArrayList<DropdownEntryInfo>();
        for (String gmId : gameModeIds) {
            GameMode gm = matchManager.getGameMode(gmId);
            String label = gm != null ? gm.getDisplayName() : gmId;
            gameModeEntries.add(new DropdownEntryInfo(LocalizableString.fromString(label), gmId));
        }
        cmd.set("#GameModeDropdown.Entries", gameModeEntries);
        cmd.set("#GameModeDropdown.Value", formGameMode);

        // Conditional game mode settings visibility
        boolean showKillTarget = "deathmatch".equals(formGameMode) || "kit_roulette".equals(formGameMode);
        boolean showRespawn = "deathmatch".equals(formGameMode) || "koth".equals(formGameMode) || "kit_roulette".equals(formGameMode);
        boolean showScore = "koth".equals(formGameMode);
        boolean showZoneRotation = "koth".equals(formGameMode);
        boolean showRandomKitPool = "kit_roulette".equals(formGameMode);
        boolean showCaptureZones = "koth".equals(formGameMode);
        boolean showWaveSpawnPoints = "wave_defense".equals(formGameMode);

        cmd.set("#KillTargetRow.Visible", showKillTarget);
        cmd.set("#RespawnDelayRow.Visible", showRespawn);
        cmd.set("#ScoreTargetRow.Visible", showScore);
        cmd.set("#ZoneRotationRow.Visible", showZoneRotation);
        cmd.set("#RandomKitPoolSection.Visible", showRandomKitPool);
        cmd.set("#CaptureZonesSection.Visible", showCaptureZones);
        cmd.set("#WaveSpawnPointsSection.Visible", showWaveSpawnPoints);

        cmd.set("#KillTargetField.Value", formKillTarget);
        cmd.set("#RespawnDelayField.Value", formRespawnDelay);
        cmd.set("#ScoreTargetField.Value", formScoreTarget);
        cmd.set("#ZoneRotationField.Value", formZoneRotation);

        // Allowed kits
        for (int i = 0; i < formAllowedKits.size(); i++) {
            cmd.append("#AllowedKitsList", "Pages/AdminStringRow.ui");
            String row = "#AllowedKitsList[" + i + "]";
            cmd.set(row + " #RowTextField.Value", formAllowedKits.get(i));

            events.addEventBinding(CustomUIEventBindingType.ValueChanged, row + " #RowTextField",
                EventData.of("Action", "field").append("Field", "kitValue").append("Index", String.valueOf(i))
                    .append("@Value",row + " #RowTextField.Value"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowRemoveBtn",
                EventData.of("Action", "removeKit").append("Index", String.valueOf(i)), false);
        }

        // Random kit pool
        if (showRandomKitPool) {
            for (int i = 0; i < formRandomKitPool.size(); i++) {
                cmd.append("#RandomKitPoolList", "Pages/AdminStringRow.ui");
                String row = "#RandomKitPoolList[" + i + "]";
                cmd.set(row + " #RowTextField.Value", formRandomKitPool.get(i));

                events.addEventBinding(CustomUIEventBindingType.ValueChanged, row + " #RowTextField",
                    EventData.of("Action", "field").append("Field", "rkitValue").append("Index", String.valueOf(i))
                        .append("@Value",row + " #RowTextField.Value"), false);
                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowRemoveBtn",
                    EventData.of("Action", "removeRKit").append("Index", String.valueOf(i)), false);
            }

            cmd.set("#SwapOnKillCheckbox.Value", formSwapOnKill);
            cmd.set("#SwapOnRespawnCheckbox.Value", formSwapOnRespawn);
        }

        // Bot settings
        var botDiffEntries = new ArrayList<DropdownEntryInfo>();
        for (String diff : BOT_DIFFICULTIES) {
            botDiffEntries.add(new DropdownEntryInfo(LocalizableString.fromString(formatDifficulty(diff)), diff));
        }
        cmd.set("#BotDifficultyDropdown.Entries", botDiffEntries);
        cmd.set("#BotDifficultyDropdown.Value", formBotDifficulty);

        cmd.set("#BotModelField.Value", formBotModelId);
        cmd.set("#AutoFillCheckbox.Value", formAutoFill);
        cmd.set("#AutoFillDelayField.Value", formAutoFillDelay);
        cmd.set("#MinRealPlayersField.Value", formMinRealPlayers);

        // Spawn points
        for (int i = 0; i < formSpawnPoints.size(); i++) {
            ArenaConfig.SpawnPoint sp = formSpawnPoints.get(i);
            cmd.append("#SpawnPointsList", "Pages/AdminSpawnRow.ui");
            String row = "#SpawnPointsList[" + i + "]";

            cmd.set(row + " #SpawnIndex.Text", "#" + (i + 1));
            cmd.set(row + " #SpawnXField.Value", formatCoord(sp.getX()));
            cmd.set(row + " #SpawnYField.Value", formatCoord(sp.getY()));
            cmd.set(row + " #SpawnZField.Value", formatCoord(sp.getZ()));
            cmd.set(row + " #SpawnYawField.Value", formatCoord(sp.getYaw()));
            cmd.set(row + " #SpawnPitchField.Value", formatCoord(sp.getPitch()));

            bindTextField(events, row + " #SpawnXField", "spawnX", String.valueOf(i));
            bindTextField(events, row + " #SpawnYField", "spawnY", String.valueOf(i));
            bindTextField(events, row + " #SpawnZField", "spawnZ", String.valueOf(i));
            bindTextField(events, row + " #SpawnYawField", "spawnYaw", String.valueOf(i));
            bindTextField(events, row + " #SpawnPitchField", "spawnPitch", String.valueOf(i));

            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #SpawnSetBtn",
                EventData.of("Action", "setSpawn").append("Index", String.valueOf(i)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #SpawnRemoveBtn",
                EventData.of("Action", "removeSpawn").append("Index", String.valueOf(i)), false);
        }

        // Bounds
        cmd.set("#BoundsMinXField.Value", formatCoord(formBoundsMin[0]));
        cmd.set("#BoundsMinYField.Value", formatCoord(formBoundsMin[1]));
        cmd.set("#BoundsMinZField.Value", formatCoord(formBoundsMin[2]));
        cmd.set("#BoundsMaxXField.Value", formatCoord(formBoundsMax[0]));
        cmd.set("#BoundsMaxYField.Value", formatCoord(formBoundsMax[1]));
        cmd.set("#BoundsMaxZField.Value", formatCoord(formBoundsMax[2]));

        // Capture zones
        if (showCaptureZones) {
            for (int i = 0; i < formCaptureZones.size(); i++) {
                ArenaConfig.CaptureZone zone = formCaptureZones.get(i);
                cmd.append("#CaptureZonesList", "Pages/AdminZoneRow.ui");
                String row = "#CaptureZonesList[" + i + "]";

                cmd.set(row + " #ZoneNameField.Value", zone.getDisplayName() != null ? zone.getDisplayName() : "");
                cmd.set(row + " #ZoneMinXField.Value", formatCoord(zone.getMinX()));
                cmd.set(row + " #ZoneMinYField.Value", formatCoord(zone.getMinY()));
                cmd.set(row + " #ZoneMinZField.Value", formatCoord(zone.getMinZ()));
                cmd.set(row + " #ZoneMaxXField.Value", formatCoord(zone.getMaxX()));
                cmd.set(row + " #ZoneMaxYField.Value", formatCoord(zone.getMaxY()));
                cmd.set(row + " #ZoneMaxZField.Value", formatCoord(zone.getMaxZ()));

                bindTextField(events, row + " #ZoneNameField", "zoneName", String.valueOf(i));
                bindTextField(events, row + " #ZoneMinXField", "zoneMinX", String.valueOf(i));
                bindTextField(events, row + " #ZoneMinYField", "zoneMinY", String.valueOf(i));
                bindTextField(events, row + " #ZoneMinZField", "zoneMinZ", String.valueOf(i));
                bindTextField(events, row + " #ZoneMaxXField", "zoneMaxX", String.valueOf(i));
                bindTextField(events, row + " #ZoneMaxYField", "zoneMaxY", String.valueOf(i));
                bindTextField(events, row + " #ZoneMaxZField", "zoneMaxZ", String.valueOf(i));

                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #ZoneRemoveBtn",
                    EventData.of("Action", "removeZone").append("Index", String.valueOf(i)), false);
                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #ZoneSetMinBtn",
                    EventData.of("Action", "setZoneMin").append("Index", String.valueOf(i)), false);
                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #ZoneSetMaxBtn",
                    EventData.of("Action", "setZoneMax").append("Index", String.valueOf(i)), false);
            }
        }

        // Wave bonus time fields
        if (showWaveSpawnPoints) {
            cmd.set("#WaveBonusPerKillField.Value", formWaveBonusPerKill);
            cmd.set("#WaveBonusPerWaveField.Value", formWaveBonusPerWave);
        }

        // Wave spawn points
        if (showWaveSpawnPoints) {
            for (int i = 0; i < formWaveSpawnPoints.size(); i++) {
                ArenaConfig.SpawnPoint sp = formWaveSpawnPoints.get(i);
                cmd.append("#WaveSpawnPointsList", "Pages/AdminSpawnRow.ui");
                String row = "#WaveSpawnPointsList[" + i + "]";

                cmd.set(row + " #SpawnIndex.Text", "#" + (i + 1));
                cmd.set(row + " #SpawnXField.Value", formatCoord(sp.getX()));
                cmd.set(row + " #SpawnYField.Value", formatCoord(sp.getY()));
                cmd.set(row + " #SpawnZField.Value", formatCoord(sp.getZ()));
                cmd.set(row + " #SpawnYawField.Value", formatCoord(sp.getYaw()));
                cmd.set(row + " #SpawnPitchField.Value", formatCoord(sp.getPitch()));

                bindTextField(events, row + " #SpawnXField", "waveSpawnX", String.valueOf(i));
                bindTextField(events, row + " #SpawnYField", "waveSpawnY", String.valueOf(i));
                bindTextField(events, row + " #SpawnZField", "waveSpawnZ", String.valueOf(i));
                bindTextField(events, row + " #SpawnYawField", "waveSpawnYaw", String.valueOf(i));
                bindTextField(events, row + " #SpawnPitchField", "waveSpawnPitch", String.valueOf(i));

                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #SpawnSetBtn",
                    EventData.of("Action", "setWaveSpawn").append("Index", String.valueOf(i)), false);
                events.addEventBinding(CustomUIEventBindingType.Activating, row + " #SpawnRemoveBtn",
                    EventData.of("Action", "removeWaveSpawn").append("Index", String.valueOf(i)), false);
            }
        }

        // Nav waypoints (always visible)
        for (int i = 0; i < formNavWaypoints.size(); i++) {
            ArenaConfig.SpawnPoint sp = formNavWaypoints.get(i);
            cmd.append("#NavWaypointsList", "Pages/AdminSpawnRow.ui");
            String row = "#NavWaypointsList[" + i + "]";

            cmd.set(row + " #SpawnIndex.Text", "#" + (i + 1));
            cmd.set(row + " #SpawnXField.Value", formatCoord(sp.getX()));
            cmd.set(row + " #SpawnYField.Value", formatCoord(sp.getY()));
            cmd.set(row + " #SpawnZField.Value", formatCoord(sp.getZ()));
            cmd.set(row + " #SpawnYawField.Value", formatCoord(sp.getYaw()));
            cmd.set(row + " #SpawnPitchField.Value", formatCoord(sp.getPitch()));

            bindTextField(events, row + " #SpawnXField", "navWpX", String.valueOf(i));
            bindTextField(events, row + " #SpawnYField", "navWpY", String.valueOf(i));
            bindTextField(events, row + " #SpawnZField", "navWpZ", String.valueOf(i));
            bindTextField(events, row + " #SpawnYawField", "navWpYaw", String.valueOf(i));
            bindTextField(events, row + " #SpawnPitchField", "navWpPitch", String.valueOf(i));

            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #SpawnSetBtn",
                EventData.of("Action", "setNavWp").append("Index", String.valueOf(i)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #SpawnRemoveBtn",
                EventData.of("Action", "removeNavWp").append("Index", String.valueOf(i)), false);
        }

        // Bind text field events for basic fields
        bindTextField(events, "#IdField", "id", null);
        bindTextField(events, "#DisplayNameField", "displayName", null);
        bindTextField(events, "#WorldNameField", "worldName", null);
        bindTextField(events, "#BotModelField", "botModel", null);

        // Bind number field events
        bindNumberField(events, "#MinPlayersField", "minPlayers");
        bindNumberField(events, "#MaxPlayersField", "maxPlayers");
        bindNumberField(events, "#WaitTimeField", "waitTime");
        bindNumberField(events, "#DurationField", "duration");
        bindNumberField(events, "#KillTargetField", "killTarget");
        bindNumberField(events, "#RespawnDelayField", "respawnDelay");
        bindNumberField(events, "#ScoreTargetField", "scoreTarget");
        bindNumberField(events, "#ZoneRotationField", "zoneRotation");
        bindNumberField(events, "#AutoFillDelayField", "autoFillDelay");
        bindNumberField(events, "#MinRealPlayersField", "minRealPlayers");
        bindNumberField(events, "#WaveBonusPerKillField", "waveBonusPerKill");
        bindNumberField(events, "#WaveBonusPerWaveField", "waveBonusPerWave");

        // Bind bounds text fields
        bindTextField(events, "#BoundsMinXField", "boundsMinX", null);
        bindTextField(events, "#BoundsMinYField", "boundsMinY", null);
        bindTextField(events, "#BoundsMinZField", "boundsMinZ", null);
        bindTextField(events, "#BoundsMaxXField", "boundsMaxX", null);
        bindTextField(events, "#BoundsMaxYField", "boundsMaxY", null);
        bindTextField(events, "#BoundsMaxZField", "boundsMaxZ", null);

        // Bind dropdown events
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GameModeDropdown",
            EventData.of("Action", "field").append("Field", "gameMode")
                .append("@Value","#GameModeDropdown.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BotDifficultyDropdown",
            EventData.of("Action", "field").append("Field", "botDifficulty")
                .append("@Value","#BotDifficultyDropdown.Value"), false);

        // Bind checkbox events (uses @BoolValue — checkboxes send boolean, not string)
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AutoFillCheckbox",
            EventData.of("Action", "field").append("Field", "autoFill")
                .append("@BoolValue","#AutoFillCheckbox.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SwapOnKillCheckbox",
            EventData.of("Action", "field").append("Field", "swapOnKill")
                .append("@BoolValue","#SwapOnKillCheckbox.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SwapOnRespawnCheckbox",
            EventData.of("Action", "field").append("Field", "swapOnRespawn")
                .append("@BoolValue","#SwapOnRespawnCheckbox.Value"), false);

        // Bind action buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddKitBtn",
            EventData.of("Action", "addKit"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddRandomKitBtn",
            EventData.of("Action", "addRKit"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddSpawnBtn",
            EventData.of("Action", "addSpawn"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddSpawnHereBtn",
            EventData.of("Action", "addSpawnHere"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetMinBtn",
            EventData.of("Action", "setMin"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetMaxBtn",
            EventData.of("Action", "setMax"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddZoneBtn",
            EventData.of("Action", "addZone"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddWaveSpawnBtn",
            EventData.of("Action", "addWaveSpawn"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddWaveSpawnHereBtn",
            EventData.of("Action", "addWaveSpawnHere"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddNavWaypointBtn",
            EventData.of("Action", "addNavWp"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddNavWaypointHereBtn",
            EventData.of("Action", "addNavWpHere"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
            EventData.of("Action", "save"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
            EventData.of("Action", "cancel"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"), false);

        hudManager.registerPage(playerUuid, this);
    }

    private void bindTextField(UIEventBuilder events, String elementId, String field, String index) {
        EventData data = EventData.of("Action", "field").append("Field", field)
            .append("@Value",elementId + ".Value");
        if (index != null) {
            data = data.append("Index", index);
        }
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, elementId, data, false);
    }

    private void bindNumberField(UIEventBuilder events, String elementId, String field) {
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, elementId,
            EventData.of("Action", "field").append("Field", field)
                .append("@IntValue",elementId + ".Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, PageEventData data) {
        try {
            if (data == null || data.action == null) return;

            Player player = store.getComponent(ref, Player.getComponentType());

            switch (data.action) {
                case "close":
                    if (player != null) player.getPageManager().setPage(ref, store, Page.None);
                    break;

                case "cancel":
                    shutdown();
                    if (onBack != null) onBack.run();
                    break;

                case "field":
                    handleFieldChange(data);
                    break;

                case "addKit":
                    formAllowedKits.add("");
                    active = true;
                    rebuild();
                    break;

                case "removeKit":
                    removeAtIndex(formAllowedKits, data.index);
                    active = true;
                    rebuild();
                    break;

                case "addRKit":
                    formRandomKitPool.add("");
                    active = true;
                    rebuild();
                    break;

                case "removeRKit":
                    removeAtIndex(formRandomKitPool, data.index);
                    active = true;
                    rebuild();
                    break;

                case "addSpawn":
                    formSpawnPoints.add(new ArenaConfig.SpawnPoint(0, 0, 0, 0, 0));
                    active = true;
                    rebuild();
                    break;

                case "addSpawnHere":
                    addSpawnFromPosition();
                    break;

                case "setSpawn":
                    setSpawnFromPosition(data.index);
                    break;

                case "removeSpawn":
                    removeSpawnAtIndex(data.index);
                    active = true;
                    rebuild();
                    break;

                case "setMin":
                    setBoundsFromPosition(true);
                    break;

                case "setMax":
                    setBoundsFromPosition(false);
                    break;

                case "addZone":
                    formCaptureZones.add(new ArenaConfig.CaptureZone("Zone " + (formCaptureZones.size() + 1), 0, 0, 0, 0, 0, 0));
                    active = true;
                    rebuild();
                    break;

                case "removeZone":
                    removeZoneAtIndex(data.index);
                    active = true;
                    rebuild();
                    break;

                case "setZoneMin":
                    setZoneBoundsFromPosition(data.index, true);
                    break;

                case "setZoneMax":
                    setZoneBoundsFromPosition(data.index, false);
                    break;

                case "addWaveSpawn":
                    formWaveSpawnPoints.add(new ArenaConfig.SpawnPoint(0, 0, 0, 0, 0));
                    active = true;
                    rebuild();
                    break;

                case "addWaveSpawnHere":
                    addWaveSpawnFromPosition();
                    break;

                case "setWaveSpawn":
                    setWaveSpawnFromPosition(data.index);
                    break;

                case "removeWaveSpawn":
                    removeWaveSpawnAtIndex(data.index);
                    active = true;
                    rebuild();
                    break;

                case "addNavWp":
                    formNavWaypoints.add(new ArenaConfig.SpawnPoint(0, 0, 0, 0, 0));
                    active = true;
                    rebuild();
                    break;

                case "addNavWpHere":
                    addNavWpFromPosition();
                    break;

                case "setNavWp":
                    setNavWpFromPosition(data.index);
                    break;

                case "removeNavWp":
                    removeNavWpAtIndex(data.index);
                    active = true;
                    rebuild();
                    break;

                case "save":
                    handleSave();
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ArenaEditorPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFieldChange(PageEventData data) {
        if (data.field == null) return;
        if (data.value == null && data.boolValue == null && data.intValue == null) return;

        int idx = parseIndex(data.index);

        switch (data.field) {
            case "id": formId = data.value; break;
            case "displayName": formDisplayName = data.value; break;
            case "worldName": formWorldName = data.value; break;
            case "gameMode":
                formGameMode = data.value;
                active = true;
                rebuild();
                break;
            case "minPlayers": formMinPlayers = data.intValue != null ? data.intValue : formMinPlayers; break;
            case "maxPlayers": formMaxPlayers = data.intValue != null ? data.intValue : formMaxPlayers; break;
            case "waitTime": formWaitTime = data.intValue != null ? data.intValue : formWaitTime; break;
            case "duration": formDuration = data.intValue != null ? data.intValue : formDuration; break;
            case "killTarget": formKillTarget = data.intValue != null ? data.intValue : formKillTarget; break;
            case "respawnDelay": formRespawnDelay = data.intValue != null ? data.intValue : formRespawnDelay; break;
            case "scoreTarget": formScoreTarget = data.intValue != null ? data.intValue : formScoreTarget; break;
            case "zoneRotation": formZoneRotation = data.intValue != null ? data.intValue : formZoneRotation; break;
            case "botDifficulty": formBotDifficulty = data.value; break;
            case "botModel": formBotModelId = data.value; break;
            case "autoFill":
                formAutoFill = data.boolValue != null ? data.boolValue : false;
                break;
            case "swapOnKill":
                formSwapOnKill = data.boolValue != null ? data.boolValue : true;
                break;
            case "swapOnRespawn":
                formSwapOnRespawn = data.boolValue != null ? data.boolValue : false;
                break;
            case "autoFillDelay": formAutoFillDelay = data.intValue != null ? data.intValue : formAutoFillDelay; break;
            case "minRealPlayers": formMinRealPlayers = data.intValue != null ? data.intValue : formMinRealPlayers; break;
            case "waveBonusPerKill": formWaveBonusPerKill = data.intValue != null ? data.intValue : formWaveBonusPerKill; break;
            case "waveBonusPerWave": formWaveBonusPerWave = data.intValue != null ? data.intValue : formWaveBonusPerWave; break;

            // Kit lists
            case "kitValue":
                if (idx >= 0 && idx < formAllowedKits.size()) formAllowedKits.set(idx, data.value);
                break;
            case "rkitValue":
                if (idx >= 0 && idx < formRandomKitPool.size()) formRandomKitPool.set(idx, data.value);
                break;

            // Spawn point fields
            case "spawnX":
                if (idx >= 0 && idx < formSpawnPoints.size()) formSpawnPoints.get(idx).setX(parseDoubleSafe(data.value, 0));
                break;
            case "spawnY":
                if (idx >= 0 && idx < formSpawnPoints.size()) formSpawnPoints.get(idx).setY(parseDoubleSafe(data.value, 0));
                break;
            case "spawnZ":
                if (idx >= 0 && idx < formSpawnPoints.size()) formSpawnPoints.get(idx).setZ(parseDoubleSafe(data.value, 0));
                break;
            case "spawnYaw":
                if (idx >= 0 && idx < formSpawnPoints.size()) formSpawnPoints.get(idx).setYaw((float) parseDoubleSafe(data.value, 0));
                break;
            case "spawnPitch":
                if (idx >= 0 && idx < formSpawnPoints.size()) formSpawnPoints.get(idx).setPitch((float) parseDoubleSafe(data.value, 0));
                break;

            // Wave spawn point fields
            case "waveSpawnX":
                if (idx >= 0 && idx < formWaveSpawnPoints.size()) formWaveSpawnPoints.get(idx).setX(parseDoubleSafe(data.value, 0));
                break;
            case "waveSpawnY":
                if (idx >= 0 && idx < formWaveSpawnPoints.size()) formWaveSpawnPoints.get(idx).setY(parseDoubleSafe(data.value, 0));
                break;
            case "waveSpawnZ":
                if (idx >= 0 && idx < formWaveSpawnPoints.size()) formWaveSpawnPoints.get(idx).setZ(parseDoubleSafe(data.value, 0));
                break;
            case "waveSpawnYaw":
                if (idx >= 0 && idx < formWaveSpawnPoints.size()) formWaveSpawnPoints.get(idx).setYaw((float) parseDoubleSafe(data.value, 0));
                break;
            case "waveSpawnPitch":
                if (idx >= 0 && idx < formWaveSpawnPoints.size()) formWaveSpawnPoints.get(idx).setPitch((float) parseDoubleSafe(data.value, 0));
                break;

            // Nav waypoint fields
            case "navWpX":
                if (idx >= 0 && idx < formNavWaypoints.size()) formNavWaypoints.get(idx).setX(parseDoubleSafe(data.value, 0));
                break;
            case "navWpY":
                if (idx >= 0 && idx < formNavWaypoints.size()) formNavWaypoints.get(idx).setY(parseDoubleSafe(data.value, 0));
                break;
            case "navWpZ":
                if (idx >= 0 && idx < formNavWaypoints.size()) formNavWaypoints.get(idx).setZ(parseDoubleSafe(data.value, 0));
                break;
            case "navWpYaw":
                if (idx >= 0 && idx < formNavWaypoints.size()) formNavWaypoints.get(idx).setYaw((float) parseDoubleSafe(data.value, 0));
                break;
            case "navWpPitch":
                if (idx >= 0 && idx < formNavWaypoints.size()) formNavWaypoints.get(idx).setPitch((float) parseDoubleSafe(data.value, 0));
                break;

            // Bounds
            case "boundsMinX": formBoundsMin[0] = parseDoubleSafe(data.value, 0); break;
            case "boundsMinY": formBoundsMin[1] = parseDoubleSafe(data.value, 0); break;
            case "boundsMinZ": formBoundsMin[2] = parseDoubleSafe(data.value, 0); break;
            case "boundsMaxX": formBoundsMax[0] = parseDoubleSafe(data.value, 0); break;
            case "boundsMaxY": formBoundsMax[1] = parseDoubleSafe(data.value, 0); break;
            case "boundsMaxZ": formBoundsMax[2] = parseDoubleSafe(data.value, 0); break;

            // Zone fields
            case "zoneName":
                if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.get(idx).setDisplayName(data.value);
                break;
            case "zoneMinX":
                if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.get(idx).setMinX(parseDoubleSafe(data.value, 0));
                break;
            case "zoneMinY":
                if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.get(idx).setMinY(parseDoubleSafe(data.value, 0));
                break;
            case "zoneMinZ":
                if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.get(idx).setMinZ(parseDoubleSafe(data.value, 0));
                break;
            case "zoneMaxX":
                if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.get(idx).setMaxX(parseDoubleSafe(data.value, 0));
                break;
            case "zoneMaxY":
                if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.get(idx).setMaxY(parseDoubleSafe(data.value, 0));
                break;
            case "zoneMaxZ":
                if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.get(idx).setMaxZ(parseDoubleSafe(data.value, 0));
                break;
        }
    }

    private void handleSave() {
        // Validate
        if (formId == null || formId.trim().isEmpty()) {
            showStatus("ID is required", "#e74c3c");
            return;
        }
        if (formDisplayName == null || formDisplayName.trim().isEmpty()) {
            showStatus("Display name is required", "#e74c3c");
            return;
        }
        if (formWorldName == null || formWorldName.trim().isEmpty()) {
            showStatus("World name is required", "#e74c3c");
            return;
        }
        if (formMinPlayers <= 0 || formMaxPlayers <= 0 || formMinPlayers > formMaxPlayers) {
            showStatus("Invalid player count", "#e74c3c");
            return;
        }
        int requiredSpawns = "wave_defense".equals(formGameMode) ? formMinPlayers : formMaxPlayers;
        if (formSpawnPoints.size() < requiredSpawns) {
            showStatus("Need at least " + requiredSpawns + " spawn points (have " + formSpawnPoints.size() + ")", "#e74c3c");
            return;
        }

        // Check for duplicate ID on create
        if (isCreate && matchManager.getArena(formId.trim()) != null) {
            showStatus("Arena ID already exists", "#e74c3c");
            return;
        }

        // Build config
        ArenaConfig config = new ArenaConfig();
        config.setId(formId.trim());
        config.setDisplayName(formDisplayName.trim());
        config.setWorldName(formWorldName.trim());
        config.setGameMode(formGameMode);
        config.setMinPlayers(formMinPlayers);
        config.setMaxPlayers(formMaxPlayers);
        config.setWaitTimeSeconds(formWaitTime);
        config.setMatchDurationSeconds(formDuration);
        config.setKillTarget(formKillTarget);
        config.setRespawnDelaySeconds(formRespawnDelay);
        config.setScoreTarget(formScoreTarget);
        config.setZoneRotationSeconds(formZoneRotation);
        config.setBotDifficulty(formBotDifficulty);
        config.setBotModelId(formBotModelId.isEmpty() ? null : formBotModelId);
        config.setAutoFillEnabled(formAutoFill);
        config.setAutoFillDelaySeconds(formAutoFillDelay);
        config.setMinRealPlayers(formMinRealPlayers);

        // Filter empty entries from lists
        List<String> kits = new ArrayList<>();
        for (String k : formAllowedKits) {
            if (k != null && !k.trim().isEmpty()) kits.add(k.trim());
        }
        config.setAllowedKits(kits);

        List<String> randomKits = new ArrayList<>();
        for (String k : formRandomKitPool) {
            if (k != null && !k.trim().isEmpty()) randomKits.add(k.trim());
        }
        config.setRandomKitPool(randomKits);
        config.setKitRouletteSwapOnKill(formSwapOnKill);
        config.setKitRouletteSwapOnRespawn(formSwapOnRespawn);

        config.setSpawnPoints(new ArrayList<>(formSpawnPoints));
        config.setBounds(new ArenaConfig.Bounds(
            formBoundsMin[0], formBoundsMin[1], formBoundsMin[2],
            formBoundsMax[0], formBoundsMax[1], formBoundsMax[2]
        ));
        config.setCaptureZones(new ArrayList<>(formCaptureZones));
        config.setWaveSpawnPoints(formWaveSpawnPoints.isEmpty() ? null : new ArrayList<>(formWaveSpawnPoints));
        config.setNavWaypoints(formNavWaypoints.isEmpty() ? null : new ArrayList<>(formNavWaypoints));
        config.setWaveBonusSecondsPerKill(formWaveBonusPerKill);
        config.setWaveBonusSecondsPerWaveClear(formWaveBonusPerWave);

        if (matchManager.saveArena(config)) {
            HyArena2.getInstance().triggerWebSync();
            shutdown();
            if (onBack != null) onBack.run();
        } else {
            showStatus("Failed to save arena", "#e74c3c");
        }
    }

    // ===== Helpers =====

    private String formatDifficulty(String difficulty) {
        if (difficulty == null) return "Medium";
        switch (difficulty) {
            case "EASY": return "Easy";
            case "HARD": return "Hard";
            case "EXTREME": return "Extreme";
            case "EASY_TANK": return "Easy Tank";
            case "MEDIUM_TANK": return "Medium Tank";
            case "HARD_TANK": return "Hard Tank";
            case "EXTREME_TANK": return "Extreme Tank";
            default: return "Medium";
        }
    }

    // ===== Position capture helpers =====

    private double[] getAdminPosition() {
        try {
            PlayerRef pRef = Universe.get().getPlayer(playerUuid);
            if (pRef == null) return null;
            Ref<EntityStore> ref = pRef.getReference();
            if (ref == null) return null;
            Store<EntityStore> store = ref.getStore();
            if (store == null) return null;

            TransformComponent transform = store.getComponent(ref,
                EntityModule.get().getTransformComponentType());
            if (transform == null) return null;

            Vector3d pos = transform.getPosition();
            Vector3f rot = transform.getRotation();
            return new double[]{pos.getX(), pos.getY(), pos.getZ(), rot.getY(), rot.getX()};
        } catch (Exception e) {
            return null;
        }
    }

    private void addSpawnFromPosition() {
        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }
        formSpawnPoints.add(new ArenaConfig.SpawnPoint(pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]));
        active = true;
        rebuild();
    }

    private void setSpawnFromPosition(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx < 0 || idx >= formSpawnPoints.size()) return;

        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }

        ArenaConfig.SpawnPoint sp = formSpawnPoints.get(idx);
        sp.setX(pos[0]);
        sp.setY(pos[1]);
        sp.setZ(pos[2]);
        sp.setYaw((float) pos[3]);
        sp.setPitch((float) pos[4]);
        active = true;
        rebuild();
    }

    private void setBoundsFromPosition(boolean isMin) {
        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }

        if (isMin) {
            formBoundsMin[0] = pos[0];
            formBoundsMin[1] = pos[1];
            formBoundsMin[2] = pos[2];
        } else {
            formBoundsMax[0] = pos[0];
            formBoundsMax[1] = pos[1];
            formBoundsMax[2] = pos[2];
        }
        active = true;
        rebuild();
    }

    private void setZoneBoundsFromPosition(String indexStr, boolean isMin) {
        int idx = parseIndex(indexStr);
        if (idx < 0 || idx >= formCaptureZones.size()) return;

        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }

        ArenaConfig.CaptureZone zone = formCaptureZones.get(idx);
        if (isMin) {
            zone.setMinX(pos[0]);
            zone.setMinY(pos[1]);
            zone.setMinZ(pos[2]);
        } else {
            zone.setMaxX(pos[0]);
            zone.setMaxY(pos[1]);
            zone.setMaxZ(pos[2]);
        }
        active = true;
        rebuild();
    }

    private void removeAtIndex(List<String> list, String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx >= 0 && idx < list.size()) list.remove(idx);
    }

    private void removeSpawnAtIndex(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx >= 0 && idx < formSpawnPoints.size()) formSpawnPoints.remove(idx);
    }

    private void removeZoneAtIndex(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx >= 0 && idx < formCaptureZones.size()) formCaptureZones.remove(idx);
    }

    private void addWaveSpawnFromPosition() {
        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }
        formWaveSpawnPoints.add(new ArenaConfig.SpawnPoint(pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]));
        active = true;
        rebuild();
    }

    private void setWaveSpawnFromPosition(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx < 0 || idx >= formWaveSpawnPoints.size()) return;

        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }

        ArenaConfig.SpawnPoint sp = formWaveSpawnPoints.get(idx);
        sp.setX(pos[0]);
        sp.setY(pos[1]);
        sp.setZ(pos[2]);
        sp.setYaw((float) pos[3]);
        sp.setPitch((float) pos[4]);
        active = true;
        rebuild();
    }

    private void removeWaveSpawnAtIndex(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx >= 0 && idx < formWaveSpawnPoints.size()) formWaveSpawnPoints.remove(idx);
    }

    private void addNavWpFromPosition() {
        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }
        formNavWaypoints.add(new ArenaConfig.SpawnPoint(pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]));
        active = true;
        rebuild();
    }

    private void setNavWpFromPosition(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx < 0 || idx >= formNavWaypoints.size()) return;

        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }

        ArenaConfig.SpawnPoint sp = formNavWaypoints.get(idx);
        sp.setX(pos[0]);
        sp.setY(pos[1]);
        sp.setZ(pos[2]);
        sp.setYaw((float) pos[3]);
        sp.setPitch((float) pos[4]);
        active = true;
        rebuild();
    }

    private void removeNavWpAtIndex(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx >= 0 && idx < formNavWaypoints.size()) formNavWaypoints.remove(idx);
    }

    private int parseIndex(String s) {
        if (s == null) return -1;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }

    private int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
    }

    private double parseDoubleSafe(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }

    private String formatCoord(double val) {
        return String.format("%.2f", val);
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#StatusMessage.Text", message);
        cmd.set("#StatusMessage.Visible", true);
        cmd.set("#StatusMessage.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void safeSendUpdate(UICommandBuilder cmd) {
        if (!active) return;
        try { sendUpdate(cmd, false); } catch (Exception e) { active = false; }
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
        public String field;
        public String value;
        public String index;
        public Boolean boolValue;
        public Integer intValue;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Field", Codec.STRING),
                    (d, v) -> d.field = v, d -> d.field).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING),
                    (d, v) -> d.value = v, d -> d.value).add()
                .append(new KeyedCodec<>("Index", Codec.STRING),
                    (d, v) -> d.index = v, d -> d.index).add()
                .append(new KeyedCodec<>("@BoolValue", Codec.BOOLEAN),
                    (d, v) -> d.boolValue = v, d -> d.boolValue).add()
                .append(new KeyedCodec<>("@IntValue", Codec.INTEGER),
                    (d, v) -> d.intValue = v, d -> d.intValue).add()
                .build();
    }
}
