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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.shop.ShopCategory;
import de.ragesith.hyarena2.shop.ShopManager;
import de.ragesith.hyarena2.ui.hud.HudManager;
import de.ragesith.hyarena2.ui.page.CloseablePage;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Editor page for creating and editing shop categories.
 */
public class ShopCategoryEditorPage extends InteractiveCustomUIPage<ShopCategoryEditorPage.PageEventData> implements CloseablePage {

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
    private int formSort;

    public ShopCategoryEditorPage(PlayerRef playerRef, UUID playerUuid,
                                   ShopCategory existingCategory, ShopManager shopManager,
                                   HudManager hudManager, ScheduledExecutorService scheduler,
                                   Runnable onBack) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.isCreate = (existingCategory == null);
        this.shopManager = shopManager;
        this.hudManager = hudManager;
        this.scheduler = scheduler;
        this.onBack = onBack;

        if (existingCategory != null) {
            formId = existingCategory.getId();
            formDisplayName = existingCategory.getDisplayName();
            formSort = existingCategory.getSort();
        } else {
            formId = "";
            formDisplayName = "";
            formSort = 0;
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/ShopCategoryEditorPage.ui");

        cmd.set("#TitleLabel.Text", isCreate ? "Create Category" : "Edit Category: " + formId);

        cmd.set("#IdField.Value", formId);
        cmd.set("#DisplayNameField.Value", formDisplayName);
        cmd.set("#SortField.Value", formSort);

        // Bind text field events
        bindTextField(events, "#IdField", "id");
        bindTextField(events, "#DisplayNameField", "displayName");

        // Bind number field event
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SortField",
            EventData.of("Action", "field").append("Field", "sort")
                .append("@IntValue", "#SortField.Value"), false);

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
            System.err.println("[ShopCategoryEditorPage] Error handling event: " + e.getMessage());
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
            case "sort":
                if (data.intValue != null) formSort = data.intValue;
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

        if (isCreate && shopManager.categoryExists(formId.trim())) {
            showStatus("Category ID already exists", "#e74c3c");
            return;
        }

        ShopCategory category = new ShopCategory();
        category.setId(formId.trim());
        category.setDisplayName(formDisplayName.trim());
        category.setSort(formSort);

        shopManager.saveCategory(category);

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
                .build();
    }
}
