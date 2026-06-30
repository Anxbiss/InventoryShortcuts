package de.anxbiss.shortcuts.client;

import de.anxbiss.shortcuts.client.config.ShortcutConfigManager;
import de.anxbiss.shortcuts.client.screen.RadialShortcutScreen;
import de.anxbiss.shortcuts.client.screen.ShortcutsConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;



public class mainClient implements ClientModInitializer {
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(Identifier.of("inventoryshortcuts", "general"));

    private static final KeyBinding OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.inventoryshortcuts.open_config",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_O,
                    KEY_CATEGORY
            )
    );

    private static final KeyBinding OPEN_RADIAL_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.inventoryshortcuts.open_radial",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    KEY_CATEGORY
            )
    );

    @Override
    public void onInitializeClient() {
        ShortcutConfigManager.initialize();
        HudRenderCallback.EVENT.register(new HudOverlay());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_CONFIG_KEY.wasPressed()) {
                client.setScreen(new ShortcutsConfigScreen(client.currentScreen));
            }

            while (OPEN_RADIAL_KEY.wasPressed()) {
                if (client.currentScreen == null
                        && ShortcutConfigManager.getConfig().enabled
                        && ShortcutConfigManager.getConfig().radialMenuEnabled) {
                    client.setScreen(new RadialShortcutScreen(OPEN_RADIAL_KEY));
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            InventoryShortcutScreens.attachTo(screen);

        });
    }
}
