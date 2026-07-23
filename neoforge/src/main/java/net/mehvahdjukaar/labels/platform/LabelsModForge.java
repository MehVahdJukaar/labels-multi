package net.mehvahdjukaar.labels.platform;

import net.mehvahdjukaar.labels.LabelsMod;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Author: MehVahdJukaar
 */
@Mod(LabelsMod.MOD_ID)
public class LabelsModForge {

    public LabelsModForge(IEventBus bus) {
        RegHelper.startRegisteringFor(bus);
        LabelsMod.commonInit();
    }
}

