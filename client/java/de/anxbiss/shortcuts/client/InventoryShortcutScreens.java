package de.anxbiss.shortcuts.client;

import de.anxbiss.shortcuts.client.config.ShortcutConfig;
import de.anxbiss.shortcuts.client.config.ShortcutConfigManager;
import de.anxbiss.shortcuts.client.gui.InventoryShortcutButton;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;

final class InventoryShortcutScreens {
    private static final int INVENTORY_BACKGROUND_WIDTH = 176;
    private static final int INVENTORY_BACKGROUND_HEIGHT = 166;

    private InventoryShortcutScreens() {
    }

    static void attachTo(Screen screen) {
        if (!(screen instanceof InventoryScreen)) {
            return;
        }

        ShortcutConfigManager.initialize();
        ShortcutConfig config = ShortcutConfigManager.getConfig();
        if (!config.enabled || !config.showInInventory) {
            return;
        }

        int left = (screen.width - INVENTORY_BACKGROUND_WIDTH) / 2;
        int top = (screen.height - INVENTORY_BACKGROUND_HEIGHT) / 2;
        int topY = top - 28 + (config.topMargin - 24);
        int bottomY = top + INVENTORY_BACKGROUND_HEIGHT - 4 + (config.bottomMargin - 4);

        @SuppressWarnings("unchecked")
        List<ClickableWidget> widgets = (List<ClickableWidget>) (List<?>) Screens.getButtons(screen);

        addRow(widgets, left, topY, ShortcutConfigManager.resolveTopShortcuts(), config, true);
        addRow(widgets, left, bottomY, ShortcutConfigManager.resolveBottomShortcuts(), config, false);
    }

    private static void addRow(
            List<ClickableWidget> widgets,
            int left,
            int y,
            List<ShortcutConfigManager.ResolvedShortcut> shortcuts,
            ShortcutConfig config,
            boolean topRow
    ) {
        int count = shortcuts.size();
        if (count == 0) {
            return;
        }

        int spacing = Math.max(0, 1 + (config.buttonSpacing - 4));
        int totalWidth = count * InventoryShortcutButton.WIDTH + (count - 1) * spacing;
        int startX = left + (INVENTORY_BACKGROUND_WIDTH - totalWidth) / 2;

        for (int i = 0; i < count; i++) {
            InventoryShortcutButton button = new InventoryShortcutButton(
                    startX + i * (InventoryShortcutButton.WIDTH + spacing),
                    y,
                    shortcuts.get(i),
                    config.showTooltips,
                    topRow
            );
            button.setSpriteIndex(i);
            widgets.add(button);
        }
    }
}
