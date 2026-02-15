package de.ragesith.hyarena2.ui.page.admin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import de.ragesith.hyarena2.HyArena2;
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
import de.ragesith.hyarena2.config.ConfigManager;
import de.ragesith.hyarena2.hub.HubManager;
import de.ragesith.hyarena2.kit.KitConfig;
import de.ragesith.hyarena2.kit.KitManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Kit editor page for creating and editing kits.
 */
public class KitEditorPage extends InteractiveCustomUIPage<KitEditorPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final boolean isCreate;
    private final KitManager kitManager;
    private final MatchManager matchManager;
    private final HubManager hubManager;
    private final ConfigManager configManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;

    // Form state
    private String formId;
    private String formDisplayName;
    private String formDescription;
    private String formPermission;
    private List<String> formItems;
    private String formHelmet;
    private String formChest;
    private String formHands;
    private String formLegs;
    private String formOffhand;

    public KitEditorPage(PlayerRef playerRef, UUID playerUuid,
                         KitConfig existingKit,
                         KitManager kitManager, MatchManager matchManager,
                         HubManager hubManager, ConfigManager configManager,
                         HudManager hudManager, ScheduledExecutorService scheduler,
                         Runnable onBack) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.isCreate = (existingKit == null);
        this.kitManager = kitManager;
        this.matchManager = matchManager;
        this.hubManager = hubManager;
        this.configManager = configManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;

        if (existingKit != null) {
            formId = existingKit.getId();
            formDisplayName = existingKit.getDisplayName();
            formDescription = existingKit.getDescription() != null ? existingKit.getDescription() : "";
            formPermission = existingKit.getPermission() != null ? existingKit.getPermission() : "";
            formItems = new ArrayList<>(existingKit.getItems());
            Map<String, String> armor = existingKit.getArmor();
            formHelmet = armor.getOrDefault("helmet", "");
            formChest = armor.getOrDefault("chest", "");
            formHands = armor.getOrDefault("hands", "");
            formLegs = armor.getOrDefault("legs", "");
            formOffhand = existingKit.getOffhand() != null ? existingKit.getOffhand() : "";
        } else {
            formId = "";
            formDisplayName = "";
            formDescription = "";
            formPermission = "";
            formItems = new ArrayList<>();
            formHelmet = "";
            formChest = "";
            formHands = "";
            formLegs = "";
            formOffhand = "";
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/KitEditorPage.ui");

        cmd.set("#TitleLabel.Text", isCreate ? "Create Kit" : "Edit Kit: " + formId);

        // Basic info
        cmd.set("#IdField.Value", formId);
        cmd.set("#DisplayNameField.Value", formDisplayName);
        cmd.set("#DescriptionField.Value", formDescription);
        cmd.set("#PermissionField.Value", formPermission);

        // Items list
        for (int i = 0; i < formItems.size(); i++) {
            cmd.append("#ItemsList", "Pages/AdminStringRow.ui");
            String row = "#ItemsList[" + i + "]";
            cmd.set(row + " #RowTextField.Value", formItems.get(i));

            events.addEventBinding(CustomUIEventBindingType.ValueChanged, row + " #RowTextField",
                EventData.of("Action", "field").append("Field", "item").append("Index", String.valueOf(i))
                    .append("@Value", row + " #RowTextField.Value"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #RowRemoveBtn",
                EventData.of("Action", "removeItem").append("Index", String.valueOf(i)), false);
        }

        // Armor
        cmd.set("#HelmetField.Value", formHelmet);
        cmd.set("#ChestField.Value", formChest);
        cmd.set("#HandsField.Value", formHands);
        cmd.set("#LegsField.Value", formLegs);
        cmd.set("#OffhandField.Value", formOffhand);

        // Bind text field events
        bindTextField(events, "#IdField", "id");
        bindTextField(events, "#DisplayNameField", "displayName");
        bindTextField(events, "#DescriptionField", "description");
        bindTextField(events, "#PermissionField", "permission");
        bindTextField(events, "#HelmetField", "helmet");
        bindTextField(events, "#ChestField", "chest");
        bindTextField(events, "#HandsField", "hands");
        bindTextField(events, "#LegsField", "legs");
        bindTextField(events, "#OffhandField", "offhand");

        // Action buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddItemBtn",
            EventData.of("Action", "addItem"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
            EventData.of("Action", "save"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
            EventData.of("Action", "cancel"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"), false);

        hudManager.registerPage(playerUuid, this);
    }

    private void bindTextField(UIEventBuilder events, String elementId, String field) {
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

                case "addItem":
                    formItems.add("");
                    active = true;
                    rebuild();
                    break;

                case "removeItem":
                    removeItem(data.index);
                    active = true;
                    rebuild();
                    break;

                case "save":
                    handleSave();
                    break;
            }
        } catch (Exception e) {
            System.err.println("[KitEditorPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFieldChange(PageEventData data) {
        if (data.field == null || data.value == null) return;

        switch (data.field) {
            case "id": formId = data.value; break;
            case "displayName": formDisplayName = data.value; break;
            case "description": formDescription = data.value; break;
            case "permission": formPermission = data.value; break;
            case "helmet": formHelmet = data.value; break;
            case "chest": formChest = data.value; break;
            case "hands": formHands = data.value; break;
            case "legs": formLegs = data.value; break;
            case "offhand": formOffhand = data.value; break;
            case "item":
                int idx = parseIndex(data.index);
                if (idx >= 0 && idx < formItems.size()) formItems.set(idx, data.value);
                break;
        }
    }

    private void handleSave() {
        if (formId == null || formId.trim().isEmpty()) {
            showStatus("ID is required", "#e74c3c");
            return;
        }
        if (formDisplayName == null || formDisplayName.trim().isEmpty()) {
            showStatus("Display name is required", "#e74c3c");
            return;
        }

        if (isCreate && kitManager.kitExists(formId.trim())) {
            showStatus("Kit ID already exists", "#e74c3c");
            return;
        }

        KitConfig config = new KitConfig(formId.trim(), formDisplayName.trim());
        config.setDescription(formDescription.trim());
        config.setPermission(formPermission.trim().isEmpty() ? null : formPermission.trim());

        // Filter empty items
        List<String> items = new ArrayList<>();
        for (String item : formItems) {
            if (item != null && !item.trim().isEmpty()) items.add(item.trim());
        }
        config.setItems(items);

        // Armor
        Map<String, String> armor = new HashMap<>();
        if (!formHelmet.trim().isEmpty()) armor.put("helmet", formHelmet.trim());
        if (!formChest.trim().isEmpty()) armor.put("chest", formChest.trim());
        if (!formHands.trim().isEmpty()) armor.put("hands", formHands.trim());
        if (!formLegs.trim().isEmpty()) armor.put("legs", formLegs.trim());
        config.setArmor(armor);

        config.setOffhand(formOffhand.trim().isEmpty() ? null : formOffhand.trim());

        if (kitManager.saveKit(config)) {
            HyArena2.getInstance().triggerWebSync();
            shutdown();
            if (onBack != null) onBack.run();
        } else {
            showStatus("Failed to save kit", "#e74c3c");
        }
    }

    private void removeItem(String indexStr) {
        int idx = parseIndex(indexStr);
        if (idx >= 0 && idx < formItems.size()) formItems.remove(idx);
    }

    private int parseIndex(String s) {
        if (s == null) return -1;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
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
                .build();
    }
}
