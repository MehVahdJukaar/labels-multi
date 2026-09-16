package net.mehvahdjukaar.labels;

import net.mehvahdjukaar.candlelight.api.VirtualOverride;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gameevent.GameEvent;

public class LabelItem extends Item {

    public LabelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Direction direction = pContext.getClickedFace();
        BlockPos blockpos = pContext.getClickedPos();
        Direction facing = pContext.getHorizontalDirection();
        Player player = pContext.getPlayer();
        ItemStack itemstack = pContext.getItemInHand();
        if (player != null && !player.mayUseItemAt(blockpos, direction, itemstack)) {
            return InteractionResult.FAIL;
        } else {
            Level level = pContext.getLevel();
            LabelEntity label = LabelEntity.placeOnFace(level, pContext.getClickLocation(), direction, facing);

            EntityType.createDefaultStackConfig(level, itemstack, player).accept(label);

            if (label.survives()) {
                if (!level.isClientSide()) {
                    label.playPlacementSound();
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, blockpos);
                    level.addFreshEntity(label);
                }

                if (!player.getAbilities().instabuild) itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    @VirtualOverride("neoforge")
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        InteractionResult res;
        if (context.getPlayer().isCreative()) {
            int i = stack.getCount();
            res = stack.useOn(context);
            stack.setCount(i);
        } else {
            res = stack.useOn(context);
        }
        return res.consumesAction() ? res : InteractionResult.PASS;
    }

    @VirtualOverride("neoforge")
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }
}
