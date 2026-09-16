package net.mehvahdjukaar.labels.platform;

import net.mehvahdjukaar.labels.LabelsMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Author: MehVahdJukaar
 */
@Mod(LabelsMod.MOD_ID)
public class LabelsModForge {

    public LabelsModForge(IEventBus bus) {
        LabelsMod.commonInit();
    }
}
