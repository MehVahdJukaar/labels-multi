package net.mehvahdjukaar.labels.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.labels.LabelEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HangingEntity.class)
public abstract class HangingEntityMixin {

    @Shadow
    protected abstract AABB getPopBox();

    @ModifyReturnValue(method = "canCoexist", at = @At("RETURN"))
    private boolean labels$labelsTakeUpSpace(boolean original) {
        if (!original) return false;
        HangingEntity self = (HangingEntity) (Object) this;
        return !self.level().hasEntities(EntityTypeTest.forClass(LabelEntity.class), this.getPopBox(), e -> true);
    }
}
