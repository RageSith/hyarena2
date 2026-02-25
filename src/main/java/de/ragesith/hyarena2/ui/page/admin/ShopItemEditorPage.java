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
import de.ragesith.hyarena2.shop.ShopCategory;
import de.ragesith.hyarena2.shop.ShopItem;
import de.ragesith.hyarena2.shop.ShopManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Editor page for creating and editing shop items.
 */
public class ShopItemEditorPage extends InteractiveCustomUIPage<ShopItemEditorPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final boolean isCreate;
    private final ShopManager shopManager;
    private final HudManager hudManager;
    private final ScheduledExecutorService scheduler;
    private final Runnable onBack;

    private volatile boolean active = true;

    // Form state
    private String formId;
    private String formDisplayName;
    private String formDescription;
    private int formCost;
    private String formPermission;
    private String formGroup;
    private boolean formOneTime;
    private String formCategoryId;
    private int formSort;

    public ShopItemEditorPage(PlayerRef playerRef, UUID playerUuid,
                              ShopItem existingItem, ShopManager shopManager,
                              HudManager hudManager, ScheduledExecutorService scheduler,
                              Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.isCreate = (existingItem == null);
        this.shopManager = shopManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;

        if (existingItem != null) {
            formId = existingItem.getId();
            formDisplayName = existingItem.getDisplayName();
            formDescription = existingItem.getDescription() != null ? existingItem.getDescription() : "";
            formCost = existingItem.getCost();
            formPermission = existingItem.getPermissionGranted() != null ? existingItem.getPermissionGranted() : "";
            formGroup = existingItem.getGroupGranted() != null ? existingItem.getGroupGranted() : "";
            formOneTime = existingItem.isOneTimePurchase();
            formSort = existingItem.getSort();

            String catId = shopManager.getCategoryForItem(existingItem.getId());
            formCategoryId = catId != null ? catId : "";
        } else {
            formId = "";
            formDisplayName = "";
            formDescription = "";
            formCost = 0;
            formPermission = "";
            formGroup = "";
            formOneTime = true;
            formCategoryId = "";
            formSort = 0;
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ShopItemEditorPage.ui");

        cmd.set("#TitleLabel.Text", isCreate ? "Create Shop Item" : "Edit Item: " + formId);

        // Basic info
        cmd.set("#IdField.Value", formId);
        cmd.set("#DisplayNameField.Value", formDisplayName);
        cmd.set("#DescriptionField.Value", formDescription);
        cmd.set("#CostField.Value", formCost);

        // Permissions
        cmd.set("#PermissionField.Value", formPermission);
        cmd.set("#GroupField.Value", formGroup);

        // Options
        cmd.set("#OneTimeField.Value", formOneTime);

        // Category dropdown
        List<ShopCategory> categories = new ArrayList<>(shopManager.getCategories());
        categories.sort((a, b) -> {
            int cmp = Integer.compare(a.getSort(), b.getSort());
            return cmp != 0 ? cmp : a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        var categoryEntries = new ArrayList<DropdownEntryInfo>();
        for (ShopCategory cat : categories) {
            categoryEntries.add(new DropdownEntryInfo(LocalizableString.fromString(cat.getDisplayName()), cat.getId()));
        }
        cmd.set("#CategoryDropdown.Entries", categoryEntries);
        cmd.set("#CategoryDropdown.Value", formCategoryId);

        // Sort
        cmd.set("#SortField.Value", formSort);

        // Bind text field events
        bindTextField(events, "#IdField", "id");
        bindTextField(events, "#DisplayNameField", "displayName");
        bindTextField(events, "#DescriptionField", "description");
        bindTextField(events, "#PermissionField", "permission");
        bindTextField(events, "#GroupField", "group");

        // Bind number field events
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CostField",
            EventData.of("Action", "field").append("Field", "cost")
                .append("@IntValue", "#CostField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SortField",
            EventData.of("Action", "field").append("Field", "sort")
                .append("@IntValue", "#SortField.Value"), false);

        // Bind checkbox event
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OneTimeField",
            EventData.of("Action", "field").append("Field", "oneTime")
                .append("@BoolValue", "#OneTimeField.Value"), false);

        // Bind dropdown event
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CategoryDropdown",
            EventData.of("Action", "field").append("Field", "category")
                .append("@Value", "#CategoryDropdown.Value"), false);

        // Action buttons
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
            EventData.of("Action", "field").append("Field", field)
                .append("@Value", elementId + ".Value"), false);
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

                case "save":
                    handleSave();
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ShopItemEditorPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFieldChange(PageEventData data) {
        if (data.field == null) return;

        switch (data.field) {
            case "id":
                if (data.value != null) formId = data.value;
                break;
            case "displayName":
                if (data.value != null) formDisplayName = data.value;
                break;
            case "description":
                if (data.value != null) formDescription = data.value;
                break;
            case "permission":
                if (data.value != null) formPermission = data.value;
                break;
            case "group":
                if (data.value != null) formGroup = data.value;
                break;
            case "category":
                if (data.value != null) formCategoryId = data.value;
                break;
            case "cost":
                if (data.intValue != null) formCost = data.intValue;
                break;
            case "sort":
                if (data.intValue != null) formSort = data.intValue;
                break;
            case "oneTime":
                if (data.boolValue != null) formOneTime = data.boolValue;
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
        if (formCost < 0) {
            showStatus("Cost must be >= 0", "#e74c3c");
            return;
        }
        if (formCategoryId == null || formCategoryId.trim().isEmpty()) {
            showStatus("Category is required", "#e74c3c");
            return;
        }

        if (isCreate && shopManager.itemExists(formId.trim())) {
            showStatus("Item ID already exists", "#e74c3c");
            return;
        }

        ShopItem item = new ShopItem();
        item.setId(formId.trim());
        item.setDisplayName(formDisplayName.trim());
        item.setDescription(formDescription.trim().isEmpty() ? null : formDescription.trim());
        item.setCost(formCost);
        item.setPermissionGranted(formPermission.trim().isEmpty() ? null : formPermission.trim());
        item.setGroupGranted(formGroup.trim().isEmpty() ? null : formGroup.trim());
        item.setOneTimePurchase(formOneTime);
        item.setSort(formSort);

        shopManager.saveItem(item, formCategoryId.trim());

        shutdown();
        if (onBack != null) onBack.run();
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
        public Integer intValue;
        public Boolean boolValue;

        public static final BuilderCodec<PageEventData> CODEC =
            BuilderCodec.builder(PageEventData.class, PageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("Field", Codec.STRING),
                    (d, v) -> d.field = v, d -> d.field).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING),
                    (d, v) -> d.value = v, d -> d.value).add()
                .append(new KeyedCodec<>("@IntValue", Codec.INTEGER),
                    (d, v) -> d.intValue = v, d -> d.intValue).add()
                .append(new KeyedCodec<>("@BoolValue", Codec.BOOLEAN),
                    (d, v) -> d.boolValue = v, d -> d.boolValue).add()
                .build();
    }
}
