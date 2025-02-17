package net.mehvahdjukaar.labels;

import com.google.common.math.DoubleMath;
import net.mehvahdjukaar.moonlight.api.platform.ForgeHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class LabelEntity extends Entity {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Byte> DATA_DYE_COLOR = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_GLOWING = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TEXT = SynchedEntityData.defineId(LabelEntity.class,
            EntityDataSerializers.BOOLEAN);

    private AttachFace attachFace = null;
    private Direction direction = null;
    private int checkInterval;

    //client
    private boolean needsVisualRefresh = true;
    @Nullable
    private ResourceLocation textureId;
    private float scale;
    private FormattedCharSequence[] labelText;

    public LabelEntity(EntityType<? extends LabelEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static LabelEntity placeOnFace(Level level, Vec3 hitVec, Direction clickedFace, Direction horizontalFacing) {
        LabelEntity lab = new LabelEntity(LabelsMod.LABEL.get(), level);
        if (clickedFace.getAxis().isHorizontal()) {
            lab.setOrientation(clickedFace, AttachFace.WALL); //clickedFace is used for horizontal clickedFace
        } else {
            lab.setOrientation(horizontalFacing.getOpposite(), clickedFace == Direction.UP ? AttachFace.FLOOR : AttachFace.CEILING);
        }
        //pos parameter will be used for support position only

        //here set correct position based on block
        float offset = lab.getThickness() / 32f;
        Vec3 step = new Vec3(clickedFace.step());
        Vec3 stepSQ = step.multiply(step);
        Vec3 invStep = new Vec3(1, 1, 1).subtract(stepSQ);
        //keep post on axis. set otherto to pos+0.5
        BlockPos pos = BlockPos.containing(hitVec);
        Vec3 newPos = pos.getCenter().multiply(invStep)
                .add(stepSQ.multiply(hitVec.add(step.scale(offset))));
        //this also sets the bounding box
        lab.setPos(newPos.x, newPos.y, newPos.z);

        return lab;
    }

    public BlockPos calculateBehindPos() {
        return BlockPos.containing(this.position().relative(this.getBehindDirection(), 1 / 16f));
    }

    private Direction getBehindDirection() {
        return switch (attachFace) {
            case WALL -> direction.getOpposite();
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
        };
    }


    public void setOrientation(@NotNull Direction horizontalOrientation, @NotNull AttachFace face) {
        this.attachFace = face;
        this.direction = horizontalOrientation;
        if (face != AttachFace.WALL) {
            this.setXRot(90f * (face == AttachFace.FLOOR ? -1 : 1));
            this.xRotO = this.getXRot();
        }
        this.setYRot((horizontalOrientation.get2DDataValue() * 90));
        this.yRotO = this.getYRot();
    }


    @Override
    public Direction getDirection() {
        return direction;
    }


    @Override
    protected AABB makeBoundingBox() {
        //can happen if called in constructor
        if (this.attachFace == null || this.direction == null) {
            return super.makeBoundingBox();
        }
        Level level = level();
        BlockPos supportPos = calculateBehindPos();

        BlockState support = level.getBlockState(supportPos);
        var shape = support.getBlockSupportShape(level, supportPos);
        if (shape.isEmpty()) {
            return super.makeBoundingBox(); //wait for survives to be called so this will be removed
        }
        double offset;
        Direction dir = this.getBehindDirection();

        if (dir.getAxisDirection() != Direction.AxisDirection.POSITIVE) {
            offset = 0.5 - shape.max(dir.getAxis());
        } else {
            offset = -0.5 + shape.min(dir.getAxis());
        }
        Vec3 v = Vec3.atCenterOf(supportPos);
        offset -= this.getThickness() / 32f;

        if (dir.getAxis() != Direction.Axis.Y && support.is(LabelsMod.LOWERS_LABELS)) {
            v = v.add(0, -0.125, 0);
        }

        v = v.add(dir.getStepX() * offset, dir.getStepY() * offset, dir.getStepZ() * offset);

        this.setPosRaw(v.x, v.y, v.z);

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        double width = this.getWidth();
        double height = this.getHeight();
        double zWidth = this.getWidth();
        double thickness = this.getThickness();
        Direction.Axis axis = dir.getAxis();
        switch (axis) {
            case X -> width = thickness;
            case Y -> height = thickness;
            case Z -> zWidth = thickness;
        }
        width /= 32;
        height /= 32;
        zWidth /= 32;
        return new AABB(x - width, y - height, z - zWidth, x + width, y + height, z + zWidth);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this,
                ((this.direction.get2DDataValue() & 0xFF) << 8) | (this.attachFace.ordinal() & 0xFF));
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        int i = packet.getData();
        Direction direction = Direction.from2DDataValue((i & 0xFF00) >> 8);
        AttachFace attachFace = AttachFace.values()[i & 0xFF];
        this.setOrientation(direction, attachFace);
        //before super since facing is needed for BB calculations set in setPos
        super.recreateFromPacket(packet);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        ItemStack item = this.getItem();
        if (!item.isEmpty()) {
            tag.put("Item", item.save(new CompoundTag()));
        }
        tag.putByte("Facing", (byte) this.direction.get2DDataValue());
        tag.putByte("AttachFace", (byte) this.attachFace.ordinal());
        tag.putBoolean("Glowing", this.hasGlowInk());
        tag.putBoolean("Text", this.hasText());
        DyeColor c = this.getColor();
        if (c != null) {
            tag.putByte("DyeColor", (byte) c.ordinal());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
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
        this.setHasGlowInk(tag.getBoolean("Glowing"));
        this.setHasText(tag.getBoolean("Text"));
        if (tag.contains("DyeColor")) {
            this.getEntityData().set(DATA_DYE_COLOR, tag.getByte("DyeColor"));
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_DYE_COLOR, (byte) -1);
        this.entityData.define(DATA_GLOWING, false);
        this.entityData.define(DATA_TEXT, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (level().isClientSide) {
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
    }

    @Override
    protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
        return 0;
    }

    public int getWidth() {
        return 8;
    }

    public int getHeight() {
        return 10;
    }

    public int getThickness() {
        return 1;
    }

    public void dropItem(@Nullable Entity entity) {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
                this.spawnAtLocation(LabelsMod.LABEL_ITEM.get());
            }
        }
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            this.checkBelowWorld();
            if (this.checkInterval++ == 100) {
                this.checkInterval = 0;
                if (!this.isRemoved() && !this.survives()) {
                    this.discard();
                    this.dropItem(null);
                }
            }
        }
    }

    // @Override
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

    private void recomputeTexture(ItemStack itemstack) {
        String s = Utils.getID(itemstack.getItem()).toString().replace(":", "/");
        var color = this.getColor();
        if (color != null) s += "_" + color.getName();
        this.textureId = LabelsMod.res(s);
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

    //Hanging entity stuff

    //  @Override
    public boolean survives() {
        Level level = level();
        if (!level.noCollision(this)) {
            return false;
        }
        BlockPos supportingPos = calculateBehindPos();
        Direction behindDIr = this.getBehindDirection();
        BlockState state = level.getBlockState(supportingPos);
        var blockShape = state.getBlockSupportShape(level, supportingPos);
        if (blockShape.isEmpty() || !state.isSolid()) {
            return false;
        }
        var bbShape = this.getBoundingBox();
        blockShape = blockShape.move(supportingPos.getX(), supportingPos.getY(), supportingPos.getZ());

        //check if shapes are touching
        if (behindDIr.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            if (!DoubleMath.fuzzyEquals(bbShape.max(behindDIr.getAxis()), blockShape.min(behindDIr.getAxis()), 1.0E-7))
                return false;
        } else {
            if (!DoubleMath.fuzzyEquals(bbShape.min(behindDIr.getAxis()), blockShape.max(behindDIr.getAxis()), 1.0E-7))
                return false;
        }

        return level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();
    }

    protected static final Predicate<Entity> HANGING_ENTITY = (entity) ->
            entity instanceof HangingEntity || entity instanceof LabelEntity;

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        if (entity instanceof Player player) {
            return !this.level().mayInteract(player, this.getOnPos()) ? true : this.hurt(this.damageSources().playerAttack(player), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level().isClientSide) {
                this.kill();
                this.markHurt();
                this.dropItem(source.getEntity());
            }
            return true;
        }
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
        if (!this.level().isClientSide && !this.isRemoved() && pos.lengthSqr() > 0.0) {
            this.kill();
            this.dropItem(null);
        }
    }

    @Override
    public void push(double x, double y, double z) {
        if (!this.level().isClientSide && !this.isRemoved() && x * x + y * y + z * z > 0.0) {
            this.kill();
            this.dropItem(null);
        }
    }

    @Override
    public ItemEntity spawnAtLocation(ItemStack stack, float offsetY) {
        ItemEntity itemEntity = new ItemEntity(this.level(), this.getX() + (double) ((float) this.direction.getStepX() * 0.15F), this.getY() + (double) offsetY, this.getZ() + (double) ((float) this.direction.getStepZ() * 0.15F), stack);
        itemEntity.setDefaultPickUpDelay();
        this.level().addFreshEntity(itemEntity);
        return itemEntity;
    }

    //idk why armor stands have to false. false makes them pop on load
    @Override
    protected boolean repositionEntityAfterLoad() {
        return true;
    }

    @Override
    public float rotate(Rotation transformRotation) {
        if (this.direction.getAxis() != Direction.Axis.Y) {
            switch (transformRotation) {
                case CLOCKWISE_180 -> this.direction = this.direction.getOpposite();
                case COUNTERCLOCKWISE_90 -> this.direction = this.direction.getCounterClockWise();
                case CLOCKWISE_90 -> this.direction = this.direction.getClockWise();
            }
        }

        float f = Mth.wrapDegrees(this.getYRot());
        switch (transformRotation) {
            case CLOCKWISE_180 -> {
                return f + 180.0F;
            }
            case COUNTERCLOCKWISE_90 -> {
                return f + 90.0F;
            }
            case CLOCKWISE_90 -> {
                return f + 270.0F;
            }
            default -> {
                return f;
            }
        }
    }

    @Override
    public float mirror(Mirror transformMirror) {
        return this.rotate(transformMirror.getRotation(this.direction));
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
    }

    @Override
    public void refreshDimensions() {
    }


    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.isRemoved()) return InteractionResult.PASS;
        ItemStack itemstack = player.getItemInHand(hand);
        Level level = level();
        if (player.isSecondaryUseActive() && !itemstack.isEmpty()) {
            if (!level.isClientSide) {
                this.setItem(itemstack);
                if (!itemstack.isEmpty()) {
                    this.playSound(SoundEvents.INK_SAC_USE, 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            boolean consume = true;
            boolean success = false;
            boolean glowInk = this.hasGlowInk();
            if (itemstack.getItem() == Items.FEATHER) {
                this.cycleText();
                consume = false;
                success = true;
            } else if (itemstack.getItem() == Items.GLOW_INK_SAC && !glowInk) {
                level.playSound(null, this, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                setHasGlowInk(true);
                success = true;
            } else if (itemstack.getItem() == Items.INK_SAC && glowInk) {
                level.playSound(null, this, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                setHasGlowInk(false);
                success = true;
            } else if (ForgeHelper.getColor(itemstack) != null) {
                level.playSound(null, this, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                var color = ForgeHelper.getColor(itemstack);
                setColor(color);
                this.recomputeTexture(this.getItem());
                success = true;
            }
            if (success) {
                if (consume && !player.isCreative()) {
                    itemstack.shrink(1);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    //not a block
                    //CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, itemstack);
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            InteractionResult interactionresult;
            if (player instanceof ServerPlayer sp) {
                BlockPos p = this.calculateBehindPos();
                interactionresult = sp.gameMode.useItemOn(sp, level, itemstack, hand,
                        new BlockHitResult(Vec3.atCenterOf(p), this.direction, p, false));
                return interactionresult;
            } else {
                return InteractionResult.SUCCESS;
            }
        }
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

    private void setHasGlowInk(boolean glowing) {
        this.getEntityData().set(DATA_GLOWING, glowing);
    }

    private void setHasText(boolean text) {
        this.getEntityData().set(DATA_TEXT, text);
    }

    private void cycleText() {
        this.getEntityData().set(DATA_TEXT, !this.getEntityData().get(DATA_TEXT));
    }


    @Nullable
    public DyeColor getColor() {
        byte i = this.getEntityData().get(DATA_DYE_COLOR);
        return i == -1 ? null : DyeColor.byId(i);
    }

    private void setColor(@Nullable DyeColor color) {
        this.getEntityData().set(DATA_DYE_COLOR, (byte) (color == null ? -1 : color.ordinal()));
    }
}