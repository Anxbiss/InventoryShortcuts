package de.anxbiss.shortcuts.client.gui;

import de.anxbiss.shortcuts.client.ShortcutExecutor;
import de.anxbiss.shortcuts.client.config.ShortcutConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class InventoryShortcutButton extends ClickableWidget {
    public static final int WIDTH = 26;
    public static final int HEIGHT = 32;

    private static final int HIDDEN_EDGE_PIXELS = 4;
    private static final float HOVER_ICON_SCALE_DELTA = 0.08F;
    private static final float HOVER_ANIMATION_EASING = 0.32F;
    private static final float HOVER_ICON_LIFT = 1.0F;

    private static final Identifier[] UNSELECTED_TOP_TABS = createSpriteArray("container/creative_inventory/tab_top_unselected_");
    private static final Identifier[] SELECTED_TOP_TABS = createSpriteArray("container/creative_inventory/tab_top_selected_");
    private static final Identifier[] UNSELECTED_BOTTOM_TABS = createSpriteArray("container/creative_inventory/tab_bottom_unselected_");
    private static final Identifier[] SELECTED_BOTTOM_TABS = createSpriteArray("container/creative_inventory/tab_bottom_selected_");

    private final ShortcutConfigManager.ResolvedShortcut shortcut;
    private final boolean showTooltips;
    private final boolean topRow;

    private int spriteIndex;
    private float hoverAnimationProgress;

    public InventoryShortcutButton(int x, int y, ShortcutConfigManager.ResolvedShortcut shortcut, boolean showTooltips, boolean topRow) {
        super(x, y, WIDTH, HEIGHT, Text.literal(shortcut.label()));
        this.shortcut = shortcut;
        this.showTooltips = showTooltips;
        this.topRow = topRow;
    }

    public void setSpriteIndex(int spriteIndex) {
        this.spriteIndex = Math.max(0, spriteIndex);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        boolean highlighted = this.isHovered();
        this.hoverAnimationProgress += ((highlighted ? 1.0F : 0.0F) - this.hoverAnimationProgress) * HOVER_ANIMATION_EASING;
        if (Math.abs((highlighted ? 1.0F : 0.0F) - this.hoverAnimationProgress) < 0.01F) {
            this.hoverAnimationProgress = highlighted ? 1.0F : 0.0F;
        }

        float easedProgress = smoothStep(this.hoverAnimationProgress);
        int scissorLeft = this.getX();
        int scissorRight = this.getX() + this.getWidth();
        int scissorTop = this.topRow ? this.getY() : this.getY() + HIDDEN_EDGE_PIXELS;
        int scissorBottom = this.topRow ? this.getY() + this.getHeight() - HIDDEN_EDGE_PIXELS : this.getY() + this.getHeight();

        context.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                this.getCurrentSprite(highlighted || this.hoverAnimationProgress > 0.01F),
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight()
        );

        int iconX = this.getX() + 13 - 8;
        int iconY = this.getY() + 16 - 8 + (this.topRow ? 1 : -1) - Math.round(easedProgress * HOVER_ICON_LIFT);
        float centerX = iconX + 8.0F;
        float centerY = iconY + 8.0F;
        float scale = 1.0F + easedProgress * HOVER_ICON_SCALE_DELTA;

        context.getMatrices().pushMatrix();
        context.getMatrices().scaleAround(scale, scale, centerX, centerY);
        context.drawItem(this.shortcut.iconStack(), iconX, iconY);
        context.getMatrices().popMatrix();
        context.disableScissor();

        if (this.showTooltips && highlighted) {
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, this.shortcut.tooltipLines(), mouseX, mouseY);
        }
    }

    @Override
    public void onClick(Click click, boolean doubleClick) {
        ShortcutExecutor.execute(this.shortcut.command());
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        ClickableWidget.playClickSound(soundManager);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    private Identifier getCurrentSprite(boolean highlighted) {
        Identifier[] sprites = this.topRow
                ? (highlighted ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS)
                : (highlighted ? SELECTED_BOTTOM_TABS : UNSELECTED_BOTTOM_TABS);
        return sprites[Math.min(this.spriteIndex, sprites.length - 1)];
    }

    private static Identifier[] createSpriteArray(String prefix) {
        Identifier[] sprites = new Identifier[7];
        for (int i = 0; i < sprites.length; i++) {
            sprites[i] = Identifier.ofVanilla(prefix + (i + 1));
        }
        return sprites;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

}
