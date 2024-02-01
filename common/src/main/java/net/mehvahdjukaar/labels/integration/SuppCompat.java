package net.mehvahdjukaar.labels.integration;

import net.mehvahdjukaar.supplementaries.common.block.blocks.SackBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SuppCompat {

    public static boolean isSack(BlockState state){
        return state.getBlock() instanceof SackBlock;
    }
}
