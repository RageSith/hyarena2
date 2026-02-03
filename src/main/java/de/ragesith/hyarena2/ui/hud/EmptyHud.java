package de.ragesith.hyarena2.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * An empty HUD used as a placeholder when clearing the custom HUD.
 */
public class EmptyHud extends CustomUIHud {

    public EmptyHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    public void build(UICommandBuilder cmd) {
        // Empty - no UI elements
    }
}
