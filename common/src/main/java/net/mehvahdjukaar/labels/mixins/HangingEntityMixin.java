package net.mehvahdjukaar.labels.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.labels.LabelEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HangingEntity.class)
public class HangingEntityMixin {

    @ModifyReturnValue(method = "method_6890",
    at = @At("RETURN"))
    private static boolean amendments$modifyHangingEntityCheck(boolean original, Entity entity){
        return original || entity instanceof LabelEntity;
    }
}
