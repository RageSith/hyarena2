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
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Editor page for creating or editing a single hub hologram entry.
 */
public class HologramEditorPage extends InteractiveCustomUIPage<HologramEditorPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final int editIndex; // -1 = create new
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;

    // Form state
    private String formText;
    private double formX, formY, formZ;

    public HologramEditorPage(PlayerRef playerRef, UUID playerUuid,
                              HubConfig.HologramEntry entry, int editIndex,
                              HubManager hubManager, ConfigManager configManager,
                              HudManager hudManager, ScheduledExecutorService scheduler,
                              Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.editIndex = editIndex;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;

        if (entry != null) {
            formText = entry.getText() != null ? entry.getText() : "";
            formX = entry.getX();
            formY = entry.getY();
            formZ = entry.getZ();
        } else {
            formText = "";
            formX = 0;
            formY = 100;
            formZ = 0;
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/HologramEditorPage.ui");

        cmd.set("#TitleLabel.Text", editIndex >= 0 ? "Edit Hologram" : "Create Hologram");

        cmd.set("#TextField.Value", formText);
        cmd.set("#XField.Value", fmt(formX));
        cmd.set("#YField.Value", fmt(formY));
        cmd.set("#ZField.Value", fmt(formZ));

        // Bind text fields
        bindField(events, "#TextField", "text");
        bindField(events, "#XField", "x");
        bindField(events, "#YField", "y");
        bindField(events, "#ZField", "z");

        // Buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetPosBtn",
            EventData.of("Action", "setPos"), false);
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

                case "setPos":
                    setFromPosition();
                    break;

                case "save":
                    handleSave();
                    break;
            }
        } catch (Exception e) {
            System.err.println("[HologramEditorPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFieldChange(PageEventData data) {
        if (data.field == null || data.value == null) return;

        switch (data.field) {
            case "text": formText = data.value; break;
            case "x": formX = parseDbl(data.value, formX); break;
            case "y": formY = parseDbl(data.value, formY); break;
            case "z": formZ = parseDbl(data.value, formZ); break;
        }
    }

    private void setFromPosition() {
        try {
            PlayerRef pRef = Universe.get().getPlayer(playerUuid);
            if (pRef == null) { showStatus("Could not read position", "#e74c3c"); return; }
            Ref<EntityStore> ref = pRef.getReference();
            if (ref == null) { showStatus("Could not read position", "#e74c3c"); return; }
            Store<EntityStore> store = ref.getStore();
            if (store == null) { showStatus("Could not read position", "#e74c3c"); return; }

            TransformComponent transform = store.getComponent(ref,
                EntityModule.get().getTransformComponentType());
            if (transform == null) { showStatus("Could not read position", "#e74c3c"); return; }

            Vector3d pos = transform.getPosition();
            formX = pos.getX();
            formY = pos.getY();
            formZ = pos.getZ();

            active = true;
            rebuild();
        } catch (Exception e) {
            showStatus("Could not read position", "#e74c3c");
        }
    }

    private void handleSave() {
        if (formText == null || formText.trim().isEmpty()) {
            showStatus("Text is required", "#e74c3c");
            return;
        }

        List<HubConfig.HologramEntry> holograms = hubManager.getConfig().getHolograms();

        if (editIndex >= 0 && editIndex < holograms.size()) {
            // Update existing
            HubConfig.HologramEntry entry = holograms.get(editIndex);
            entry.setText(formText.trim());
            entry.setX(formX);
            entry.setY(formY);
            entry.setZ(formZ);
        } else {
            // Create new
            holograms.add(new HubConfig.HologramEntry(formX, formY, formZ, formText.trim()));
        }

        // Save to disk and respawn
        configManager.saveConfig("hub.json", hubManager.getConfig());
        hubManager.respawnHubHolograms();

        // Navigate back to list (no status message — page is replaced immediately)
        shutdown();
        if (onBack != null) onBack.run();
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

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Field", Codec.STRING),
                    (d, v) -> d.field = v, d -> d.field).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING),
                    (d, v) -> d.value = v, d -> d.value).add()
                .build();
    }
}
