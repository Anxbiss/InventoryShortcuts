package de.anxbiss.shortcuts.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.anxbiss.shortcuts.client.screen.ShortcutsConfigScreen;

public class ShortcutsModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ShortcutsConfigScreen::new;
    }
}