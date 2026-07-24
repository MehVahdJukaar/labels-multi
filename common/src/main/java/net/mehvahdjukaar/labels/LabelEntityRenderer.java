package net.mehvahdjukaar.labels;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.DynamicTextureRenderer;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderableDynamicTexture;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.client.util.TextUtil;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.SpriteUtils;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.mehvahdjukaar.moonlight.api.util.math.colors.BaseColor;
import net.mehvahdjukaar.moonlight.api.util.math.colors.RGBColor;
import net.mehvahdjukaar.moonlight.core.misc.McMetaFile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntUnaryOperator;


public class LabelEntityRenderer extends EntityRenderer<LabelEntity> {

    private final ModelBlockRenderer modelRenderer;
    private final ModelManager modelManager;
    private final Camera camera;

    public LabelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        Minecraft minecraft = Minecraft.getInstance();
        this.modelRenderer = minecraft.getBlockRenderer().getModelRenderer();
        this.modelManager = minecraft.getBlockRenderer().getBlockModelShaper().getModelManager();
        this.camera = minecraft.gameRenderer.getMainCamera();
    }

    @Override
    public void render(LabelEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, light);

        if (this.entityRenderDispatcher.shouldRenderHitBoxes()) {
            BlockPos behind = entity.calculateBehindPos();
            VertexConsumer lines = buffer.getBuffer(RenderType.lines());
            poseStack.pushPose();
            var ep = entity.position();
            Vec3 vec3 = new Vec3(behind.getX() - ep.x, behind.getY() - ep.y, behind.getZ() - ep.z);
            AABB bb = new AABB(vec3, vec3.add(1, 1, 1)).inflate(0.01);
            LevelRenderer.renderLineBox(poseStack, lines, bb, 1.0F, 0, 0, 1.0F);
            poseStack.popPose();
        }

        poseStack.pushPose();

        //prevents incorrect rendering on first frame

        poseStack.mulPose(Axis.YP.rotationDegrees(180 - entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));

        poseStack.translate(0, -0, -0.5 + 1 / 32f);
        poseStack.translate(-0.5, -0.5, -0.5);

        modelRenderer.renderModel(poseStack.last(), buffer.getBuffer(Sheets.cutoutBlockSheet()), //
                null, ClientHelper.getModel(modelManager, LabelsModClient.LABEL_MODEL), 1.0F, 1.0F, 1.0F,
                light, OverlayTexture.NO_OVERLAY);

        Item item = entity.getItem().getItem();
        var id = entity.getTextureId();
        if (item != Items.AIR && id != null) {

            RenderableDynamicTexture tex = DynamicTextureRenderer.requestFlatItemTexture(
                    id,
                    item,
                    ClientConfigs.TEXTURE_SIZE.get(),
                    i -> {
                        try {
                            LabelEntityRenderer.postProcess(i, entity.getColor());
                        } catch (Exception e) {
                            LabelsMod.LOGGER.warn("Failed to correctly create label image for {}:", i, e);
                        }
                    });

            if (tex != null) {

                boolean hasText = entity.hasText();

                //if(entity.hasGlowInk())
                //buffer = Minecraft.getInstance().renderBuffers().outlineBufferSource();

                VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(tex.getTextureLocation()));

                PoseStack.Pose pose = poseStack.last();
                int overlay = OverlayTexture.NO_OVERLAY;

                float z = 15.8f / 16f;

                float s = hasText ? 0.1875f : 0.25f;
                poseStack.translate(0.5, hasText ? 0.575 : 0.5, z);

                poseStack.pushPose();

                boolean glow = entity.hasGlowInk();
                if (glow) light = LightTexture.FULL_BRIGHT;

                vertexConsumer.addVertex(pose, -s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
                vertexConsumer.addVertex(pose, -s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);

                vertexConsumer.addVertex(pose, s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);
                vertexConsumer.addVertex(pose, s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, 0f, 0f, -1f);

                poseStack.popPose();

                if (hasText) drawLabelText(poseStack, buffer, entity, entity.getItem().getHoverName(), glow, light);
            }
        }

        poseStack.popPose();
    }


    //post process image
    private static void postProcess(NativeImage image, @Nullable DyeColor tint) {

        //tex.getPixels().flipY();

        boolean reduceColors = ClientConfigs.REDUCE_COLORS.get();
        boolean recolor = ClientConfigs.IS_RECOLORED.get();
        boolean outline = ClientConfigs.OUTLINE.get() && recolor; //outline shade comes from the recolor palette

        if (recolor || reduceColors) {
            //cleans image so we don't have similar colors
            SpriteUtils.mergeSimilarColors(image, 0.015f);
        }

        //taken off the untouched alpha channel, before anything writes to the image
        boolean[] outlineMask = outline ? findOutlinePixels(image) : null;

        if (recolor) SpriteUtils.grayscaleImage(image);

        if (reduceColors) {
            //reduce main image colors
            int cutoff = 11;
            IntUnaryOperator fn = i -> {
                if (i < cutoff) return i;
                else return (int) (Math.pow(i - cutoff + 1, 1 / 3f) + cutoff - 1);
            };
            //actually removes colors to have a palette around 13 (same as vanilla item textures)
            SpriteUtils.reduceColors(image, fn);
            //here we have a grayscale image with the amount of colors we want. Actual colors arent right yet
        }

        if (recolor) {
            BaseColor<?> dark = new RGBColor(ColorManager.getDark(tint));
            BaseColor<?> light = new RGBColor(ColorManager.getLight(tint));

            if (ClientConfigs.COLOR_PRESET.get() != ClientConfigs.Preset.DEFAULT) {
                dark = dark.asHCL();
                light = light.asHCL();
            }

            Palette old = Palette.fromImage(TextureImage.of(image, (McMetaFile) null), null, 0);
            int s = old.size();
            Palette newPalette;
            if (s < 3) {
                newPalette = Palette.ofColors(List.of(light.asRGB(), dark.asRGB()));
            } else {
                //one extra shade so the outline can claim it without stealing one from the item
                newPalette = Palette.fromArc(light.asRGB(), dark.asRGB(), s + (outline ? 1 : 0));
            }

            //index 0 is the darkest. If there's no spare shade just go one step darker than the item
            int outlineColor = !outline ? 0 : (newPalette.size() > s ? newPalette.remove(0) :
                    newPalette.getDarkest().getDarkened()).value();

            fastInPlaceRecolor(image, old, newPalette);

            if (outline) {
                SpriteUtils.forEachPixel(image, (x, y) -> {
                    if (outlineMask[y * image.getWidth() + x]) image.setPixelRGBA(x, y, outlineColor);
                });
            }
        }
        //image isn't closed as TextureImage just wraps native image so we cant close that
    }

    //transparent pixels touching the item. Reads only, so growing the outline can never feed back into itself
    private static boolean[] findOutlinePixels(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] mask = new boolean[width * height];
        SpriteUtils.forEachPixel(image, (x, y) -> {
            if (!isTransparent(image, x, y)) return;
            boolean touchesItem = (x > 0 && !isTransparent(image, x - 1, y)) ||
                    (x < width - 1 && !isTransparent(image, x + 1, y)) ||
                    (y > 0 && !isTransparent(image, x, y - 1)) ||
                    (y < height - 1 && !isTransparent(image, x, y + 1));
            if (touchesItem) mask[y * width + x] = true;
        });
        return mask;
    }

    private static boolean isTransparent(NativeImage image, int x, int y) {
        return RGBColor.getA(image.getPixelRGBA(x, y)) == 0;
    }

    //like with respriter but faster as palettes are already same size
    private static void fastInPlaceRecolor(NativeImage image, Palette old, Palette newPalette) {
        assert old.size() <= newPalette.size() : "Palettes must have same size";
        SpriteUtils.forEachPixel(image, (x, y) -> {

            int c = image.getPixelRGBA(x, y);
            //manual recolor cause faster since we are iterating anyway
            for (int i = 0; i < old.size(); i++) {
                if (old.getValues().get(i).value() == c) {
                    c = newPalette.getValues().get(i).value();
                    image.setPixelRGBA(x, y, c);
                    break;
                }
            }
        });
    }

    private void drawLabelText(PoseStack matrixStack, MultiBufferSource buffer,
                               LabelEntity entity, Component text, boolean glow, int light) {
        matrixStack.scale(-1, 1, -1);

        Font font = Minecraft.getInstance().font;

        matrixStack.pushPose();
        matrixStack.translate(0, 0.25, 0);


        if (entity.needsVisualUpdate()) {
            float paperHeight = 1 - (2 * 0.45f);
            float paperWidth = 1 - (2 * 0.275f);
            var pair = TextUtil.fitLinesToBox(font, text, paperWidth, paperHeight);
            entity.setLabelText(pair.getFirst().toArray(FormattedCharSequence[]::new));
            entity.setLabelTextScale(pair.getSecond());
        }

        float scale = entity.getLabelTextScale();
        FormattedCharSequence[] tempPageLines = entity.getLabelText();

        matrixStack.translate(0, -0.475, 0);

        matrixStack.scale(scale, -scale, scale);

        DyeColor c = DyeColor.BLACK;

        if (ClientConfigs.COLORED_TEXT.get()) {
            var d = entity.getColor();
            if (d != null) c = d;
        }

        TextUtil.renderAllLines(tempPageLines, 10, font, matrixStack, buffer,
                TextUtil.renderProperties(c, glow, 1.5f, light, Style.EMPTY,
                        entity.getDirection().step(),
                        () -> LOD.at(camera, entity.blockPosition()).isVeryNear()));

        matrixStack.popPose();
    }

    @Override
    public Vec3 getRenderOffset(LabelEntity entity, float partialTicks) {
        return Vec3.ZERO;
    }

    @Override
    public ResourceLocation getTextureLocation(LabelEntity labelEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    protected boolean shouldShowName(LabelEntity labelEntity) {
        return false;
    }

}