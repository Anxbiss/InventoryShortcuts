package de.anxbiss.shortcuts.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShortcutConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("InventoryShortcuts");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("inventoryshortcuts.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("anxbissshortcuts.json");

    private static ShortcutConfig config;
    private static long lastModified = Long.MIN_VALUE;

    private ShortcutConfigManager() {
    }

    public static void initialize() {
        loadOrCreateConfig();
    }

    public static ShortcutConfig getConfig() {
        if (config == null) {
            initialize();
        }

        return config;
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }

    public static void saveConfig(ShortcutConfig newConfig) throws IOException {
        ShortcutConfig normalizedConfig = newConfig.copy();
        normalizedConfig.normalize();

        Files.createDirectories(CONFIG_PATH.getParent());
        writeConfig(normalizedConfig);
        config = normalizedConfig;
        lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
    }

    public static boolean reloadIfChanged() {
        if (config == null) {
            initialize();
            return true;
        }

        try {
            if (!Files.exists(CONFIG_PATH)) {
                writeConfig(ShortcutConfig.createDefault());
                loadOrCreateConfig();
                return true;
            }

            long currentLastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            if (currentLastModified != lastModified) {
                loadOrCreateConfig();
                return true;
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not check shortcut config for changes", exception);
        }

        return false;
    }

    public static List<ResolvedShortcut> resolveTopShortcuts() {
        return resolveShortcuts(getConfig().topShortcuts);
    }

    public static List<ResolvedShortcut> resolveBottomShortcuts() {
        return resolveShortcuts(getConfig().bottomShortcuts);
    }

    public static List<ResolvedShortcut> resolveRadialShortcuts() {
        return resolveShortcuts(getConfig().radialShortcuts);
    }

    public static ItemPreview resolveItemPreview(String itemId) {
        ParsedItem parsedItem = parseItem(itemId, false);
        return new ItemPreview(parsedItem.stack(), parsedItem.valid());
    }

    private static List<ResolvedShortcut> resolveShortcuts(List<ShortcutConfig.ShortcutEntry> entries) {
        List<ResolvedShortcut> resolved = new ArrayList<>();

        for (ShortcutConfig.ShortcutEntry entry : entries) {
            if (entry == null || !entry.enabled) {
                continue;
            }

            String command = normalizeCommand(entry.command);
            if (command.isEmpty()) {
                continue;
            }

            ParsedItem parsedItem = parseItem(entry.item, true);
            String label = entry.label.isBlank() ? prettifyCommand(command) : entry.label;

            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal(label).formatted(Formatting.GOLD));
            tooltip.add(Text.literal(command).formatted(Formatting.GRAY));
            if (!parsedItem.valid()) {
                tooltip.add(Text.literal("Ungueltige Item-ID: " + entry.item).formatted(Formatting.RED));
            }

            resolved.add(new ResolvedShortcut(label, command, parsedItem.stack(), List.copyOf(tooltip)));
        }

        return resolved;
    }

    private static void loadOrCreateConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH) && Files.exists(LEGACY_CONFIG_PATH)) {
                Files.copy(LEGACY_CONFIG_PATH, CONFIG_PATH);
            }

            if (!Files.exists(CONFIG_PATH)) {
                writeConfig(ShortcutConfig.createDefault());
            }

            ShortcutConfig loadedConfig;
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                loadedConfig = GSON.fromJson(reader, ShortcutConfig.class);
            }

            if (loadedConfig == null) {
                loadedConfig = ShortcutConfig.createDefault();
            }

            loadedConfig.normalize();
            config = loadedConfig;
            lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
        } catch (JsonParseException exception) {
            LOGGER.error("Shortcut config could not be parsed. Keeping the previous config.", exception);
            if (config == null) {
                config = ShortcutConfig.createDefault();
            }
        } catch (IOException exception) {
            LOGGER.error("Shortcut config could not be loaded. Falling back to defaults.", exception);
            config = ShortcutConfig.createDefault();
            lastModified = Long.MIN_VALUE;
        }
    }

    private static void writeConfig(ShortcutConfig shortcutConfig) throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(shortcutConfig, writer);
        }
    }

    private static String normalizeCommand(String command) {
        return command == null ? "" : command.trim();
    }

    private static String prettifyCommand(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private static ParsedItem parseItem(String itemId, boolean logWarnings) {
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null) {
            return new ParsedItem(Items.BARRIER.getDefaultStack(), false);
        }

        Optional<Item> item = Registries.ITEM.getOptionalValue(identifier);
        if (item.isEmpty() || item.get() == Items.AIR) {
            if (logWarnings) {
                LOGGER.warn("Unknown shortcut item configured: {}", itemId);
            }
            return new ParsedItem(Items.BARRIER.getDefaultStack(), false);
        }

        return new ParsedItem(item.get().getDefaultStack(), true);
    }

    private record ParsedItem(ItemStack stack, boolean valid) {
    }

    public record ResolvedShortcut(String label, String command, ItemStack iconStack, List<Text> tooltipLines) {
    }

    public record ItemPreview(ItemStack stack, boolean valid) {
    }
}
