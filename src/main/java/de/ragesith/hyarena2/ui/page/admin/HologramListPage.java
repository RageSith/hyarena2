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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.config.HubConfig;
import de.ragesith.hyarena2.config.Position;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Admin page listing all hub holograms with Set Pos/TP/Edit/Delete buttons.
 */
public class HologramListPage extends InteractiveCustomUIPage<HologramListPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;
    private List<HubConfig.HologramEntry> holoList;
    private int pendingDeleteIndex = -1;

    public HologramListPage(PlayerRef playerRef, UUID playerUuid,
                            HubManager hubManager, ConfigManager configManager,
                            HudManager hudManager, ScheduledExecutorService scheduler,
                            Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/HologramListPage.ui");

        holoList = hubManager.getConfig().getHolograms();
        cmd.set("#HoloCountLabel.Text", holoList.size() + " hologram" + (holoList.size() != 1 ? "s" : ""));

        for (int i = 0; i < holoList.size(); i++) {
            HubConfig.HologramEntry entry = holoList.get(i);

            cmd.append("#HoloList", "Pages/AdminHologramRow.ui");
            String row = "#HoloList[" + i + "]";

            String displayText = entry.getText() != null ? entry.getText() : "(empty)";
            cmd.set(row + " #RowHoloText.Text", displayText);
            cmd.set(row + " #RowHoloPos.Text",
                String.format("%.1f, %.1f, %.1f", entry.getX(), entry.getY(), entry.getZ()));

            String idx = String.valueOf(i);

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowSetPosBtn",
                EventData.of("Action", "setpos").append("Index", idx),
                false
            );

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowTpBtn",
                EventData.of("Action", "teleport").append("Index", idx),
                false
            );

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowEditBtn",
                EventData.of("Action", "edit").append("Index", idx),
                false
            );

            if (i == pendingDeleteIndex) {
                cmd.set(row + " #RowDeleteBtn.Text", "Confirm?");
            }

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #RowDeleteBtn",
                EventData.of("Action", "delete").append("Index", idx),
                false
            );
        }

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
            "#CloseButton",
            EventData.of("Action", "close"),
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

            int index = parseIndex(data.index);

            switch (data.action) {
                case "close":
                    player.getPageManager().setPage(ref, store, Page.None);
                    break;

                case "back":
                    shutdown();
                    if (onBack != null) onBack.run();
                    break;

                case "create":
                    openEditor(ref, store, player, -1);
                    break;

                case "edit":
                    if (index >= 0 && index < holoList.size()) {
                        openEditor(ref, store, player, index);
                    }
                    break;

                case "setpos":
                    if (index >= 0 && index < holoList.size()) {
                        handleSetPos(ref, store, player, index);
                    }
                    break;

                case "teleport":
                    if (index >= 0 && index < holoList.size()) {
                        handleTeleport(ref, store, player, index);
                    }
                    break;

                case "delete":
                    if (index >= 0 && index < holoList.size()) {
                        handleDelete(index);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[HologramListPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSetPos(Ref<EntityStore> ref, Store<EntityStore> store, Player player, int index) {
        TransformComponent transform = store.getComponent(ref,
            EntityModule.get().getTransformComponentType());
        if (transform == null) {
            showStatus("Could not read position", "#e74c3c");
            return;
        }

        Vector3d pos = transform.getPosition();
        HubConfig.HologramEntry entry = holoList.get(index);
        entry.setX(pos.getX());
        entry.setY(pos.getY());
        entry.setZ(pos.getZ());

        saveAndRespawn();
        showStatus("Position set! Holograms respawned.", "#2ecc71");

        // Refresh the row position text
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#HoloList[" + index + "] #RowHoloPos.Text",
            String.format("%.1f, %.1f, %.1f", pos.getX(), pos.getY(), pos.getZ()));
        safeSendUpdate(cmd);
    }

    private void handleTeleport(Ref<EntityStore> ref, Store<EntityStore> store, Player player, int index) {
        HubConfig.HologramEntry entry = holoList.get(index);
        World hubWorld = hubManager.getHubWorld();
        if (hubWorld == null) {
            showStatus("Hub world not found", "#e74c3c");
            return;
        }

        Position pos = new Position(entry.getX(), entry.getY(), entry.getZ(), 0, 0);
        player.getPageManager().setPage(ref, store, Page.None);
        shutdown();
        hubManager.teleportPlayerToWorld(player, pos, hubWorld);
    }

    private void openEditor(Ref<EntityStore> ref, Store<EntityStore> store, Player player, int index) {
        shutdown();
        HubConfig.HologramEntry entry = (index >= 0 && index < holoList.size())
            ? holoList.get(index) : null;
        HologramEditorPage editorPage = new HologramEditorPage(
            playerRef, playerUuid, entry, index,
            hubManager, configManager, hudManager, scheduler,
            this::reopenSelf
        );
        player.getPageManager().openCustomPage(ref, store, editorPage);
    }

    private void handleDelete(int index) {
        if (index != pendingDeleteIndex) {
            pendingDeleteIndex = index;
            UICommandBuilder cmd = new UICommandBuilder();
            cmd.set("#HoloList[" + index + "] #RowDeleteBtn.Text", "Confirm?");
            safeSendUpdate(cmd);
            return;
        }

        // Confirmed delete
        holoList.remove(index);
        pendingDeleteIndex = -1;
        saveAndRespawn();
        reopenSelf();
    }

    private void saveAndRespawn() {
        configManager.saveConfig("hub.json", hubManager.getConfig());
        hubManager.respawnHubHolograms();
    }

    private void showStatus(String message, String color) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#HoloCountLabel.Text", message);
        cmd.set("#HoloCountLabel.Style.TextColor", color);
        safeSendUpdate(cmd);
    }

    private void reopenSelf() {
        Ref<EntityStore> pRef = playerRef.getReference();
        if (pRef == null) return;
        Store<EntityStore> pStore = pRef.getStore();
        Player p = pStore.getComponent(pRef, Player.getComponentType());
        if (p == null) return;

        HologramListPage page = new HologramListPage(
            playerRef, playerUuid, hubManager, configManager,
            hudManager, scheduler, onBack
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

    private int parseIndex(String s) {
        if (s == null) return -1;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
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
        public String index;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Index", Codec.STRING),
                    (d, v) -> d.index = v, d -> d.index).add()
                .build();
    }
}
