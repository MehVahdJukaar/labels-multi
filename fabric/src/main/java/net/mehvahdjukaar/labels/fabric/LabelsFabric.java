package net.mehvahdjukaar.labels.fabric;

import net.fabricmc.api.ModInitializer;
import net.mehvahdjukaar.labels.LabelsMod;
import net.mehvahdjukaar.labels.LabelsModClient;
import net.mehvahdjukaar.moonlight2.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight2.api.platform.fabric.RegHelperImpl;


public class LabelsFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        LabelsMod.commonInit();


        //registers stuff
        RegHelperImpl.registerEntries();

        if (PlatformHelper.getEnv().isClient()) {
            LabelsModClient.init();
        }
    }
}
