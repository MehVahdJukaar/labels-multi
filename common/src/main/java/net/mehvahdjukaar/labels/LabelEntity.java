package net.mehvahdjukaar.labels;

import com.google.common.math.DoubleMath;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LabelEntity extends HangingEntity {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.ITEM_STACK);


    //client
    private boolean needsVisualRefresh = true;
    private ResourceLocation textureId;
    private float scale;
    private List<FormattedCharSequence> labelText;

    public LabelEntity(EntityType<? extends HangingEntity> entityType, Level world) {
        super(entityType, world);
    }

    public LabelEntity(Level level, BlockPos pos, Direction direction) {
        super(LabelsMod.LABEL.get(), level, pos);
        this.setDirection(direction);
        this.setPos(pos.getX(), pos.getY(), pos.getZ());
    }

    //might as well use this
    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, this.direction.get2DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        this.setDirection(Direction.from2DDataValue(pPacket.getData()));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
        return 0;
    }

    @Override
    public int getWidth() {
        return 10;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void dropItem(@Nullable Entity entity) {
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (entity instanceof Player player) {
                if (!player.getAbilities().instabuild) {
                    this.spawnAtLocation(LabelsMod.LABEL_ITEM.get());
                }
            }
        }
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.ITEM_FRAME_PLACE, 1.0F, 1.0F);
    }

    public void setItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            stack = stack.copy();
            stack.setCount(1);
            stack.setEntityRepresentation(this);
        }
        this.getEntityData().set(DATA_ITEM, stack);
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (pKey.equals(DATA_ITEM)) {
            ItemStack itemstack = this.getItem();
            if (!itemstack.isEmpty() && itemstack.getEntityRepresentation() != this) {
                itemstack.setEntityRepresentation(this);
            }
            this.textureId = LabelsMod.res(Utils.getID(itemstack.getItem()).toString().replace(":", "/"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!this.getItem().isEmpty()) {
            tag.put("Item", this.getItem().save(new CompoundTag()));
        }
        tag.putByte("Facing", (byte) this.direction.get2DDataValue());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        CompoundTag compound = tag.getCompound("Item");
        if (!compound.isEmpty()) {
            ItemStack itemstack = ItemStack.of(compound);
            if (itemstack.isEmpty()) {
                LabelsMod.LOGGER.warn("Unable to load item from: {}", compound);
            }
            this.setItem(itemstack);
        }
        this.setDirection(Direction.from2DDataValue(tag.getByte("Facing")));
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return LabelsMod.LABEL_ITEM.get().getDefaultInstance();
    }

    //just updates bounding box based off current pos
    @Override
    protected void recalculateBoundingBox() {
        if (this.direction != null) {

            BlockPos pos = this.pos;
            var shape = level.getBlockState(pos).getBlockSupportShape(level, pos);
            if (shape.isEmpty()) {
                return; //wait for survives to be called so this will be removed
            }
            double offset;
            if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                offset = -0.5 + shape.max(direction.getAxis());
            } else {
                offset = 0.5 - shape.min(direction.getAxis());
            }
            Vec3 v = Vec3.atCenterOf(pos);
            offset += 1 / 32f;

            v = v.add(direction.getStepX() * offset, direction.getStepY() * offset, direction.getStepZ() * offset);

            this.setPosRaw(v.x, v.y, v.z);

            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            double width = this.getWidth();
            double height = this.getHeight();
            double zWidth = this.getWidth();
            Direction.Axis axis = this.direction.getAxis();
            switch (axis) {
                case X -> width = 1.0D;
                case Y -> height = 1.0D;
                case Z -> zWidth = 1.0D;
            }
            width /= 32;
            height /= 32;
            zWidth /= 32;
            this.setBoundingBox(new AABB(x - width, y - height, z - zWidth, x + width, y + height, z + zWidth));
            //this.pos = new BlockPos(this.getX(), this.getY(), this.getZ());
        }
    }

    public BlockPos getSupportingBlockPos() {
        return switch (this.getDirection()) {
            default -> new BlockPos(this.position().add(0, 0, 0.05));
            case SOUTH -> new BlockPos(this.position().add(0, 0, -0.05));
            case WEST -> new BlockPos(this.position().add(0.05, 0, 0));
            case EAST -> new BlockPos(this.position().add(-0.05, 0, 0));
        };
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (player.isSecondaryUseActive() && !itemstack.isEmpty() && !this.isRemoved()) {
            if (!this.level.isClientSide) {
                this.setItem(itemstack);
                if (!itemstack.isEmpty()) {
                    this.playSound(SoundEvents.INK_SAC_USE, 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide);
        } else {
            InteractionResult interactionresult;
            if (player instanceof ServerPlayer sp) {
                BlockPos p = this.getSupportingBlockPos();
                interactionresult = sp.gameMode.useItemOn(sp, sp.level, itemstack, hand,
                        new BlockHitResult(Vec3.atCenterOf(p), this.direction.getOpposite(), p, false));
                return interactionresult;
            }else{
                return InteractionResult.SUCCESS;
            }
        }
    }

    @Override
    public boolean survives() {
        if (!this.level.noCollision(this)) {
            return false;
        }
        BlockPos pos = getSupportingBlockPos();
        Direction dir = this.getDirection();
        BlockState state = this.level.getBlockState(pos);
        var blockShape = state.getBlockSupportShape(level, pos);
        if (blockShape.isEmpty() || !state.getMaterial().isSolid()) {
            return false;
        }
        var bbShape = this.getBoundingBox().move(-Mth.floor(this.getX()), -Mth.floor(this.getY()), -Mth.floor(this.getZ()));

        if (dir.getAxisDirection() != Direction.AxisDirection.POSITIVE) {
            if (!DoubleMath.fuzzyEquals(bbShape.max(dir.getAxis()), bbShape.max(dir.getAxis()), 1.0E-7)) return false;
        } else {
            if (!DoubleMath.fuzzyEquals(bbShape.min(dir.getAxis()), bbShape.min(dir.getAxis()), 1.0E-7)) return false;
        }

        return this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();

    }

    public boolean needsVisualUpdate() {
        if (this.needsVisualRefresh) {
            this.needsVisualRefresh = false;
            return true;
        }
        return false;
    }

    public void setLabelText(List<FormattedCharSequence> tempPageLines) {
        this.labelText = tempPageLines;
    }

    public void setLabelTextScale(float scale) {
        this.scale = scale;
    }

    public float getLabelTextScale() {
        return scale;
    }

    public List<FormattedCharSequence> getLabelText() {
        return labelText;
    }

    public ResourceLocation getTextureId() {
        return textureId;
    }
}
