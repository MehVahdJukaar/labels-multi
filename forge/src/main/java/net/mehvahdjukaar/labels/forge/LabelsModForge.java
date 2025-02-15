package net.mehvahdjukaar.labels.forge;

import net.mehvahdjukaar.labels.LabelsMod;
import net.mehvahdjukaar.labels.LabelsModClient;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MehVahdJukaar
 */
@Mod(LabelsMod.MOD_ID)
public class LabelsModForge {

    public LabelsModForge() {
        LabelsMod.commonInit();
    }
}

