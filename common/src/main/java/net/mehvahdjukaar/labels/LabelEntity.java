package net.mehvahdjukaar.labels;

import com.google.common.math.DoubleMath;
import net.mehvahdjukaar.moonlight.api.platform.ForgeHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.advancements.CriteriaTriggers;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LabelEntity extends HangingEntity {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Byte> DATA_DYE_COLOR = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_GLOWING = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TEXT = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.BOOLEAN);

    private AttachFace attachFace = AttachFace.WALL;

    //client
    private boolean needsVisualRefresh = true;
    @Nullable
    private ResourceLocation textureId;
    private float scale;
    private FormattedCharSequence[] labelText;


    public LabelEntity(EntityType<? extends HangingEntity> entityType, Level world) {
        super(entityType, world);
    }

    public LabelEntity(Level level, BlockPos pos, Direction clickedFace, Direction horizontalFacing) {
        super(LabelsMod.LABEL.get(), level, pos);
        if (clickedFace.getAxis().isHorizontal()) {
            this.setOrientation(clickedFace,AttachFace.WALL); //clickedFace is used for horizontal clickedFace
        } else {
            this.setOrientation(horizontalFacing.getOpposite(), clickedFace == Direction.UP ? AttachFace.FLOOR : AttachFace.CEILING);
        }
        this.setPos(pos.getX(), pos.getY(), pos.getZ());
    }


    public void setOrientation(Direction horizontalOrientation, AttachFace face) {
        this.attachFace = face;
        super.setDirection(horizontalOrientation);
        if(face != AttachFace.WALL) {
            this.setXRot(90f * (face == AttachFace.FLOOR ? -1 : 1));
            this.setYRot((horizontalOrientation.get2DDataValue() * 90));
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
        }
    }

    //item frame code
    @Deprecated(since = "use the one with 2 param")
    @Override
    protected void setDirection(Direction facingDirection) {
        super.setDirection(facingDirection);
    }

    //might as well use this
    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this,
                ((this.direction.get2DDataValue() & 0b1111) << 4) | (this.attachFace.ordinal() & 0b1111),
                this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        int data = pPacket.getData();
        this.setOrientation(Direction.from2DDataValue((data >> 4) & 0b1111),
                AttachFace.values()[data & 0b1111]);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_DYE_COLOR, (byte) -1);
        this.entityData.define(DATA_GLOWING, false);
        this.entityData.define(DATA_TEXT, false);
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
            if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
                this.spawnAtLocation(LabelsMod.LABEL_ITEM.get());
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
            recomputeTexture(itemstack);
            this.needsVisualRefresh = true;
        } else if (pKey.equals(DATA_DYE_COLOR)) {
            recomputeTexture(this.getItem());
        }
    }

    private void recomputeTexture(ItemStack itemstack) {
        String s = Utils.getID(itemstack.getItem()).toString().replace(":", "/");
        var color = this.getColor();
        if (color != null) s += "_" + color.getName();
        this.textureId = LabelsMod.res(s);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!this.getItem().isEmpty()) {
            tag.put("Item", this.getItem().save(new CompoundTag()));
        }
        tag.putByte("Facing", (byte) this.direction.get2DDataValue());
        tag.putByte("AttachFace", (byte) this.attachFace.ordinal());
        tag.putBoolean("Glowing", this.hasGlowInk());
        tag.putBoolean("Text", this.hasText());
        var c = this.getColor();
        if (c != null) {
            tag.putByte("DyeColor", (byte) c.ordinal());
        }
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
        this.setOrientation(Direction.from2DDataValue(tag.getByte("Facing")),
                AttachFace.values()[tag.getByte("AttachFace")]);
        this.getEntityData().set(DATA_GLOWING, tag.getBoolean("Glowing"));
        this.getEntityData().set(DATA_TEXT, tag.getBoolean("Text"));
        if (tag.contains("DyeColor")) {
            this.getEntityData().set(DATA_DYE_COLOR, tag.getByte("DyeColor"));
        }

    }

    @Override
    public boolean isCurrentlyGlowing() {
        return false;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return LabelsMod.LABEL_ITEM.get().getDefaultInstance();
    }

    @Override
    public void setPos(double x, double y, double z) {
        this.setPosRaw(x,y,z);
        super.setPos(x, y, z);
    }

    //just updates bounding box based off current pos
    @Override
    protected void recalculateBoundingBox() {
        if (this.direction != null) {

            var shape = level.getBlockState(pos).getBlockSupportShape(level, pos);
            if (shape.isEmpty()) {
                return; //wait for survives to be called so this will be removed
            }
            double offset;
            Direction dir = this.getBehindDirection();

            if (dir.getAxisDirection() != Direction.AxisDirection.POSITIVE) {
                offset = 0.5 - shape.max(dir.getAxis());
            } else {
                offset = -0.5 + shape.min(dir.getAxis());
            }
            Vec3 v = Vec3.atCenterOf(pos);
            offset -= 1 / 32f;

            v = v.add(dir.getStepX() * offset, dir.getStepY() * offset, dir.getStepZ() * offset);

            this.setPosRaw(v.x, v.y, v.z);

            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            double width = this.getWidth();
            double height = this.getHeight();
            double zWidth = this.getWidth();
            Direction.Axis axis = dir.getAxis();
            switch (axis) {
                case X -> width = 1.0D;
                case Y -> height = 1.0D;
                case Z -> zWidth = 1.0D;
            }
            width /= 32;
            height /= 32;
            zWidth /= 32;
            this.setBoundingBox(new AABB(x - width, y - height, z - zWidth, x + width, y + height, z + zWidth));
        }
    }

    public BlockPos getSupportingBlockPos() {
        Direction behind = this.getBehindDirection();
        return new BlockPos(this.position().relative(behind, 0.05));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.isRemoved()) return InteractionResult.PASS;
        ItemStack itemstack = player.getItemInHand(hand);

        if (player.isSecondaryUseActive() && !itemstack.isEmpty()) {
            if (!this.level.isClientSide) {
                this.setItem(itemstack);
                if (!itemstack.isEmpty()) {
                    this.playSound(SoundEvents.INK_SAC_USE, 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(player.level.isClientSide);
        } else {
            boolean consume = true;
            boolean success = false;
            boolean glowInk = this.hasGlowInk();
            if (itemstack.getItem() == Items.FEATHER) {
                this.cycleText();
                consume = false;
                success = true;
            } else if (itemstack.getItem() == Items.GLOW_INK_SAC && !glowInk) {
                level.playSound(null, pos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.getEntityData().set(DATA_GLOWING, true);
                success = true;
            } else if (itemstack.getItem() == Items.INK_SAC && glowInk) {
                level.playSound(null, pos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.getEntityData().set(DATA_GLOWING, false);
                success = true;
            } else if (ForgeHelper.getColor(itemstack) != null) {
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                var color = ForgeHelper.getColor(itemstack);
                this.getEntityData().set(DATA_DYE_COLOR, (byte) (color == null ? -1 : color.ordinal()));
                this.recomputeTexture(this.getItem());
                success = true;
            }
            if (success) {
                if (consume && !player.isCreative()) {
                    itemstack.shrink(1);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, itemstack);
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            InteractionResult interactionresult;
            if (player instanceof ServerPlayer sp) {
                BlockPos p = this.getSupportingBlockPos();
                interactionresult = sp.gameMode.useItemOn(sp, sp.level, itemstack, hand,
                        new BlockHitResult(Vec3.atCenterOf(p), this.direction, p, false));
                return interactionresult;
            } else {
                return InteractionResult.SUCCESS;
            }
        }
    }

    private void cycleText() {
        this.getEntityData().set(DATA_TEXT, !this.getEntityData().get(DATA_TEXT));
    }

    @Override
    public boolean survives() {
        if (!this.level.noCollision(this)) {
            return false;
        }
        BlockPos pos = getSupportingBlockPos();
        Direction behindDIr = this.getBehindDirection();
        BlockState state = this.level.getBlockState(pos);
        var blockShape = state.getBlockSupportShape(level, pos);
        if (blockShape.isEmpty() || !state.getMaterial().isSolid()) {
            return false;
        }
        var bbShape = this.getBoundingBox().move(-Mth.floor(this.getX()), -Mth.floor(this.getY()), -Mth.floor(this.getZ()));

        if (behindDIr.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            if (!DoubleMath.fuzzyEquals(bbShape.max(behindDIr.getAxis()), bbShape.max(behindDIr.getAxis()), 1.0E-7))
                return false;
        } else {
            if (!DoubleMath.fuzzyEquals(bbShape.min(behindDIr.getAxis()), bbShape.min(behindDIr.getAxis()), 1.0E-7))
                return false;
        }

        return this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();

    }

    private Direction getBehindDirection() {
        return switch (attachFace) {
            case WALL -> direction.getOpposite();
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
        };
    }

    public boolean needsVisualUpdate() {
        if (this.needsVisualRefresh) {
            this.needsVisualRefresh = false;
            return true;
        }
        return false;
    }

    public void setLabelText(FormattedCharSequence[] tempPageLines) {
        this.labelText = tempPageLines;
    }

    public void setLabelTextScale(float scale) {
        this.scale = scale;
    }

    public float getLabelTextScale() {
        return scale;
    }

    public FormattedCharSequence[] getLabelText() {
        return labelText;
    }

    @Nullable
    public ResourceLocation getTextureId() {
        return textureId;
    }

    public boolean hasGlowInk() {
        return this.getEntityData().get(DATA_GLOWING);
    }

    public boolean hasText() {
        return this.getEntityData().get(DATA_TEXT);
    }

    @Nullable
    public DyeColor getColor() {
        byte i = this.getEntityData().get(DATA_DYE_COLOR);
        return i == -1 ? null : DyeColor.byId(i);
    }
}
