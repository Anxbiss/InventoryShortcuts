package de.anxbiss.shortcuts.client.config;

import java.util.ArrayList;
import java.util.List;

public final class ShortcutConfig {
    public static final int MAX_SHORTCUTS_PER_ROW = 8;

    public boolean enabled = true;
    public boolean showInInventory = true;
    public boolean showTooltips = true;
    public boolean radialMenuEnabled = true;
    public boolean showMoonDisplay = true;
    public boolean showWeatherDisplay = true;
    public Boolean showMoonWeather = null;
    public int buttonSize = 20;
    public int buttonSpacing = 4;
    public int topMargin = 24;
    public int bottomMargin = 4;
    public int moonDisplayX = -1;
    public int moonDisplayY = -1;
    public String moonDisplayAnchor = "BOTTOM_RIGHT";
    public int moonDisplayOffsetX = -1;
    public int moonDisplayOffsetY = -1;
    public float moonDisplayScale = 1.0F;
    public int weatherDisplayX = -1;
    public int weatherDisplayY = -1;
    public String weatherDisplayAnchor = "BOTTOM_RIGHT";
    public int weatherDisplayOffsetX = -1;
    public int weatherDisplayOffsetY = -1;
    public float weatherDisplayScale = 1.0F;
    public List<ShortcutEntry> topShortcuts = new ArrayList<>();
    public List<ShortcutEntry> bottomShortcuts = new ArrayList<>();
    public List<ShortcutEntry> radialShortcuts = new ArrayList<>();

    public static ShortcutConfig createDefault() {
        ShortcutConfig config = new ShortcutConfig();

        config.topShortcuts.add(new ShortcutEntry("WORKBENCH", "/wb", "minecraft:crafting_table"));
        config.topShortcuts.add(new ShortcutEntry("SPECTATE", "/spectate", "minecraft:eye_of_ender"));
        config.topShortcuts.add(new ShortcutEntry("Shop", "/shop", "minecraft:emerald"));
        config.topShortcuts.add(new ShortcutEntry("Enderchest", "/ec", "minecraft:ender_chest"));

        config.bottomShortcuts.add(new ShortcutEntry("Heal", "/heal", "minecraft:golden_apple"));
        config.bottomShortcuts.add(new ShortcutEntry("Feed", "/feed", "minecraft:cooked_beef"));
        config.bottomShortcuts.add(new ShortcutEntry("Kits", "/kit", "minecraft:diamond_sword"));
        config.bottomShortcuts.add(new ShortcutEntry("Repair", "/repair", "minecraft:anvil"));
        config.bottomShortcuts.add(new ShortcutEntry("Warp", "/warp", "minecraft:ender_pearl"));
        config.bottomShortcuts.add(new ShortcutEntry("Back", "/back", "minecraft:clock"));

        addDefaultRadialShortcuts(config.radialShortcuts);

        config.normalize();
        return config;
    }

    public ShortcutConfig copy() {
        ShortcutConfig copy = new ShortcutConfig();
        copy.enabled = this.enabled;
        copy.showInInventory = this.showInInventory;
        copy.showTooltips = this.showTooltips;
        copy.radialMenuEnabled = this.radialMenuEnabled;
        copy.buttonSize = this.buttonSize;
        copy.buttonSpacing = this.buttonSpacing;
        copy.topMargin = this.topMargin;
        copy.bottomMargin = this.bottomMargin;
        copy.showMoonDisplay = this.showMoonDisplay;
        copy.showWeatherDisplay = this.showWeatherDisplay;
        copy.showMoonWeather = this.showMoonWeather;
        copy.moonDisplayX = this.moonDisplayX;
        copy.moonDisplayY = this.moonDisplayY;
        copy.moonDisplayAnchor = this.moonDisplayAnchor;
        copy.moonDisplayOffsetX = this.moonDisplayOffsetX;
        copy.moonDisplayOffsetY = this.moonDisplayOffsetY;
        copy.moonDisplayScale = this.moonDisplayScale;
        copy.weatherDisplayX = this.weatherDisplayX;
        copy.weatherDisplayY = this.weatherDisplayY;
        copy.weatherDisplayAnchor = this.weatherDisplayAnchor;
        copy.weatherDisplayOffsetX = this.weatherDisplayOffsetX;
        copy.weatherDisplayOffsetY = this.weatherDisplayOffsetY;
        copy.weatherDisplayScale = this.weatherDisplayScale;

        this.topShortcuts.stream().map(ShortcutEntry::copy).forEach(copy.topShortcuts::add);
        this.bottomShortcuts.stream().map(ShortcutEntry::copy).forEach(copy.bottomShortcuts::add);
        this.radialShortcuts.stream().map(ShortcutEntry::copy).forEach(copy.radialShortcuts::add);
        return copy;
    }

    public void normalize() {
        if (this.showMoonWeather != null) {
            this.showMoonDisplay = this.showMoonWeather;
            this.showWeatherDisplay = this.showMoonWeather;
            this.showMoonWeather = null;
        }

        this.buttonSize = clamp(this.buttonSize, 20, 32);
        this.buttonSpacing = clamp(this.buttonSpacing, 0, 12);
        this.topMargin = clamp(this.topMargin, 20, 80);
        this.bottomMargin = clamp(this.bottomMargin, 0, 40);
        this.moonDisplayX = clamp(this.moonDisplayX, -1, 10000);
        this.moonDisplayY = clamp(this.moonDisplayY, -1, 10000);
        this.weatherDisplayX = clamp(this.weatherDisplayX, -1, 10000);
        this.weatherDisplayY = clamp(this.weatherDisplayY, -1, 10000);
        this.moonDisplayAnchor = normalizeAnchor(this.moonDisplayAnchor);
        this.weatherDisplayAnchor = normalizeAnchor(this.weatherDisplayAnchor);
        this.moonDisplayOffsetX = clamp(this.moonDisplayOffsetX, -1, 10000);
        this.moonDisplayOffsetY = clamp(this.moonDisplayOffsetY, -1, 10000);
        this.weatherDisplayOffsetX = clamp(this.weatherDisplayOffsetX, -1, 10000);
        this.weatherDisplayOffsetY = clamp(this.weatherDisplayOffsetY, -1, 10000);
        this.moonDisplayScale = clamp(this.moonDisplayScale, 0.5F, 3.0F);
        this.weatherDisplayScale = clamp(this.weatherDisplayScale, 0.5F, 3.0F);

        if (this.topShortcuts == null) {
            this.topShortcuts = new ArrayList<>();
        }

        if (this.bottomShortcuts == null) {
            this.bottomShortcuts = new ArrayList<>();
        }

        if (this.radialShortcuts == null) {
            this.radialShortcuts = new ArrayList<>();
            addDefaultRadialShortcuts(this.radialShortcuts);
        }

        normalizeShortcutList(this.topShortcuts);
        normalizeShortcutList(this.bottomShortcuts);
        normalizeShortcutList(this.radialShortcuts);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeAnchor(String value) {
        if (value == null) {
            return "BOTTOM_RIGHT";
        }

        return switch (value.trim().toUpperCase()) {
            case "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT" -> value.trim().toUpperCase();
            default -> "BOTTOM_RIGHT";
        };
    }

    private static void addDefaultRadialShortcuts(List<ShortcutEntry> shortcuts) {
        shortcuts.add(new ShortcutEntry("WORKBENCH", "/wb", "minecraft:crafting_table"));
        shortcuts.add(new ShortcutEntry("SPECTATE", "/spectate", "minecraft:eye_of_ender"));
        shortcuts.add(new ShortcutEntry("Shop", "/shop", "minecraft:emerald"));
        shortcuts.add(new ShortcutEntry("Enderchest", "/ec", "minecraft:ender_chest"));
        shortcuts.add(new ShortcutEntry("Heal", "/heal", "minecraft:golden_apple"));
        shortcuts.add(new ShortcutEntry("Feed", "/feed", "minecraft:cooked_beef"));
        shortcuts.add(new ShortcutEntry("Warp", "/warp", "minecraft:ender_pearl"));
        shortcuts.add(new ShortcutEntry("Back", "/back", "minecraft:clock"));
    }

    private static void normalizeShortcutList(List<ShortcutEntry> shortcuts) {
        while (shortcuts.size() > MAX_SHORTCUTS_PER_ROW) {
            shortcuts.remove(shortcuts.size() - 1);
        }

        shortcuts.forEach(ShortcutEntry::normalize);
    }

    public static final class ShortcutEntry {
        public boolean enabled = true;
        public String label = "";
        public String command = "";
        public String item = "minecraft:paper";

        public ShortcutEntry() {
        }

        public ShortcutEntry(String label, String command, String item) {
            this.label = label;
            this.command = command;
            this.item = item;
        }

        public ShortcutEntry copy() {
            ShortcutEntry copy = new ShortcutEntry();
            copy.enabled = this.enabled;
            copy.label = this.label;
            copy.command = this.command;
            copy.item = this.item;
            return copy;
        }

        public void normalize() {
            this.label = normalizeString(this.label);
            this.command = normalizeString(this.command);
            this.item = normalizeString(this.item);

            if (this.item.isEmpty()) {
                this.item = "minecraft:paper";
            }
        }

        private static String normalizeString(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
