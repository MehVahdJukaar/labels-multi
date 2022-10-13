package net.mehvahdjukaar.labels.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.mehvahdjukaar.labels.LabelsMod;
import net.mehvahdjukaar.labels.LabelsModClient;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.fabric.FabricSetupCallbacks;

public class LabelsFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        LabelsMod.commonInit();

        if (PlatformHelper.getEnv().isClient()) {
            FabricSetupCallbacks.CLIENT_SETUP.add(LabelsModClient::init);
        }
    }
}
