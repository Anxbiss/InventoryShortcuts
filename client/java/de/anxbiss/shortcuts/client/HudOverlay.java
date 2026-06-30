package de.anxbiss.shortcuts.client;

import de.anxbiss.shortcuts.client.config.ShortcutConfig;
import de.anxbiss.shortcuts.client.config.ShortcutConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class HudOverlay implements HudRenderCallback {
    private static final int ICON_SIZE = 16;
    private static final int ICON_Y_OFFSET = -2;
    private static final int TEXT_GAP = 4;
    private static final int ROW_HEIGHT = 16;
    private static final int PANEL_PADDING_X = 5;
    private static final int PANEL_PADDING_Y = 4;
    private static final int SCREEN_MARGIN = 8;
    private static final int DISPLAY_GAP = 4;
    private static final int TEXT_COLOR = 0xFFF5F5F5;
    private static final int BACKGROUND_COLOR = 0x90000000;

    private static final String[] MOON_PHASE_LABELS = new String[] {
            "Vollmond",
            "Abnehmend",
            "Halbmond",
            "Abnehmende Sichel",
            "Neumond",
            "Zunehmende Sichel",
            "Halbmond",
            "Zunehmend"
    };

    private static final Identifier[] MOON_TEXTURES = new Identifier[] {
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase1.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase8.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase7.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase6.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase5.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase4.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase3.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/moonphase2.png")
    };

    private static final Identifier[] WEATHER_TEXTURES = new Identifier[] {
            Identifier.of("inventoryshortcuts", "textures/gui/clear.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/rain.png"),
            Identifier.of("inventoryshortcuts", "textures/gui/thunder.png")
    };

    @Override
    public void onHudRender(@NotNull DrawContext context, RenderTickCounter tickCounter) {
        render(context);
    }

    public void render(DrawContext context) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player == null || minecraft.world == null) {
            return;
        }

        ShortcutConfig config = ShortcutConfigManager.getConfig();
        if (!config.enabled || (!config.showMoonDisplay && !config.showWeatherDisplay)) {
            return;
        }

        TextRenderer textRenderer = minecraft.textRenderer;
        HudDisplay moonDisplay = createMoonDisplay(minecraft);
        HudDisplay weatherDisplay = createWeatherDisplay(minecraft);

        DisplayPlacement weatherPlacement = resolveWeatherPlacement(context, textRenderer, config, weatherDisplay);
        DisplayPlacement moonPlacement = resolveMoonPlacement(context, textRenderer, config, moonDisplay, weatherPlacement);

        if (config.showMoonDisplay) {
            renderDisplay(context, textRenderer, moonDisplay, moonPlacement.x(), moonPlacement.y(), config.moonDisplayScale);
        }

        if (config.showWeatherDisplay) {
            renderDisplay(context, textRenderer, weatherDisplay, weatherPlacement.x(), weatherPlacement.y(), config.weatherDisplayScale);
        }
    }

    public static HudDisplay createMoonDisplay(MinecraftClient minecraft) {
        int moonPhase = 0;
        if (minecraft.world != null) {
            moonPhase = (int) ((minecraft.world.getTimeOfDay() / 24000L % 8 + 8) % 8);
        }

        return new HudDisplay(MOON_TEXTURES[moonPhase], "Mondphase: " + MOON_PHASE_LABELS[moonPhase]);
    }

    public static HudDisplay createWeatherDisplay(MinecraftClient minecraft) {
        boolean raining = minecraft.world != null && minecraft.world.isRaining();
        boolean thundering = minecraft.world != null && minecraft.world.isThundering();
        Identifier weatherTexture = thundering
                ? WEATHER_TEXTURES[2]
                : raining
                    ? WEATHER_TEXTURES[1]
                    : WEATHER_TEXTURES[0];

        return new HudDisplay(weatherTexture, "Wetter: " + getWeatherLabel(raining, thundering));
    }

    public static DisplayPlacement resolveMoonPlacement(
            DrawContext context,
            TextRenderer textRenderer,
            ShortcutConfig config,
            HudDisplay display,
            DisplayPlacement weatherPlacement
    ) {
        return resolveMoonPlacement(
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight(),
                textRenderer,
                config,
                display,
                weatherPlacement
        );
    }

    public static DisplayPlacement resolveMoonPlacement(
            int screenWidth,
            int screenHeight,
            TextRenderer textRenderer,
            ShortcutConfig config,
            HudDisplay display,
            DisplayPlacement weatherPlacement
    ) {
        int width = getDisplayWidth(textRenderer, display);
        ensureMoonAnchor(config, screenWidth, screenHeight, width, getDisplayHeight());
        int defaultX = getDefaultX(screenWidth, width, config.moonDisplayScale);
        int defaultY;
        if (config.showWeatherDisplay && weatherPlacement != null) {
            defaultY = weatherPlacement.y() - getScaledDisplayHeight(config.moonDisplayScale) - DISPLAY_GAP;
        } else {
            defaultY = getDefaultY(screenHeight, config.moonDisplayScale);
        }

        return new DisplayPlacement(
                resolveX(screenWidth, config.moonDisplayX, config.moonDisplayAnchor, config.moonDisplayOffsetX, defaultX, width, config.moonDisplayScale),
                resolveY(screenHeight, config.moonDisplayY, config.moonDisplayAnchor, config.moonDisplayOffsetY, defaultY, config.moonDisplayScale),
                width,
                getDisplayHeight()
        );
    }

    public static DisplayPlacement resolveWeatherPlacement(
            DrawContext context,
            TextRenderer textRenderer,
            ShortcutConfig config,
            HudDisplay display
    ) {
        return resolveWeatherPlacement(
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight(),
                textRenderer,
                config,
                display
        );
    }

    public static DisplayPlacement resolveWeatherPlacement(
            int screenWidth,
            int screenHeight,
            TextRenderer textRenderer,
            ShortcutConfig config,
            HudDisplay display
    ) {
        int width = getDisplayWidth(textRenderer, display);
        ensureWeatherAnchor(config, screenWidth, screenHeight, width, getDisplayHeight());
        int defaultX = getDefaultX(screenWidth, width, config.weatherDisplayScale);
        int defaultY = getDefaultY(screenHeight, config.weatherDisplayScale);
        return new DisplayPlacement(
                resolveX(screenWidth, config.weatherDisplayX, config.weatherDisplayAnchor, config.weatherDisplayOffsetX, defaultX, width, config.weatherDisplayScale),
                resolveY(screenHeight, config.weatherDisplayY, config.weatherDisplayAnchor, config.weatherDisplayOffsetY, defaultY, config.weatherDisplayScale),
                width,
                getDisplayHeight()
        );
    }

    public static void renderDisplay(
            DrawContext context,
            TextRenderer textRenderer,
            HudDisplay display,
            int x,
            int y,
            float scale
    ) {
        int panelWidth = getDisplayWidth(textRenderer, display);
        int panelHeight = getDisplayHeight();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.fill(0, 0, panelWidth, panelHeight, BACKGROUND_COLOR);
        drawRow(context, textRenderer, display.texture(), display.text(), PANEL_PADDING_X, PANEL_PADDING_Y);
        context.getMatrices().popMatrix();
    }

    public static int getDisplayWidth(TextRenderer textRenderer, HudDisplay display) {
        return PANEL_PADDING_X * 2 + ICON_SIZE + TEXT_GAP + textRenderer.getWidth(display.text());
    }

    public static int getDisplayHeight() {
        return PANEL_PADDING_Y * 2 + ROW_HEIGHT;
    }

    public static int getScaledDisplayWidth(TextRenderer textRenderer, HudDisplay display, float scale) {
        return Math.round(getDisplayWidth(textRenderer, display) * scale);
    }

    public static int getScaledDisplayHeight(float scale) {
        return Math.round(getDisplayHeight() * scale);
    }

    private static int getDefaultX(int screenWidth, int displayWidth, float scale) {
        return screenWidth - Math.round(displayWidth * scale) - SCREEN_MARGIN;
    }

    private static int getDefaultY(int screenHeight, float scale) {
        return screenHeight - getScaledDisplayHeight(scale) - SCREEN_MARGIN;
    }

    private static int resolveX(int screenWidth, int configuredX, String anchor, int offsetX, int defaultX, int displayWidth, float scale) {
        int scaledWidth = Math.round(displayWidth * scale);
        if (offsetX >= 0) {
            int anchoredX = isRightAnchored(anchor) ? screenWidth - scaledWidth - offsetX : offsetX;
            return clamp(anchoredX, 0, Math.max(0, screenWidth - scaledWidth));
        }

        int x = configuredX >= 0 ? configuredX : defaultX;
        return clamp(x, 0, Math.max(0, screenWidth - scaledWidth));
    }

    private static int resolveY(int screenHeight, int configuredY, String anchor, int offsetY, int defaultY, float scale) {
        int scaledHeight = getScaledDisplayHeight(scale);
        if (offsetY >= 0) {
            int anchoredY = isBottomAnchored(anchor) ? screenHeight - scaledHeight - offsetY : offsetY;
            return clamp(anchoredY, 0, Math.max(0, screenHeight - scaledHeight));
        }

        int y = configuredY >= 0 ? configuredY : defaultY;
        return clamp(y, 0, Math.max(0, screenHeight - scaledHeight));
    }

    private static boolean isRightAnchored(String anchor) {
        return anchor != null && anchor.endsWith("RIGHT");
    }

    private static boolean isBottomAnchored(String anchor) {
        return anchor != null && anchor.startsWith("BOTTOM");
    }

    private static void ensureMoonAnchor(ShortcutConfig config, int screenWidth, int screenHeight, int displayWidth, int displayHeight) {
        if ((config.moonDisplayOffsetX >= 0 && config.moonDisplayOffsetY >= 0) || config.moonDisplayX < 0 || config.moonDisplayY < 0) {
            return;
        }

        AnchorPlacement anchor = createAnchorPlacement(
                screenWidth,
                screenHeight,
                config.moonDisplayX,
                config.moonDisplayY,
                Math.round(displayWidth * config.moonDisplayScale),
                Math.round(displayHeight * config.moonDisplayScale)
        );
        config.moonDisplayAnchor = anchor.anchor();
        config.moonDisplayOffsetX = anchor.offsetX();
        config.moonDisplayOffsetY = anchor.offsetY();
    }

    private static void ensureWeatherAnchor(ShortcutConfig config, int screenWidth, int screenHeight, int displayWidth, int displayHeight) {
        if ((config.weatherDisplayOffsetX >= 0 && config.weatherDisplayOffsetY >= 0) || config.weatherDisplayX < 0 || config.weatherDisplayY < 0) {
            return;
        }

        AnchorPlacement anchor = createAnchorPlacement(
                screenWidth,
                screenHeight,
                config.weatherDisplayX,
                config.weatherDisplayY,
                Math.round(displayWidth * config.weatherDisplayScale),
                Math.round(displayHeight * config.weatherDisplayScale)
        );
        config.weatherDisplayAnchor = anchor.anchor();
        config.weatherDisplayOffsetX = anchor.offsetX();
        config.weatherDisplayOffsetY = anchor.offsetY();
    }

    private static AnchorPlacement createAnchorPlacement(int screenWidth, int screenHeight, int x, int y, int scaledWidth, int scaledHeight) {
        int clampedX = clamp(x, 0, Math.max(0, screenWidth - scaledWidth));
        int clampedY = clamp(y, 0, Math.max(0, screenHeight - scaledHeight));
        int rightOffset = Math.max(0, screenWidth - clampedX - scaledWidth);
        int bottomOffset = Math.max(0, screenHeight - clampedY - scaledHeight);
        boolean rightAnchored = rightOffset <= clampedX;
        boolean bottomAnchored = bottomOffset <= clampedY;
        String anchor = (bottomAnchored ? "BOTTOM" : "TOP") + "_" + (rightAnchored ? "RIGHT" : "LEFT");
        return new AnchorPlacement(anchor, rightAnchored ? rightOffset : clampedX, bottomAnchored ? bottomOffset : clampedY);
    }

    private static void drawRow(DrawContext context, TextRenderer textRenderer, Identifier texture, String text, int x, int y) {
        drawIcon(context, texture, x, y + ICON_Y_OFFSET);
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(text),
                x + ICON_SIZE + TEXT_GAP,
                y + (ICON_SIZE - textRenderer.fontHeight) / 2,
                TEXT_COLOR
        );
    }

    private static void drawIcon(DrawContext context, Identifier texture, int x, int y) {
        context.drawTexturedQuad(texture, x, y, x + ICON_SIZE, y + ICON_SIZE, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    private static String getWeatherLabel(boolean raining, boolean thundering) {
        if (thundering) {
            return "Gewitter";
        }

        if (raining) {
            return "Regen";
        }

        return "Klar";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record HudDisplay(Identifier texture, String text) {
    }

    public record DisplayPlacement(int x, int y, int width, int height) {
    }

    private record AnchorPlacement(String anchor, int offsetX, int offsetY) {
    }
}
