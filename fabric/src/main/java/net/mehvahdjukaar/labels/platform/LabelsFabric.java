package net.mehvahdjukaar.labels.platform;

import net.fabricmc.api.ModInitializer;
import net.mehvahdjukaar.labels.LabelsMod;

public class LabelsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LabelsMod.commonInit();
    }
}
