package de.anxbiss.shortcuts.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemSelectScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onSelect;

    private TextFieldWidget searchField;
    private final List<Item> filteredItems = new ArrayList<>();

    private String search = "";

    public ItemSelectScreen(Screen parent, Consumer<String> onSelect) {
        super(Text.literal("Select Item"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        this.searchField = new TextFieldWidget(
                this.textRenderer,
                this.width / 2 - 100,
                20,
                200,
                20,
                Text.literal("Search")
        );
        this.searchField.setChangedListener(value -> {
            this.search = value.toLowerCase();
            this.refreshItems();
        });
        this.addDrawableChild(this.searchField);
        this.setInitialFocus(this.searchField);

        this.refreshItems();
    }

    private void refreshItems() {
        this.filteredItems.clear();

        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (id.toString().toLowerCase().contains(this.search)) {
                this.filteredItems.add(item);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderDarkening(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);

        int x = this.width / 2 - 100;
        int y = 50;

        for (int i = 0; i < Math.min(this.filteredItems.size(), 40); i++) {
            Item item = this.filteredItems.get(i);
            context.drawItem(item.getDefaultStack(), x + (i % 8) * 20, y + (i / 8) * 20);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
