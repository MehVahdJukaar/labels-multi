package net.mehvahdjukaar.labels.integration.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.mehvahdjukaar.labels.ClientConfigs;

public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClientConfigs.CONFIG_SPEC::makeScreen;
    }
}