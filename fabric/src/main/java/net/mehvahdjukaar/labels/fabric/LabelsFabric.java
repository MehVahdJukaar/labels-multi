package net.mehvahdjukaar.labels.fabric;

import net.fabricmc.api.ModInitializer;
import net.mehvahdjukaar.labels.LabelsMod;
import net.mehvahdjukaar.labels.LabelsModClient;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.fabric.MLFabricSetupCallbacks;

public class LabelsFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        LabelsMod.commonInit();

        if (PlatHelper.getPhysicalSide().isClient()) {
            MLFabricSetupCallbacks.CLIENT_SETUP.add(LabelsModClient::init);
        }
    }
}
