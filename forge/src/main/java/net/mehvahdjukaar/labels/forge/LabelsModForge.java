package net.mehvahdjukaar.labels.forge;

import net.mehvahdjukaar.labels.LabelsMod;
import net.mehvahdjukaar.labels.LabelsModClient;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MehVahdJukaar
 */
@Mod(LabelsMod.MOD_ID)
public class LabelsModForge {

    public LabelsModForge() {
        LabelsMod.commonInit();

        if (PlatformHelper.getEnv().isClient()) {
            LabelsModClient.init();
        }
    }
}

