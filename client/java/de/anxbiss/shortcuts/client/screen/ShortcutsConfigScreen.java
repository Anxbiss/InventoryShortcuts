package de.anxbiss.shortcuts.client.screen;

import de.anxbiss.shortcuts.client.config.ShortcutConfig;
import de.anxbiss.shortcuts.client.config.ShortcutConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.io.IOException;
import java.util.List;

public final class ShortcutsConfigScreen extends Screen {
    private static final int DEFAULT_OPTION_WIDTH = 220;
    private static final int COMPACT_OPTION_WIDTH = 220;
    private static final int OPTION_HEIGHT = 20;
    private static final int DEFAULT_COLUMN_GAP = 34;
    private static final int COMPACT_COLUMN_GAP = 18;
    private static final int DEFAULT_ROW_GAP = 24;
    private static final int COMPACT_ROW_GAP = 22;
    private static final int SLOT_BUTTON_COUNT = ShortcutConfig.MAX_SHORTCUTS_PER_ROW;
    private static final int DEFAULT_SLOT_BUTTON_WIDTH = 106;
    private static final int OVERLAY_COLOR = 0xA0000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFB0B0B0;
    private static final int ERROR_COLOR = 0xFFFF7070;
    private static final int SUCCESS_COLOR = 0xFF90EE90;

    private final Screen parent;

    private ShortcutConfig workingCopy;
    private ShortcutRow selectedRow = ShortcutRow.TOP;
    private ConfigPage selectedPage = ConfigPage.GENERAL;
    private int selectedIndex;
    private boolean syncingWidgets;

    private ButtonWidget pageButton;
    private ButtonWidget modEnabledButton;
    private ButtonWidget inventoryButton;
    private ButtonWidget tooltipButton;
    private ButtonWidget radialMenuButton;
    private ButtonWidget moonButton;
    private ButtonWidget weatherButton;
    private ButtonWidget rowButton;
    private ButtonWidget shortcutEnabledButton;
    private ButtonWidget editShortcutButton;
    private ButtonWidget newShortcutButton;
    private ButtonWidget deleteShortcutButton;
    private ButtonWidget moveShortcutLeftButton;
    private ButtonWidget moveShortcutRightButton;
    private ButtonWidget resetDefaultsButton;
    private ButtonWidget spacingButton;
    private ButtonWidget topMarginButton;
    private ButtonWidget bottomMarginButton;
    private final List<ButtonWidget> shortcutSlotButtons = new ArrayList<>();

    private boolean compactLayout;
    private boolean ultraCompactLayout;
    private int optionWidth;
    private int columnGap;
    private int rowGap;
    private int slotButtonWidth;
    private int layoutLeftX;
    private int layoutRightX;
    private int startY;

    private Text statusMessage = Text.literal("");
    private int statusColor = SUCCESS_COLOR;

    public ShortcutsConfigScreen(Screen parent) {
        super(Text.literal("InventoryShortcuts"));
        this.parent = parent;
        this.workingCopy = ShortcutConfigManager.getConfig().copy();
        this.workingCopy.normalize();
    }

    @Override
    protected void init() {
        this.buildWidgets();
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
        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.compactLayout ? 10 : 24, TEXT_COLOR);
        if (this.ultraCompactLayout) {
            this.drawSectionTitle(context, this.selectedPage == ConfigPage.GENERAL ? "General" : "Shortcuts", this.layoutLeftX, this.startY - 12);
        } else
        if (this.compactLayout) {
            this.drawSectionTitle(context, "General", this.layoutLeftX, this.startY - 12);
            this.drawSectionTitle(context, "HUD", this.layoutRightX, this.startY - 12);
            this.drawSectionTitle(context, "Inventory", this.layoutLeftX, this.startY + this.rowGap * 3 + 10);
            this.drawSectionTitle(context, "Shortcuts", this.layoutRightX, this.startY + this.rowGap * 3 + 10);
        } else {
            this.drawSectionTitle(context, "General", this.layoutLeftX, 55);
            this.drawSectionTitle(context, "HUD", this.layoutLeftX, 151);
            this.drawSectionTitle(context, "Inventory Buttons", this.layoutLeftX, 247);
            this.drawSectionTitle(context, "Shortcuts", this.layoutRightX, 55);
        }
        if (!this.statusMessage.getString().isEmpty() && !this.compactLayout) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.statusMessage, this.width / 2, this.height - 52, this.statusColor);
        } else if (!this.compactLayout && this.height >= 420) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("JSON: config/inventoryshortcuts.json"), this.width / 2, this.height - 52, MUTED_COLOR);
        }
    }

    private void buildWidgets() {
        this.updateLayout();
        this.pageButton = null;
        this.modEnabledButton = null;
        this.inventoryButton = null;
        this.tooltipButton = null;
        this.radialMenuButton = null;
        this.moonButton = null;
        this.weatherButton = null;
        this.rowButton = null;
        this.shortcutEnabledButton = null;
        this.editShortcutButton = null;
        this.newShortcutButton = null;
        this.deleteShortcutButton = null;
        this.moveShortcutLeftButton = null;
        this.moveShortcutRightButton = null;
        this.resetDefaultsButton = null;
        this.spacingButton = null;
        this.topMarginButton = null;
        this.bottomMarginButton = null;
        this.shortcutSlotButtons.clear();
        int leftX = this.layoutLeftX;
        int rightX = this.layoutRightX;

        if (this.ultraCompactLayout) {
            this.pageButton = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Page"), button -> {
                                this.selectedPage = this.selectedPage == ConfigPage.GENERAL ? ConfigPage.SHORTCUTS : ConfigPage.GENERAL;
                                this.clearAndInit();
                            })
                            .dimensions(leftX, 28, this.optionWidth, OPTION_HEIGHT)
                            .build()
            );

            if (this.selectedPage == ConfigPage.GENERAL) {
                this.buildGeneralWidgets(leftX, rightX, this.startY);
                this.buildInventoryWidgets(leftX, this.startY + this.rowGap * 4);
                this.buildResetButton(rightX, this.startY + this.rowGap * 3);
            } else {
                this.buildShortcutWidgets(leftX, this.startY, true);
            }

            this.buildDoneButton();
            this.syncWidgetsFromConfig();
            return;
        }

        this.buildGeneralWidgets(leftX, this.compactLayout ? rightX : leftX, this.startY);
        int inventoryY = this.compactLayout ? this.startY + this.rowGap * 4 : this.startY + this.rowGap * 9;
        this.buildInventoryWidgets(leftX, inventoryY);
        this.buildShortcutWidgets(rightX, this.compactLayout ? this.startY + this.rowGap * 4 : this.startY, false);
        this.buildResetButton(this.compactLayout ? leftX : rightX, this.compactLayout ? inventoryY + this.rowGap * 3 : this.height - 76);
        this.buildDoneButton();
        this.syncWidgetsFromConfig();
    }

    private void buildGeneralWidgets(int generalX, int hudX, int startY) {
        this.modEnabledButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Mod Status"), button -> {
                            this.workingCopy.enabled = !this.workingCopy.enabled;
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(generalX, startY, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.inventoryButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Inventory Preview"), button -> {
                            this.workingCopy.showInInventory = !this.workingCopy.showInInventory;
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(generalX, startY + this.rowGap, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.tooltipButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Tooltips"), button -> {
                            this.workingCopy.showTooltips = !this.workingCopy.showTooltips;
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(generalX, startY + this.rowGap * 2, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.radialMenuButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Radial Menu"), button -> {
                            this.workingCopy.radialMenuEnabled = !this.workingCopy.radialMenuEnabled;
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(generalX, startY + this.rowGap * 3, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        int hudY = this.ultraCompactLayout ? startY + this.rowGap * 4 : (this.compactLayout ? startY : startY + this.rowGap * 4);
        this.moonButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Moon Display"), button -> {
                            this.workingCopy.showMoonDisplay = !this.workingCopy.showMoonDisplay;
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(hudX, hudY, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.weatherButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Weather Display"), button -> {
                            this.workingCopy.showWeatherDisplay = !this.workingCopy.showWeatherDisplay;
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(hudX, hudY + this.rowGap, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("HUD Layout"), button -> {
                            if (this.client != null) {
                                this.client.setScreen(new HudLayoutScreen(this, this.workingCopy));
                            }
                        })
                        .dimensions(hudX, hudY + this.rowGap * 2, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );
    }

    private void buildShortcutWidgets(int rightX, int shortcutsY, boolean singleColumn) {
        this.rowButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Row"), button -> {
                            if (!this.syncingWidgets) {
                                this.selectedRow = this.selectedRow.next();
                                this.clampSelectedIndex();
                                this.clearStatus();
                                this.syncWidgetsFromConfig();
                            }
                        })
                        .dimensions(rightX, shortcutsY, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.shortcutSlotButtons.clear();
        int slotStartY = shortcutsY + this.rowGap;
        int slotColumns = 2;
        int slotRows = 4;
        int slotWidth = singleColumn ? Math.max(70, (this.optionWidth - 8) / 2) : this.slotButtonWidth;
        for (int i = 0; i < SLOT_BUTTON_COUNT; i++) {
            int slotIndex = i;
            int slotX = rightX + (i % slotColumns) * (slotWidth + 8);
            int slotY = slotStartY + (i / slotColumns) * this.rowGap;
            ButtonWidget slotButton = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Slot"), button -> this.selectSlot(slotIndex))
                            .dimensions(slotX, slotY, slotWidth, OPTION_HEIGHT)
                            .build()
            );
            this.shortcutSlotButtons.add(slotButton);
        }

        int shortcutActionsY = slotStartY + this.rowGap * slotRows;
        int shortcutActionWidth = (this.compactLayout || this.ultraCompactLayout) ? Math.max(70, (this.optionWidth - 8) / 2) : this.optionWidth;
        int shortcutActionSecondX = (this.compactLayout || this.ultraCompactLayout) ? rightX + shortcutActionWidth + 8 : rightX;
        this.shortcutEnabledButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Selected Active"), button -> {
                            ShortcutConfig.ShortcutEntry currentEntry = this.getCurrentEntry();
                            if (currentEntry != null) {
                                currentEntry.enabled = !currentEntry.enabled;
                                this.syncWidgetsFromConfig();
                            }
                        })
                        .dimensions(rightX, shortcutActionsY, shortcutActionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.editShortcutButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Edit Shortcut"), button -> {
                            if (this.client != null && this.getCurrentEntry() != null) {
                                this.client.setScreen(new ShortcutEditScreen(this, this.getCurrentEntry(), this::syncWidgetsFromConfig));
                            }
                        })
                        .dimensions(shortcutActionSecondX, this.compactLayout ? shortcutActionsY : shortcutActionsY + this.rowGap, shortcutActionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.newShortcutButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("New Shortcut"), button -> this.addShortcut())
                        .dimensions(rightX, this.compactLayout ? shortcutActionsY + this.rowGap : shortcutActionsY + this.rowGap * 2, shortcutActionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.deleteShortcutButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Delete Shortcut"), button -> this.removeShortcut())
                        .dimensions(shortcutActionSecondX, this.compactLayout ? shortcutActionsY + this.rowGap : shortcutActionsY + this.rowGap * 3, shortcutActionWidth, OPTION_HEIGHT)
                        .build()
        );

        int moveY = this.compactLayout || this.ultraCompactLayout ? shortcutActionsY + this.rowGap * 2 : shortcutActionsY + this.rowGap * 4;
        int moveButtonWidth = this.compactLayout || this.ultraCompactLayout ? shortcutActionWidth : Math.max(70, (this.optionWidth - 8) / 2);
        int moveSecondX = this.compactLayout || this.ultraCompactLayout ? shortcutActionSecondX : rightX + moveButtonWidth + 8;
        this.moveShortcutLeftButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("< Move"), button -> this.moveShortcut(-1))
                        .dimensions(rightX, moveY, moveButtonWidth, OPTION_HEIGHT)
                        .build()
        );

        this.moveShortcutRightButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Move >"), button -> this.moveShortcut(1))
                        .dimensions(moveSecondX, moveY, moveButtonWidth, OPTION_HEIGHT)
                        .build()
        );
    }

    private void buildInventoryWidgets(int leftX, int inventoryY) {
        this.spacingButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Button Spacing"), button -> {
                            this.workingCopy.buttonSpacing = cycleValue(this.workingCopy.buttonSpacing, 0, 12);
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(leftX, inventoryY, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.topMarginButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Top Bar Offset"), button -> {
                            this.workingCopy.topMargin = cycleValue(this.workingCopy.topMargin, 20, 80);
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(leftX, inventoryY + this.rowGap, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );

        this.bottomMarginButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Bottom Bar Offset"), button -> {
                            this.workingCopy.bottomMargin = cycleValue(this.workingCopy.bottomMargin, 0, 40);
                            this.syncWidgetsFromConfig();
                        })
                        .dimensions(leftX, inventoryY + this.rowGap * 2, this.optionWidth, OPTION_HEIGHT)
                        .build()
        );
    }

    private void buildResetButton(int x, int y) {
        this.resetDefaultsButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reset Defaults"), button -> this.resetToDefaults())
                        .dimensions(x, Math.min(y, this.height - 52), this.optionWidth, OPTION_HEIGHT)
                        .build()
        );
    }

    private void buildDoneButton() {
        int doneWidth = Math.min(400, Math.max(160, this.width - 48));
        int doneX = (this.width - doneWidth) / 2;
        int doneY = this.height - 28;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Done"), button -> this.saveAndClose())
                        .dimensions(doneX, doneY, doneWidth, OPTION_HEIGHT)
                        .build()
        );

        this.syncWidgetsFromConfig();
    }

    private void syncWidgetsFromConfig() {
        this.workingCopy.normalize();
        this.clampSelectedIndex();

        this.syncingWidgets = true;
        if (this.pageButton != null) {
            this.pageButton.setMessage(Text.literal("Page: " + this.selectedPage.label));
        }
        if (this.modEnabledButton != null) {
            this.modEnabledButton.setMessage(onOffText("Mod Status", this.workingCopy.enabled));
        }
        if (this.inventoryButton != null) {
            this.inventoryButton.setMessage(onOffText("Inventory Preview", this.workingCopy.showInInventory));
        }
        if (this.tooltipButton != null) {
            this.tooltipButton.setMessage(onOffText("Tooltips", this.workingCopy.showTooltips));
        }
        if (this.radialMenuButton != null) {
            this.radialMenuButton.setMessage(onOffText("Radial Menu", this.workingCopy.radialMenuEnabled));
        }
        if (this.moonButton != null) {
            this.moonButton.setMessage(onOffText("Moon Display", this.workingCopy.showMoonDisplay));
        }
        if (this.weatherButton != null) {
            this.weatherButton.setMessage(onOffText("Weather Display", this.workingCopy.showWeatherDisplay));
        }
        if (this.rowButton != null) {
            this.rowButton.setMessage(Text.literal("Row: " + this.selectedRow.getLabel().getString()));
        }

        ShortcutConfig.ShortcutEntry currentEntry = this.getCurrentEntry();
        boolean hasEntry = currentEntry != null;
        if (this.shortcutEnabledButton != null) {
            this.shortcutEnabledButton.active = hasEntry;
            this.shortcutEnabledButton.setMessage(onOffText(this.compactLayout ? "Active" : "Selected Active", hasEntry && currentEntry.enabled));
        }

        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        for (int i = 0; i < this.shortcutSlotButtons.size(); i++) {
            ButtonWidget slotButton = this.shortcutSlotButtons.get(i);
            boolean hasSlot = i < entries.size();
            slotButton.active = hasSlot;
            if (hasSlot) {
                String prefix = i == this.selectedIndex ? "> " : "";
                slotButton.setMessage(Text.literal(prefix + getEntryLabel(entries.get(i), i)));
            } else {
                slotButton.setMessage(Text.literal("-"));
            }
        }

        if (this.editShortcutButton != null) {
            this.editShortcutButton.active = hasEntry;
            this.editShortcutButton.setMessage(Text.literal(this.compactLayout ? "Edit" : "Edit Shortcut"));
        }
        if (this.newShortcutButton != null) {
            boolean canAdd = entries.size() < ShortcutConfig.MAX_SHORTCUTS_PER_ROW;
            this.newShortcutButton.active = canAdd;
            this.newShortcutButton.setMessage(Text.literal(canAdd ? (this.compactLayout ? "New" : "New Shortcut") : "Max Shortcuts"));
        }
        if (this.deleteShortcutButton != null) {
            this.deleteShortcutButton.active = hasEntry;
            this.deleteShortcutButton.setMessage(Text.literal(this.compactLayout ? "Delete" : "Delete Shortcut"));
        }
        boolean canMove = entries.size() > 1;
        if (this.moveShortcutLeftButton != null) {
            this.moveShortcutLeftButton.active = canMove;
            this.moveShortcutLeftButton.setMessage(Text.literal("< Move"));
        }
        if (this.moveShortcutRightButton != null) {
            this.moveShortcutRightButton.active = canMove;
            this.moveShortcutRightButton.setMessage(Text.literal("Move >"));
        }
        if (this.resetDefaultsButton != null) {
            this.resetDefaultsButton.setMessage(Text.literal(this.compactLayout ? "Reset" : "Reset Defaults"));
        }
        if (this.spacingButton != null) {
            this.spacingButton.setMessage(Text.literal("Button Spacing: " + this.workingCopy.buttonSpacing));
        }
        if (this.topMarginButton != null) {
            this.topMarginButton.setMessage(Text.literal("Top Bar Offset: " + this.workingCopy.topMargin));
        }
        if (this.bottomMarginButton != null) {
            this.bottomMarginButton.setMessage(Text.literal("Bottom Bar Offset: " + this.workingCopy.bottomMargin));
        }
        this.syncingWidgets = false;
    }

    private void selectSlot(int index) {
        if (index >= this.getSelectedEntries().size()) {
            return;
        }

        this.selectedIndex = index;
        this.clearStatus();
        this.syncWidgetsFromConfig();
    }

    private void changeSlot(int delta) {
        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        if (entries.isEmpty()) {
            return;
        }

        this.selectedIndex = Math.floorMod(this.selectedIndex + delta, entries.size());
        this.clearStatus();
        this.syncWidgetsFromConfig();
    }

    private void addShortcut() {
        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        if (entries.size() >= ShortcutConfig.MAX_SHORTCUTS_PER_ROW) {
            this.statusMessage = Text.literal("Maximum of 8 shortcuts reached.");
            this.statusColor = ERROR_COLOR;
            this.syncWidgetsFromConfig();
            return;
        }

        entries.add(new ShortcutConfig.ShortcutEntry("New Button", "/help", "minecraft:paper"));
        this.selectedIndex = entries.size() - 1;
        this.statusMessage = Text.literal("Added new shortcut.");
        this.statusColor = SUCCESS_COLOR;
        this.syncWidgetsFromConfig();
    }

    private void removeShortcut() {
        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        if (entries.isEmpty()) {
            return;
        }

        entries.remove(this.selectedIndex);
        if (this.selectedIndex >= entries.size()) {
            this.selectedIndex = Math.max(0, entries.size() - 1);
        }

        this.statusMessage = Text.literal("Shortcut removed.");
        this.statusColor = SUCCESS_COLOR;
        this.syncWidgetsFromConfig();
    }

    private void moveShortcut(int delta) {
        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        if (entries.size() < 2) {
            return;
        }

        int targetIndex = Math.floorMod(this.selectedIndex + delta, entries.size());
        ShortcutConfig.ShortcutEntry entry = entries.remove(this.selectedIndex);
        entries.add(targetIndex, entry);
        this.selectedIndex = targetIndex;
        this.statusMessage = Text.literal("Shortcut moved.");
        this.statusColor = SUCCESS_COLOR;
        this.syncWidgetsFromConfig();
    }

    private void resetToDefaults() {
        this.workingCopy = ShortcutConfig.createDefault();
        this.selectedRow = ShortcutRow.TOP;
        this.selectedIndex = 0;
        this.statusMessage = Text.literal("Loaded default settings.");
        this.statusColor = SUCCESS_COLOR;
        this.clearAndInit();
    }

    private void saveAndClose() {
        try {
            this.workingCopy.normalize();
            ShortcutConfigManager.saveConfig(this.workingCopy);
            this.close();
        } catch (IOException exception) {
            this.statusMessage = Text.literal("Config could not be saved.");
            this.statusColor = ERROR_COLOR;
        }
    }

    private void clampSelectedIndex() {
        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        if (entries.isEmpty()) {
            this.selectedIndex = 0;
            return;
        }

        if (this.selectedIndex < 0) {
            this.selectedIndex = 0;
        } else if (this.selectedIndex >= entries.size()) {
            this.selectedIndex = entries.size() - 1;
        }
    }

    private String getSlotLabel() {
        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        if (entries.isEmpty()) {
            return "None";
        }

        ShortcutConfig.ShortcutEntry currentEntry = entries.get(this.selectedIndex);
        String label = safeString(currentEntry.label).isBlank() ? fallbackLabel(currentEntry.command) : currentEntry.label;
        if (label.isBlank()) {
            label = "Slot " + (this.selectedIndex + 1);
        }

        return (this.selectedIndex + 1) + "/" + entries.size() + " " + label;
    }

    private static String getEntryLabel(ShortcutConfig.ShortcutEntry entry, int index) {
        String label = safeString(entry.label).isBlank() ? fallbackLabel(entry.command) : entry.label;
        if (label.isBlank()) {
            label = "Slot " + (index + 1);
        }

        return (index + 1) + ". " + label;
    }

    private void updateLayout() {
        this.ultraCompactLayout = this.height < 300;
        this.compactLayout = this.height < 390;
        this.optionWidth = Math.min(this.compactLayout ? COMPACT_OPTION_WIDTH : DEFAULT_OPTION_WIDTH, Math.max(130, (this.width - 48 - DEFAULT_COLUMN_GAP) / 2));
        this.columnGap = this.compactLayout ? COMPACT_COLUMN_GAP : DEFAULT_COLUMN_GAP;
        this.rowGap = this.compactLayout ? COMPACT_ROW_GAP : DEFAULT_ROW_GAP;
        this.slotButtonWidth = this.compactLayout ? Math.max(70, (this.optionWidth - 8) / 2) : DEFAULT_SLOT_BUTTON_WIDTH;
        int gridWidth = this.optionWidth * 2 + this.columnGap;
        this.layoutLeftX = Math.max(12, (this.width - gridWidth) / 2);
        this.layoutRightX = this.layoutLeftX + this.optionWidth + this.columnGap;
        this.startY = this.ultraCompactLayout ? 54 : (this.compactLayout ? 50 : 75);
    }

    private void drawSectionTitle(DrawContext context, String label, int x, int y) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), x, y, TEXT_COLOR);
    }

    private List<ShortcutConfig.ShortcutEntry> getSelectedEntries() {
        return switch (this.selectedRow) {
            case TOP -> this.workingCopy.topShortcuts;
            case BOTTOM -> this.workingCopy.bottomShortcuts;
            case RADIAL -> this.workingCopy.radialShortcuts;
        };
    }

    private ShortcutConfig.ShortcutEntry getCurrentEntry() {
        List<ShortcutConfig.ShortcutEntry> entries = this.getSelectedEntries();
        if (entries.isEmpty() || this.selectedIndex < 0 || this.selectedIndex >= entries.size()) {
            return null;
        }

        return entries.get(this.selectedIndex);
    }

    private void clearStatus() {
        this.statusMessage = Text.literal("");
        this.statusColor = SUCCESS_COLOR;
    }

    private static int cycleValue(int value, int min, int max) {
        if (isShiftDown()) {
            return value <= min ? max : value - 1;
        }

        return value >= max ? min : value + 1;
    }

    private static boolean isShiftDown() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        long handle = minecraft.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static Text onOffText(String label, boolean value) {
        return Text.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String fallbackLabel(String command) {
        String normalizedCommand = safeString(command).trim();
        return normalizedCommand.startsWith("/") ? normalizedCommand.substring(1) : normalizedCommand;
    }

    private enum ShortcutRow {
        TOP(Text.literal("Top")),
        BOTTOM(Text.literal("Bottom")),
        RADIAL(Text.literal("Radial"));

        private final Text label;

        ShortcutRow(Text label) {
            this.label = label;
        }

        public Text getLabel() {
            return this.label;
        }

        public ShortcutRow next() {
            return switch (this) {
                case TOP -> BOTTOM;
                case BOTTOM -> RADIAL;
                case RADIAL -> TOP;
            };
        }
    }

    private enum ConfigPage {
        GENERAL("General"),
        SHORTCUTS("Shortcuts");

        private final String label;

        ConfigPage(String label) {
            this.label = label;
        }
    }
}
