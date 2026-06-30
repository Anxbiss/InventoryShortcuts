package de.anxbiss.shortcuts.client.screen;

import de.anxbiss.shortcuts.client.HudOverlay;
import de.anxbiss.shortcuts.client.config.ShortcutConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class HudLayoutScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int CONTROL_WIDTH = 110;
    private static final int SMALL_BUTTON_WIDTH = 24;
    private static final int SELECTED_BORDER_COLOR = 0xFFFFFFFF;
    private static final int AVAILABLE_BORDER_COLOR = 0x80FFFFFF;
    private static final int OVERLAY_COLOR = 0xA0000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFB0B0B0;

    private final Screen parent;
    private final ShortcutConfig workingCopy;

    private DisplayTarget selectedTarget = DisplayTarget.MOON;
    private boolean dragging;
    private boolean syncingControls;
    private int dragOffsetX;
    private int dragOffsetY;

    private ButtonWidget moonButton;
    private ButtonWidget weatherButton;
    private ButtonWidget scaleDownButton;
    private ButtonWidget scaleUpButton;

    public HudLayoutScreen(Screen parent, ShortcutConfig workingCopy) {
        super(Text.literal("HUD Layout"));
        this.parent = parent;
        this.workingCopy = workingCopy;
        this.workingCopy.normalize();
        this.ensureVisibleSelection();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int bottomY = this.height - 28;
        int controlsY = Math.max(32, bottomY - 26);

        this.moonButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Mond"), button -> {
                            this.workingCopy.showMoonDisplay = !this.workingCopy.showMoonDisplay;
                            this.ensureVisibleSelection();
                            this.syncControls();
                        })
                        .dimensions(centerX - 230, controlsY, CONTROL_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        this.weatherButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Wetter"), button -> {
                            this.workingCopy.showWeatherDisplay = !this.workingCopy.showWeatherDisplay;
                            this.ensureVisibleSelection();
                            this.syncControls();
                        })
                        .dimensions(centerX - 112, controlsY, CONTROL_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        this.scaleDownButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("-"), button -> this.adjustSelectedScale(-0.1F))
                        .dimensions(centerX + 12, controlsY, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        this.scaleUpButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("+"), button -> this.adjustSelectedScale(0.1F))
                        .dimensions(centerX + 42, controlsY, SMALL_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reset"), button -> this.resetSelected())
                        .dimensions(centerX + 78, controlsY, 64, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                        .dimensions(centerX + 150, controlsY, 80, BUTTON_HEIGHT)
                        .build()
        );

        this.syncControls();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, OVERLAY_COLOR);

        MinecraftClient minecraft = MinecraftClient.getInstance();
        TextRenderer textRenderer = minecraft.textRenderer;
        HudOverlay.HudDisplay moonDisplay = HudOverlay.createMoonDisplay(minecraft);
        HudOverlay.HudDisplay weatherDisplay = HudOverlay.createWeatherDisplay(minecraft);
        HudOverlay.DisplayPlacement weatherPlacement = HudOverlay.resolveWeatherPlacement(context, textRenderer, this.workingCopy, weatherDisplay);
        HudOverlay.DisplayPlacement moonPlacement = HudOverlay.resolveMoonPlacement(context, textRenderer, this.workingCopy, moonDisplay, weatherPlacement);

        if (this.workingCopy.showMoonDisplay) {
            this.renderEditableDisplay(context, textRenderer, moonDisplay, moonPlacement, this.workingCopy.moonDisplayScale, DisplayTarget.MOON);
        }

        if (this.workingCopy.showWeatherDisplay) {
            this.renderEditableDisplay(context, textRenderer, weatherDisplay, weatherPlacement, this.workingCopy.weatherDisplayScale, DisplayTarget.WEATHER);
        }

        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, TEXT_COLOR);
        String scaleText = this.selectedTarget == null
                ? "Scale: -"
                : "Scale: " + Math.round(this.getSelectedScale() * 100.0F) + "%";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(scaleText), this.width / 2, this.height - 56, MUTED_COLOR);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        DisplayTarget target = this.getDisplayAt((int) mouseX, (int) mouseY);
        if (target == null) {
            return false;
        }

        this.selectedTarget = target;
        this.dragging = true;
        HudOverlay.DisplayPlacement placement = this.getPlacement(target);
        this.dragOffsetX = (int) mouseX - placement.x();
        this.dragOffsetY = (int) mouseY - placement.y();
        this.storePosition(target, placement.x(), placement.y());
        this.syncControls();
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (!this.dragging || this.selectedTarget == null) {
            return super.mouseDragged(click, deltaX, deltaY);
        }

        HudOverlay.DisplayPlacement placement = this.getPlacement(this.selectedTarget);
        int scaledWidth = Math.round(placement.width() * this.getSelectedScale());
        int scaledHeight = Math.round(placement.height() * this.getSelectedScale());
        int x = clamp((int) mouseX - this.dragOffsetX, 0, Math.max(0, this.width - scaledWidth));
        int y = clamp((int) mouseY - this.dragOffsetY, 0, Math.max(0, this.height - scaledHeight));
        this.storePosition(this.selectedTarget, x, y);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.dragging = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.selectedTarget != null && (this.isSelectedVisible() || this.getDisplayAt((int) mouseX, (int) mouseY) != null)) {
            this.adjustSelectedScale(verticalAmount > 0.0D ? 0.1F : -0.1F);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderEditableDisplay(
            DrawContext context,
            TextRenderer textRenderer,
            HudOverlay.HudDisplay display,
            HudOverlay.DisplayPlacement placement,
            float scale,
            DisplayTarget target
    ) {
        HudOverlay.renderDisplay(context, textRenderer, display, placement.x(), placement.y(), scale);
        int scaledWidth = Math.round(placement.width() * scale);
        int scaledHeight = Math.round(placement.height() * scale);
        int color = target == this.selectedTarget ? SELECTED_BORDER_COLOR : AVAILABLE_BORDER_COLOR;
        context.drawStrokedRectangle(placement.x() - 2, placement.y() - 2, scaledWidth + 4, scaledHeight + 4, color);
    }

    private DisplayTarget getDisplayAt(int mouseX, int mouseY) {
        if (this.workingCopy.showMoonDisplay && this.contains(this.getPlacement(DisplayTarget.MOON), this.workingCopy.moonDisplayScale, mouseX, mouseY)) {
            return DisplayTarget.MOON;
        }

        if (this.workingCopy.showWeatherDisplay && this.contains(this.getPlacement(DisplayTarget.WEATHER), this.workingCopy.weatherDisplayScale, mouseX, mouseY)) {
            return DisplayTarget.WEATHER;
        }

        return null;
    }

    private boolean contains(HudOverlay.DisplayPlacement placement, float scale, int mouseX, int mouseY) {
        int scaledWidth = Math.round(placement.width() * scale);
        int scaledHeight = Math.round(placement.height() * scale);
        return mouseX >= placement.x()
                && mouseX <= placement.x() + scaledWidth
                && mouseY >= placement.y()
                && mouseY <= placement.y() + scaledHeight;
    }

    private HudOverlay.DisplayPlacement getPlacement(DisplayTarget target) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        TextRenderer textRenderer = minecraft.textRenderer;
        HudOverlay.HudDisplay weatherDisplay = HudOverlay.createWeatherDisplay(minecraft);
        HudOverlay.DisplayPlacement weatherPlacement = HudOverlay.resolveWeatherPlacement(this.width, this.height, textRenderer, this.workingCopy, weatherDisplay);

        if (target == DisplayTarget.WEATHER) {
            return weatherPlacement;
        }

        HudOverlay.HudDisplay moonDisplay = HudOverlay.createMoonDisplay(minecraft);
        return HudOverlay.resolveMoonPlacement(this.width, this.height, textRenderer, this.workingCopy, moonDisplay, weatherPlacement);
    }

    private void storePosition(DisplayTarget target, int x, int y) {
        HudOverlay.DisplayPlacement placement = this.getPlacement(target);
        float scale = this.getScale(target);
        int scaledWidth = Math.round(placement.width() * scale);
        int scaledHeight = Math.round(placement.height() * scale);
        int leftOffset = clamp(x, 0, Math.max(0, this.width - scaledWidth));
        int topOffset = clamp(y, 0, Math.max(0, this.height - scaledHeight));
        int rightOffset = Math.max(0, this.width - leftOffset - scaledWidth);
        int bottomOffset = Math.max(0, this.height - topOffset - scaledHeight);
        boolean rightAnchored = rightOffset <= leftOffset;
        boolean bottomAnchored = bottomOffset <= topOffset;
        String anchor = (bottomAnchored ? "BOTTOM" : "TOP") + "_" + (rightAnchored ? "RIGHT" : "LEFT");
        int offsetX = rightAnchored ? rightOffset : leftOffset;
        int offsetY = bottomAnchored ? bottomOffset : topOffset;

        if (target == DisplayTarget.MOON) {
            this.workingCopy.moonDisplayX = x;
            this.workingCopy.moonDisplayY = y;
            this.workingCopy.moonDisplayAnchor = anchor;
            this.workingCopy.moonDisplayOffsetX = offsetX;
            this.workingCopy.moonDisplayOffsetY = offsetY;
            return;
        }

        this.workingCopy.weatherDisplayX = x;
        this.workingCopy.weatherDisplayY = y;
        this.workingCopy.weatherDisplayAnchor = anchor;
        this.workingCopy.weatherDisplayOffsetX = offsetX;
        this.workingCopy.weatherDisplayOffsetY = offsetY;
    }

    private float getSelectedScale() {
        return this.selectedTarget == null ? 1.0F : this.getScale(this.selectedTarget);
    }

    private float getScale(DisplayTarget target) {
        if (target == null) {
            return 1.0F;
        }

        if (target == DisplayTarget.WEATHER) {
            return this.workingCopy.weatherDisplayScale;
        }

        return this.workingCopy.moonDisplayScale;
    }

    private void adjustSelectedScale(float delta) {
        if (this.selectedTarget == null) {
            return;
        }

        HudOverlay.DisplayPlacement placement = this.getPlacement(this.selectedTarget);
        this.storePosition(this.selectedTarget, placement.x(), placement.y());

        if (this.selectedTarget == DisplayTarget.MOON) {
            this.workingCopy.moonDisplayScale = clamp(this.workingCopy.moonDisplayScale + delta, 0.5F, 3.0F);
        } else {
            this.workingCopy.weatherDisplayScale = clamp(this.workingCopy.weatherDisplayScale + delta, 0.5F, 3.0F);
        }

        this.workingCopy.normalize();
        this.syncControls();
    }

    private void resetSelected() {
        if (this.selectedTarget == DisplayTarget.MOON) {
            this.workingCopy.moonDisplayX = -1;
            this.workingCopy.moonDisplayY = -1;
            this.workingCopy.moonDisplayAnchor = "BOTTOM_RIGHT";
            this.workingCopy.moonDisplayOffsetX = -1;
            this.workingCopy.moonDisplayOffsetY = -1;
            this.workingCopy.moonDisplayScale = 1.0F;
        } else if (this.selectedTarget == DisplayTarget.WEATHER) {
            this.workingCopy.weatherDisplayX = -1;
            this.workingCopy.weatherDisplayY = -1;
            this.workingCopy.weatherDisplayAnchor = "BOTTOM_RIGHT";
            this.workingCopy.weatherDisplayOffsetX = -1;
            this.workingCopy.weatherDisplayOffsetY = -1;
            this.workingCopy.weatherDisplayScale = 1.0F;
        }

        this.syncControls();
    }

    private void ensureVisibleSelection() {
        if (this.selectedTarget == DisplayTarget.MOON && this.workingCopy.showMoonDisplay) {
            return;
        }

        if (this.selectedTarget == DisplayTarget.WEATHER && this.workingCopy.showWeatherDisplay) {
            return;
        }

        if (this.workingCopy.showMoonDisplay) {
            this.selectedTarget = DisplayTarget.MOON;
        } else if (this.workingCopy.showWeatherDisplay) {
            this.selectedTarget = DisplayTarget.WEATHER;
        } else {
            this.selectedTarget = null;
        }
    }

    private boolean isSelectedVisible() {
        return this.selectedTarget == DisplayTarget.MOON && this.workingCopy.showMoonDisplay
                || this.selectedTarget == DisplayTarget.WEATHER && this.workingCopy.showWeatherDisplay;
    }

    private void syncControls() {
        this.workingCopy.normalize();
        this.syncingControls = true;
        if (this.moonButton != null) {
            this.moonButton.setMessage(onOffText("Mond", this.workingCopy.showMoonDisplay));
        }

        if (this.weatherButton != null) {
            this.weatherButton.setMessage(onOffText("Wetter", this.workingCopy.showWeatherDisplay));
        }

        boolean canScale = this.selectedTarget != null && this.isSelectedVisible();
        if (this.scaleDownButton != null) {
            this.scaleDownButton.active = canScale;
        }

        if (this.scaleUpButton != null) {
            this.scaleUpButton.active = canScale;
        }
        this.syncingControls = false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Text onOffText(String label, boolean value) {
        return Text.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    private enum DisplayTarget {
        MOON,
        WEATHER
    }
}
