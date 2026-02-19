package de.ragesith.hyarena2.ui.page.admin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.config.BoundingBox;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Hub settings editor page.
 */
public class HubSettingsPage extends InteractiveCustomUIPage<HubSettingsPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;

    // Form state
    private String formWorldName;
    private double formSpawnX, formSpawnY, formSpawnZ;
    private float formSpawnYaw, formSpawnPitch;
    private double formSpawnZoneMinX, formSpawnZoneMinZ;
    private double formSpawnZoneMaxX, formSpawnZoneMaxZ;
    private double formSpawnZoneY;
    private boolean hasSpawnZone;
    private double formBoundsMinX, formBoundsMinY, formBoundsMinZ;
    private double formBoundsMaxX, formBoundsMaxY, formBoundsMaxZ;

    public HubSettingsPage(PlayerRef playerRef, UUID playerUuid,
                           HubManager hubManager, ConfigManager configManager,
                           HudManager hudManager, ScheduledExecutorService scheduler,
                           Runnable onBack) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;

        // Load current hub config
        HubConfig config = hubManager.getConfig();
        formWorldName = config.getWorldName() != null ? config.getWorldName() : "default";

        Position spawn = config.getSpawnPoint();
        if (spawn != null) {
            formSpawnX = spawn.getX();
            formSpawnY = spawn.getY();
            formSpawnZ = spawn.getZ();
            formSpawnYaw = spawn.getYaw();
            formSpawnPitch = spawn.getPitch();
        }

        BoundingBox spawnZone = config.getSpawnZone();
        if (spawnZone != null) {
            hasSpawnZone = true;
            formSpawnZoneMinX = spawnZone.getMinX();
            formSpawnZoneMinZ = spawnZone.getMinZ();
            formSpawnZoneMaxX = spawnZone.getMaxX();
            formSpawnZoneMaxZ = spawnZone.getMaxZ();
            formSpawnZoneY = spawnZone.getMinY();
        }

        BoundingBox bounds = config.getBounds();
        if (bounds != null) {
            formBoundsMinX = bounds.getMinX();
            formBoundsMinY = bounds.getMinY();
            formBoundsMinZ = bounds.getMinZ();
            formBoundsMaxX = bounds.getMaxX();
            formBoundsMaxY = bounds.getMaxY();
            formBoundsMaxZ = bounds.getMaxZ();
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/HubSettingsPage.ui");

        cmd.set("#WorldNameField.Value", formWorldName);
        cmd.set("#SpawnXField.Value", fmt(formSpawnX));
        cmd.set("#SpawnYField.Value", fmt(formSpawnY));
        cmd.set("#SpawnZField.Value", fmt(formSpawnZ));
        cmd.set("#SpawnYawField.Value", fmt(formSpawnYaw));
        cmd.set("#SpawnPitchField.Value", fmt(formSpawnPitch));
        cmd.set("#SpawnZoneEnabled.Value", hasSpawnZone);
        cmd.set("#SpawnZoneMinXField.Value", fmt(formSpawnZoneMinX));
        cmd.set("#SpawnZoneMinZField.Value", fmt(formSpawnZoneMinZ));
        cmd.set("#SpawnZoneMaxXField.Value", fmt(formSpawnZoneMaxX));
        cmd.set("#SpawnZoneMaxZField.Value", fmt(formSpawnZoneMaxZ));
        cmd.set("#SpawnZoneYField.Value", fmt(formSpawnZoneY));
        cmd.set("#SpawnZoneSection.Visible", hasSpawnZone);
        cmd.set("#BoundsMinXField.Value", fmt(formBoundsMinX));
        cmd.set("#BoundsMinYField.Value", fmt(formBoundsMinY));
        cmd.set("#BoundsMinZField.Value", fmt(formBoundsMinZ));
        cmd.set("#BoundsMaxXField.Value", fmt(formBoundsMaxX));
        cmd.set("#BoundsMaxYField.Value", fmt(formBoundsMaxY));
        cmd.set("#BoundsMaxZField.Value", fmt(formBoundsMaxZ));

        // Bind text fields
        bindField(events, "#WorldNameField", "worldName");
        bindField(events, "#SpawnXField", "spawnX");
        bindField(events, "#SpawnYField", "spawnY");
        bindField(events, "#SpawnZField", "spawnZ");
        bindField(events, "#SpawnYawField", "spawnYaw");
        bindField(events, "#SpawnPitchField", "spawnPitch");
        bindField(events, "#SpawnZoneMinXField", "szMinX");
        bindField(events, "#SpawnZoneMinZField", "szMinZ");
        bindField(events, "#SpawnZoneMaxXField", "szMaxX");
        bindField(events, "#SpawnZoneMaxZField", "szMaxZ");
        bindField(events, "#SpawnZoneYField", "szY");
        bindField(events, "#BoundsMinXField", "boundsMinX");
        bindField(events, "#BoundsMinYField", "boundsMinY");
        bindField(events, "#BoundsMinZField", "boundsMinZ");
        bindField(events, "#BoundsMaxXField", "boundsMaxX");
        bindField(events, "#BoundsMaxYField", "boundsMaxY");
        bindField(events, "#BoundsMaxZField", "boundsMaxZ");

        // Buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetSpawnBtn",
            EventData.of("Action", "setSpawn"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SpawnZoneEnabled",
            EventData.of("Action", "toggleSpawnZone").append("@BoolValue", "#SpawnZoneEnabled.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetSZMinBtn",
            EventData.of("Action", "setSZMin"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetSZMaxBtn",
            EventData.of("Action", "setSZMax"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetMinBtn",
            EventData.of("Action", "setMin"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetMaxBtn",
            EventData.of("Action", "setMax"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
            EventData.of("Action", "save"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
            EventData.of("Action", "cancel"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"), false);

        hudManager.registerPage(playerUuid, this);
    }

    private void bindField(UIEventBuilder events, String elementId, String field) {
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, elementId,
            EventData.of("Action", "field").append("Field", field).append("@Value", elementId + ".Value"), false);
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

                case "setSpawn":
                    setSpawnFromPosition();
                    break;

                case "toggleSpawnZone":
                    handleToggleSpawnZone(data);
                    break;

                case "setSZMin":
                    setSpawnZoneFromPosition(true);
                    break;

                case "setSZMax":
                    setSpawnZoneFromPosition(false);
                    break;

                case "setMin":
                    setBoundsFromPosition(true);
                    break;

                case "setMax":
                    setBoundsFromPosition(false);
                    break;

                case "save":
                    handleSave();
                    break;
            }
        } catch (Exception e) {
            System.err.println("[HubSettingsPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFieldChange(PageEventData data) {
        if (data.field == null || data.value == null) return;

        switch (data.field) {
            case "worldName": formWorldName = data.value; break;
            case "spawnX": formSpawnX = parseDbl(data.value, formSpawnX); break;
            case "spawnY": formSpawnY = parseDbl(data.value, formSpawnY); break;
            case "spawnZ": formSpawnZ = parseDbl(data.value, formSpawnZ); break;
            case "spawnYaw": formSpawnYaw = (float) parseDbl(data.value, formSpawnYaw); break;
            case "spawnPitch": formSpawnPitch = (float) parseDbl(data.value, formSpawnPitch); break;
            case "szMinX": formSpawnZoneMinX = parseDbl(data.value, formSpawnZoneMinX); break;
            case "szMinZ": formSpawnZoneMinZ = parseDbl(data.value, formSpawnZoneMinZ); break;
            case "szMaxX": formSpawnZoneMaxX = parseDbl(data.value, formSpawnZoneMaxX); break;
            case "szMaxZ": formSpawnZoneMaxZ = parseDbl(data.value, formSpawnZoneMaxZ); break;
            case "szY": formSpawnZoneY = parseDbl(data.value, formSpawnZoneY); break;
            case "boundsMinX": formBoundsMinX = parseDbl(data.value, formBoundsMinX); break;
            case "boundsMinY": formBoundsMinY = parseDbl(data.value, formBoundsMinY); break;
            case "boundsMinZ": formBoundsMinZ = parseDbl(data.value, formBoundsMinZ); break;
            case "boundsMaxX": formBoundsMaxX = parseDbl(data.value, formBoundsMaxX); break;
            case "boundsMaxY": formBoundsMaxY = parseDbl(data.value, formBoundsMaxY); break;
            case "boundsMaxZ": formBoundsMaxZ = parseDbl(data.value, formBoundsMaxZ); break;
        }
    }

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

    private void setSpawnFromPosition() {
        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }
        formSpawnX = pos[0];
        formSpawnY = pos[1];
        formSpawnZ = pos[2];
        formSpawnYaw = (float) pos[3];
        formSpawnPitch = (float) pos[4];
        active = true;
        rebuild();
    }

    private void handleToggleSpawnZone(PageEventData data) {
        hasSpawnZone = data.boolValue != null && data.boolValue;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#SpawnZoneSection.Visible", hasSpawnZone);
        safeSendUpdate(cmd);
    }

    private void setSpawnZoneFromPosition(boolean isMin) {
        double[] pos = getAdminPosition();
        if (pos == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }

        if (isMin) {
            formSpawnZoneMinX = pos[0];
            formSpawnZoneMinZ = pos[2];
        } else {
            formSpawnZoneMaxX = pos[0];
            formSpawnZoneMaxZ = pos[2];
        }
        formSpawnZoneY = pos[1];
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
            formBoundsMinX = pos[0];
            formBoundsMinY = pos[1];
            formBoundsMinZ = pos[2];
        } else {
            formBoundsMaxX = pos[0];
            formBoundsMaxY = pos[1];
            formBoundsMaxZ = pos[2];
        }
        active = true;
        rebuild();
    }

    private void handleSave() {
        if (formWorldName == null || formWorldName.trim().isEmpty()) {
            showStatus("World name is required", "#e74c3c");
            return;
        }

        // Build new HubConfig
        HubConfig newConfig = new HubConfig();
        newConfig.setWorldName(formWorldName.trim());
        newConfig.setSpawnPoint(new Position(formSpawnX, formSpawnY, formSpawnZ, formSpawnYaw, formSpawnPitch));
        if (hasSpawnZone) {
            newConfig.setSpawnZone(new BoundingBox(
                formSpawnZoneMinX, formSpawnZoneY, formSpawnZoneMinZ,
                formSpawnZoneMaxX, formSpawnZoneY, formSpawnZoneMaxZ
            ));
        }
        newConfig.setBounds(new BoundingBox(
            formBoundsMinX, formBoundsMinY, formBoundsMinZ,
            formBoundsMaxX, formBoundsMaxY, formBoundsMaxZ
        ));
        newConfig.setHolograms(hubManager.getConfig().getHolograms());

        // Save to disk
        configManager.saveConfig("hub", newConfig);

        // Update runtime
        hubManager.updateConfig(newConfig);

        showStatus("Hub settings saved!", "#2ecc71");
    }

    private double parseDbl(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }

    private String fmt(double val) {
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
        public Boolean boolValue;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Field", Codec.STRING),
                    (d, v) -> d.field = v, d -> d.field).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING),
                    (d, v) -> d.value = v, d -> d.value).add()
                .append(new KeyedCodec<>("@BoolValue", Codec.BOOLEAN),
                    (d, v) -> d.boolValue = v, d -> d.boolValue).add()
                .build();
    }
}
