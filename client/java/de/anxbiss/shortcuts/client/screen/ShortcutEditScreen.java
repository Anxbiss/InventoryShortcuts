package de.anxbiss.shortcuts.client.screen;

import de.anxbiss.shortcuts.client.config.ShortcutConfig;
import de.anxbiss.shortcuts.client.config.ShortcutConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class ShortcutEditScreen extends Screen {
    private static final int FIELD_WIDTH = 300;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_GAP = 44;
    private static final int OVERLAY_COLOR = 0xA0000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFB0B0B0;
    private static final int ERROR_COLOR = 0xFFFF7070;
    private static final int SUCCESS_COLOR = 0xFF90EE90;

    private final Screen parent;
    private final ShortcutConfig.ShortcutEntry entry;
    private final Runnable onCloseCallback;

    private boolean syncingWidgets;
    private ButtonWidget enabledButton;
    private TextFieldWidget labelField;
    private TextFieldWidget commandField;
    private TextFieldWidget itemField;

    public ShortcutEditScreen(Screen parent, ShortcutConfig.ShortcutEntry entry, Runnable onCloseCallback) {
        super(Text.literal("Edit Shortcut"));
        this.parent = parent;
        this.entry = entry;
        this.onCloseCallback = onCloseCallback;
        this.entry.normalize();
    }

    @Override
    protected void init() {
        int x = (this.width - FIELD_WIDTH) / 2;
        int startY = 76;

        this.enabledButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Shortcut Active"), button -> {
                            this.entry.enabled = !this.entry.enabled;
                            this.syncWidgetsFromEntry();
                        })
                        .dimensions(x, startY, FIELD_WIDTH, FIELD_HEIGHT)
                        .build()
        );

        this.labelField = this.createTextField(x, startY + ROW_GAP, "Label", 48, value -> this.entry.label = value);
        this.commandField = this.createTextField(x, startY + ROW_GAP * 2, "Command", 128, value -> this.entry.command = value);
        this.itemField = this.createTextField(x, startY + ROW_GAP * 3, "Item-ID", 128, value -> this.entry.item = value);

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                        .dimensions((this.width - 400) / 2, this.height - 28, 400, FIELD_HEIGHT)
                        .build()
        );

        this.syncWidgetsFromEntry();
    }

    @Override
    public void close() {
        this.entry.normalize();
        this.onCloseCallback.run();
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
        super.render(context, mouseX, mouseY, deltaTicks);

        int x = (this.width - FIELD_WIDTH) / 2;
        int startY = 76;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 24, TEXT_COLOR);
        context.drawText(this.textRenderer, Text.literal("Label"), x, startY + ROW_GAP - 12, TEXT_COLOR, false);
        context.drawText(this.textRenderer, Text.literal("Command"), x, startY + ROW_GAP * 2 - 12, TEXT_COLOR, false);
        context.drawText(this.textRenderer, Text.literal("Item-ID"), x, startY + ROW_GAP * 3 - 12, TEXT_COLOR, false);

        this.renderPreview(context, x, startY + ROW_GAP * 4 + 8);
    }

    private TextFieldWidget createTextField(int x, int y, String placeholder, int maxLength, java.util.function.Consumer<String> changeListener) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, FIELD_WIDTH, FIELD_HEIGHT, Text.literal(placeholder));
        field.setMaxLength(maxLength);
        field.setPlaceholder(Text.literal(placeholder));
        field.setChangedListener(value -> {
            if (!this.syncingWidgets) {
                changeListener.accept(value);
            }
        });
        this.addDrawableChild(field);
        return field;
    }

    private void syncWidgetsFromEntry() {
        this.syncingWidgets = true;
        this.enabledButton.setMessage(onOffText("Shortcut Active", this.entry.enabled));
        this.labelField.setText(safeString(this.entry.label));
        this.commandField.setText(safeString(this.entry.command));
        this.itemField.setText(safeString(this.entry.item));
        this.syncingWidgets = false;
    }

    private void renderPreview(DrawContext context, int x, int y) {
        int previewHeight = 58;
        context.fill(x, y, x + FIELD_WIDTH, y + previewHeight, 0x66000000);
        context.drawStrokedRectangle(x, y, FIELD_WIDTH, previewHeight, 0x60FFFFFF);

        ShortcutConfigManager.ItemPreview preview = ShortcutConfigManager.resolveItemPreview(this.entry.item);
        ItemStack previewStack = preview.stack();
        context.fill(x + 10, y + 10, x + 30, y + 30, preview.valid() ? 0x663CB371 : 0x66C84B4B);
        context.drawItem(previewStack, x + 12, y + 12);

        String previewLabel = safeString(this.entry.label).isBlank() ? fallbackLabel(this.entry.command) : safeString(this.entry.label).trim();
        String previewCommand = safeString(this.entry.command).trim();
        context.drawTextWithShadow(this.textRenderer, Text.literal(previewLabel.isBlank() ? "(ohne Label)" : previewLabel), x + 38, y + 12, TEXT_COLOR);
        context.drawText(this.textRenderer, Text.literal(previewCommand.isBlank() ? "(ohne Command)" : previewCommand), x + 38, y + 27, MUTED_COLOR, false);
        context.drawText(this.textRenderer, Text.literal(preview.valid() ? "Item ok" : "Item-ID ungueltig"), x + 38, y + 42, preview.valid() ? SUCCESS_COLOR : ERROR_COLOR, false);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String fallbackLabel(String command) {
        String normalizedCommand = safeString(command).trim();
        return normalizedCommand.startsWith("/") ? normalizedCommand.substring(1) : normalizedCommand;
    }

    private static Text onOffText(String label, boolean value) {
        return Text.literal(label + ": " + (value ? "ON" : "OFF"));
    }
}
