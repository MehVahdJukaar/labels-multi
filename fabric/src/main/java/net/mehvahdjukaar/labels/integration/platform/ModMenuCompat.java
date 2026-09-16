package net.mehvahdjukaar.labels.integration.platform;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.mehvahdjukaar.labels.ClientConfigs;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;

public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ClientHelper.makeConfigScreen(ClientConfigs.CONFIG_SPEC, parent, null);
    }
}
