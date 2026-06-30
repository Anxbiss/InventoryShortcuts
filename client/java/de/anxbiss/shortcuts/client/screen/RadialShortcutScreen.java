package de.anxbiss.shortcuts.client.screen;

import de.anxbiss.shortcuts.client.ShortcutExecutor;
import de.anxbiss.shortcuts.client.config.ShortcutConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class RadialShortcutScreen extends Screen {
    private static final int BACKGROUND_RGB = 0x000000;
    private static final int RING_RGB = 0x101010;
    private static final int RING_EDGE_RGB = 0x3A3A3A;
    private static final int SLOT_RGB = 0x2B2B2B;
    private static final int SELECTED_RGB = 0xB00000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_TEXT_COLOR = 0xFFB0B0B0;
    private static final float FADE_IN_SPEED = 5.2F;
    private static final float FADE_OUT_SPEED = 7.4F;
    private static final float HOVER_RESPONSE = 15.0F;

    private final KeyBinding holdKey;
    private final List<ShortcutConfigManager.ResolvedShortcut> shortcuts;
    private final float[] hoverAnimations;

    private float alpha;
    private float alphaVelocity;
    private long lastFrameTimeNanos = -1L;
    private boolean closing;
    private boolean executed;
    private boolean releaseArmed;
    private int hoveredIndex = -1;

    public RadialShortcutScreen(KeyBinding holdKey) {
        super(Text.literal("Radial Shortcuts"));
        this.holdKey = holdKey;
        this.shortcuts = ShortcutConfigManager.resolveRadialShortcuts();
        this.hoverAnimations = new float[this.shortcuts.size()];
    }

    @Override
    protected void init() {
        this.keepCursorUnlocked();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        this.keepCursorUnlocked();
        boolean holdKeyDown = this.isHoldKeyDown();
        if (holdKeyDown) {
            this.releaseArmed = true;
        }

        if (!this.closing && !holdKeyDown) {
            if (this.releaseArmed) {
                this.executeHoveredShortcut();
            }
            this.closing = true;
        }
    }

    @Override
    public void close() {
        this.closing = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.keepCursorUnlocked();
        float frameDelta = this.updateFrameAnimation();
        if (this.client != null && this.client.currentScreen != this) {
            return;
        }

        this.hoveredIndex = this.findHoveredShortcut(mouseX, mouseY);
        this.updateHoverAnimations(frameDelta);

        float easedAlpha = smoothStep(this.alpha);
        context.fill(0, 0, this.width, this.height, color(BACKGROUND_RGB, 0.48F * easedAlpha));

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int radius = this.getAnimatedRadius();
        int outerRadius = radius + 31;
        int innerRadius = Math.max(30, radius - 31);

        float blurStrength = Math.min(1.0F, Math.abs(this.alphaVelocity) * 10.0F);
        this.drawMotionBlur(context, centerX, centerY, innerRadius, outerRadius, radius, easedAlpha, blurStrength);
        this.drawRing(context, centerX, centerY, innerRadius, outerRadius, color(RING_RGB, 0.74F * easedAlpha));
        this.drawRing(context, centerX, centerY, outerRadius - 2, outerRadius, color(RING_EDGE_RGB, 0.24F * easedAlpha));
        this.drawRing(context, centerX, centerY, innerRadius, innerRadius + 2, color(0x000000, 0.5F * easedAlpha));
        this.drawFilledCircle(context, centerX, centerY, innerRadius, color(BACKGROUND_RGB, 0.78F * easedAlpha));

        if (this.shortcuts.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No radial shortcuts"), centerX, centerY - 5, MUTED_TEXT_COLOR);
            super.render(context, mouseX, mouseY, deltaTicks);
            return;
        }

        if (this.hoveredIndex >= 0) {
            double selectedAngle = this.getSlotAngle(this.hoveredIndex, this.shortcuts.size());
            float hoverEase = smoothStep(this.hoverAnimations[this.hoveredIndex]);
            this.drawArcSegment(
                    context,
                    centerX,
                    centerY,
                    innerRadius + 2,
                    outerRadius - 2,
                    selectedAngle,
                    Math.PI / Math.max(3, this.shortcuts.size()) * 0.72D,
                    color(SELECTED_RGB, 0.34F * easedAlpha * hoverEase)
            );
        }

        for (int i = 0; i < this.shortcuts.size(); i++) {
            ShortcutConfigManager.ResolvedShortcut shortcut = this.shortcuts.get(i);
            double angle = this.getSlotAngle(i, this.shortcuts.size());
            float hoverEase = smoothStep(this.hoverAnimations[i]);
            double slotRadius = radius + hoverEase * 7.0D;
            int slotCenterX = centerX + (int) Math.round(Math.cos(angle) * slotRadius);
            int slotCenterY = centerY + (int) Math.round(Math.sin(angle) * slotRadius);
            int iconX = slotCenterX - 8;
            int iconY = slotCenterY - 8;
            boolean selected = i == this.hoveredIndex;

            int slotHalfSize = Math.round(16 + hoverEase * 5.0F);
            int slotColor = selected ? color(SELECTED_RGB, 0.82F * easedAlpha) : color(SLOT_RGB, 0.86F * easedAlpha);
            context.fill(slotCenterX - slotHalfSize, slotCenterY - slotHalfSize, slotCenterX + slotHalfSize, slotCenterY + slotHalfSize, slotColor);
            this.drawBorder(
                    context,
                    slotCenterX - slotHalfSize,
                    slotCenterY - slotHalfSize,
                    slotHalfSize * 2,
                    slotHalfSize * 2,
                    color(0xFFFFFF, (0.18F + hoverEase * 0.42F) * easedAlpha)
            );

            float iconScale = 1.0F + hoverEase * 0.18F;
            context.getMatrices().pushMatrix();
            context.getMatrices().scaleAround(iconScale, iconScale, slotCenterX, slotCenterY);
            context.drawItem(shortcut.iconStack(), iconX, iconY);
            context.getMatrices().popMatrix();
        }

        String centerLabel = this.hoveredIndex >= 0 ? this.shortcuts.get(this.hoveredIndex).label() : "InventoryShortcuts";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(centerLabel), centerX, centerY - 5, color(0xFFFFFF, easedAlpha));
        if (this.hoveredIndex >= 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.shortcuts.get(this.hoveredIndex).command()), centerX, centerY + 9, color(0xB0B0B0, easedAlpha));
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private int findHoveredShortcut(int mouseX, int mouseY) {
        if (this.shortcuts.isEmpty()) {
            return -1;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int radius = this.getAnimatedRadius();
        int bestIndex = -1;
        double bestDistance = 34.0D;

        for (int i = 0; i < this.shortcuts.size(); i++) {
            double angle = this.getSlotAngle(i, this.shortcuts.size());
            double slotX = centerX + Math.cos(angle) * radius;
            double slotY = centerY + Math.sin(angle) * radius;
            double distance = Math.hypot(mouseX - slotX, mouseY - slotY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private double getSlotAngle(int index, int count) {
        return -Math.PI / 2.0D + (Math.PI * 2.0D * index) / Math.max(1, count);
    }

    private int getAnimatedRadius() {
        int baseRadius = Math.max(48, Math.min(112, Math.min(this.width, this.height) / 4));
        return Math.round(baseRadius * (0.86F + smoothStep(this.alpha) * 0.14F));
    }

    private void drawFilledCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt(radiusSquared - y * y);
            context.fill(centerX - halfWidth, centerY + y, centerX + halfWidth + 1, centerY + y + 1, color);
        }
    }

    private void drawMotionBlur(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, int radius, float easedAlpha, float blurStrength) {
        if (blurStrength <= 0.02F) {
            return;
        }

        int direction = this.closing ? 1 : -1;
        for (int layer = 3; layer >= 1; layer--) {
            int offset = direction * layer * 4;
            float layerAlpha = blurStrength * easedAlpha * (0.11F / layer);
            int layerInner = Math.max(8, innerRadius + offset);
            int layerOuter = Math.max(layerInner + 2, outerRadius + offset);
            this.drawRing(context, centerX, centerY, layerInner, layerOuter, color(RING_EDGE_RGB, layerAlpha));
        }

        this.drawSlotMotionBlur(context, centerX, centerY, radius, easedAlpha, blurStrength, direction);
    }

    private void drawSlotMotionBlur(DrawContext context, int centerX, int centerY, int radius, float easedAlpha, float blurStrength, int direction) {
        for (int i = 0; i < this.shortcuts.size(); i++) {
            double angle = this.getSlotAngle(i, this.shortcuts.size());
            float hoverEase = smoothStep(this.hoverAnimations[i]);
            boolean selected = i == this.hoveredIndex;
            int rgb = selected ? SELECTED_RGB : SLOT_RGB;

            for (int layer = 2; layer >= 1; layer--) {
                double slotRadius = radius + hoverEase * 7.0D + direction * layer * 6.0D;
                int slotCenterX = centerX + (int) Math.round(Math.cos(angle) * slotRadius);
                int slotCenterY = centerY + (int) Math.round(Math.sin(angle) * slotRadius);
                int slotHalfSize = Math.round(16 + hoverEase * 5.0F + layer * 2.0F);
                float layerAlpha = blurStrength * easedAlpha * (selected ? 0.18F : 0.11F) / layer;

                context.fill(
                        slotCenterX - slotHalfSize,
                        slotCenterY - slotHalfSize,
                        slotCenterX + slotHalfSize,
                        slotCenterY + slotHalfSize,
                        color(rgb, layerAlpha)
                );
            }
        }
    }

    private void drawRing(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, int color) {
        int outerSquared = outerRadius * outerRadius;
        int innerSquared = innerRadius * innerRadius;
        for (int y = -outerRadius; y <= outerRadius; y++) {
            int outerHalfWidth = (int) Math.sqrt(outerSquared - y * y);
            if (Math.abs(y) > innerRadius) {
                context.fill(centerX - outerHalfWidth, centerY + y, centerX + outerHalfWidth + 1, centerY + y + 1, color);
                continue;
            }

            int innerHalfWidth = (int) Math.sqrt(innerSquared - y * y);
            context.fill(centerX - outerHalfWidth, centerY + y, centerX - innerHalfWidth, centerY + y + 1, color);
            context.fill(centerX + innerHalfWidth + 1, centerY + y, centerX + outerHalfWidth + 1, centerY + y + 1, color);
        }
    }

    private void drawArcSegment(DrawContext context, int centerX, int centerY, int innerRadius, int outerRadius, double centerAngle, double halfAngle, int color) {
        int outerSquared = outerRadius * outerRadius;
        int innerSquared = innerRadius * innerRadius;
        for (int y = -outerRadius; y <= outerRadius; y++) {
            int startX = Integer.MIN_VALUE;
            for (int x = -outerRadius; x <= outerRadius; x++) {
                int distanceSquared = x * x + y * y;
                boolean inside = distanceSquared >= innerSquared
                        && distanceSquared <= outerSquared
                        && Math.abs(angleDifference(Math.atan2(y, x), centerAngle)) <= halfAngle;
                if (inside && startX == Integer.MIN_VALUE) {
                    startX = x;
                } else if (!inside && startX != Integer.MIN_VALUE) {
                    context.fill(centerX + startX, centerY + y, centerX + x, centerY + y + 1, color);
                    startX = Integer.MIN_VALUE;
                }
            }

            if (startX != Integer.MIN_VALUE) {
                context.fill(centerX + startX, centerY + y, centerX + outerRadius + 1, centerY + y + 1, color);
            }
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void executeHoveredShortcut() {
        if (this.executed || this.hoveredIndex < 0 || this.hoveredIndex >= this.shortcuts.size()) {
            return;
        }

        this.executed = true;
        ShortcutExecutor.execute(this.shortcuts.get(this.hoveredIndex).command());
    }

    private void updateHoverAnimations(float frameDelta) {
        float easing = 1.0F - (float) Math.exp(-HOVER_RESPONSE * frameDelta);
        for (int i = 0; i < this.hoverAnimations.length; i++) {
            float target = i == this.hoveredIndex ? 1.0F : 0.0F;
            this.hoverAnimations[i] += (target - this.hoverAnimations[i]) * easing;
            if (Math.abs(target - this.hoverAnimations[i]) < 0.01F) {
                this.hoverAnimations[i] = target;
            }
        }
    }

    private float updateFrameAnimation() {
        long currentTime = System.nanoTime();
        float frameDelta;
        if (this.lastFrameTimeNanos < 0L) {
            frameDelta = 1.0F / 60.0F;
        } else {
            frameDelta = Math.min(0.05F, (currentTime - this.lastFrameTimeNanos) / 1_000_000_000.0F);
        }
        this.lastFrameTimeNanos = currentTime;

        float previousAlpha = this.alpha;
        float target = this.closing ? 0.0F : 1.0F;
        float speed = this.closing ? FADE_OUT_SPEED : FADE_IN_SPEED;
        this.alpha = approach(this.alpha, target, speed * frameDelta);
        this.alphaVelocity = this.alpha - previousAlpha;

        if (this.closing && this.alpha <= 0.0F && this.client != null) {
            this.client.setScreen(null);
        }

        return frameDelta;
    }

    private boolean isHoldKeyDown() {
        if (this.client == null || this.client.getWindow() == null || this.holdKey.isUnbound()) {
            return false;
        }

        InputUtil.Key boundKey = InputUtil.fromTranslationKey(this.holdKey.getBoundKeyTranslationKey());
        if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), boundKey.getCode()) == GLFW.GLFW_PRESS;
        }

        return InputUtil.isKeyPressed(this.client.getWindow(), boundKey.getCode());
    }

    private void keepCursorUnlocked() {
        if (this.client != null && this.client.mouse.isCursorLocked()) {
            this.client.mouse.unlockCursor();
        }
    }

    private static int color(int rgb, float alpha) {
        int alphaByte = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return alphaByte << 24 | rgb & 0xFFFFFF;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float approach(float value, float target, float maxDelta) {
        if (value < target) {
            return Math.min(target, value + maxDelta);
        }

        return Math.max(target, value - maxDelta);
    }

    private static double angleDifference(double angle, double targetAngle) {
        return Math.atan2(Math.sin(angle - targetAngle), Math.cos(angle - targetAngle));
    }
}
