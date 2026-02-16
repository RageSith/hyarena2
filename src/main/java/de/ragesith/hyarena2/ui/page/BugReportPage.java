package de.ragesith.hyarena2.ui.page;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.ragesith.hyarena2.api.ApiClient;
import de.ragesith.hyarena2.ui.hud.HudManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bug report form page. Players fill in title, category, and description,
 * then submit to the web API.
 */
public class BugReportPage extends InteractiveCustomUIPage<BugReportPage.PageEventData> implements CloseablePage {

    private final PlayerRef playerRef;
    private final UUID playerUuid;
    private final String playerName;
    private final ApiClient apiClient;
    private final HudManager hudManager;
    private final World world;

    private volatile boolean active = true;

    // Form state
    private String formTitle = "";
    private String formCategory = "other";
    private String formDescription = "";

    private static final String[][] CATEGORIES = {
        {"ui_ux", "UI / UX"},
        {"arena", "Arena"},
        {"kit", "Kit"},
        {"matchmaking", "Matchmaking"},
        {"other", "Other"},
    };

    public BugReportPage(PlayerRef playerRef, UUID playerUuid, String playerName,
                         ApiClient apiClient, HudManager hudManager, World world) {
        super(playerRef, CustomPageLifetime.CantClose, PageEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.apiClient = apiClient;
        this.hudManager = hudManager;
        this.world = world;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/BugReportPage.ui");

        // Populate category dropdown
        List<DropdownEntryInfo> categoryEntries = new ArrayList<>();
        for (String[] cat : CATEGORIES) {
            categoryEntries.add(new DropdownEntryInfo(LocalizableString.fromString(cat[1]), cat[0]));
        }
        cmd.set("#CategoryDropdown.Entries", categoryEntries);
        cmd.set("#CategoryDropdown.Value", formCategory);

        // Bind text field events
        bindTextField(events, "#TitleField", "title");
        bindTextField(events, "#DescriptionField", "description");

        // Bind dropdown event
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CategoryDropdown",
            EventData.of("Action", "field").append("Field", "category")
                .append("@Value", "#CategoryDropdown.Value"), false);

        // Action buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SubmitBtn",
            EventData.of("Action", "submit"), false);
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
                    if (player != null) player.getPageManager().setPage(ref, store, Page.None);
                    break;

                case "field":
                    handleFieldChange(data);
                    break;

                case "submit":
                    handleSubmit();
                    break;
            }
        } catch (Exception e) {
            System.err.println("[BugReportPage] Error handling event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFieldChange(PageEventData data) {
        if (data.field == null) return;

        switch (data.field) {
            case "title":
                if (data.value != null) formTitle = data.value;
                break;
            case "category":
                if (data.value != null) formCategory = data.value;
                break;
            case "description":
                if (data.value != null) formDescription = data.value;
                break;
        }
    }

    private void handleSubmit() {
        // Validate title
        if (formTitle == null || formTitle.trim().length() < 3) {
            showStatus("Title must be at least 3 characters", "#e74c3c");
            return;
        }

        // Validate description
        if (formDescription == null || formDescription.trim().length() < 10) {
            showStatus("Description must be at least 10 characters", "#e74c3c");
            return;
        }

        showStatus("Submitting report...", "#f39c12");

        // Build JSON payload
        String title = formTitle.trim().replace("\"", "\\\"");
        String desc = formDescription.trim().replace("\"", "\\\"");
        String category = formCategory != null ? formCategory : "other";
        String name = playerName.replace("\"", "\\\"");

        String json = "{\"uuid\":\"" + playerUuid + "\","
            + "\"username\":\"" + name + "\","
            + "\"title\":\"" + title + "\","
            + "\"category\":\"" + category + "\","
            + "\"description\":\"" + desc + "\"}";

        apiClient.postAsync("/api/bug/submit", json).thenAccept(response -> {
            world.execute(() -> {
                if (!active) return;

                if (response == null) {
                    showStatus("Failed to connect to the API. Try again later.", "#e74c3c");
                    return;
                }

                int status = response.statusCode();
                if (status != 200) {
                    showStatus("Failed to submit report (HTTP " + status + ")", "#e74c3c");
                    return;
                }

                try {
                    JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!body.get("success").getAsBoolean()) {
                        String error = body.getAsJsonObject("error").get("message").getAsString();
                        showStatus("Error: " + error, "#e74c3c");
                        return;
                    }

                    int reportId = body.getAsJsonObject("data").get("report_id").getAsInt();
                    showStatus("Bug report #" + reportId + " submitted! Thank you.", "#2ecc71");
                } catch (Exception e) {
                    showStatus("Failed to parse API response.", "#e74c3c");
                    System.err.println("[BugReportPage] Response parse error: " + e.getMessage());
                }
            });
        });
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
