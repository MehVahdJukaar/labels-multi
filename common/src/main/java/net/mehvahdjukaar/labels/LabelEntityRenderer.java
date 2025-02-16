package net.mehvahdjukaar.labels;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.FrameBufferBackedDynamicTexture;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
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
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
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

        if(this.entityRenderDispatcher.shouldRenderHitBoxes()){
            BlockPos behind = entity.getPos();
            VertexConsumer lines = buffer.getBuffer(RenderType.lines());
            poseStack.pushPose();
            var ep = entity.position();
            Vec3 vec3 = new Vec3(behind.getX() - ep.x, behind.getY() - ep.y, behind.getZ() - ep.z);
            AABB bb = new AABB(vec3, vec3.add(1,1,1)).inflate(0.01);
            LevelRenderer.renderLineBox(poseStack, lines, bb, 1.0F, 0, 0, 1.0F);
            poseStack.popPose();
        }

        poseStack.pushPose();

        //prevents incorrect rendering on first frame

        poseStack.mulPose(Axis.YP.rotationDegrees(180 - entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees( - entity.getXRot()));

        poseStack.translate(0, -0, -0.5 + 1 / 32f);
        poseStack.translate(-0.5, -0.5, -0.5);

        modelRenderer.renderModel(poseStack.last(), buffer.getBuffer(Sheets.cutoutBlockSheet()), //
                null, ClientHelper.getModel(modelManager, LabelsModClient.LABEL_MODEL), 1.0F, 1.0F, 1.0F,
                light, OverlayTexture.NO_OVERLAY);

        Item item = entity.getItem().getItem();
        var id = entity.getTextureId();
        if (item != Items.AIR && id != null) {

            FrameBufferBackedDynamicTexture tex = RenderedTexturesManager.requestFlatItemTexture(
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

            if (tex.isInitialized()) {

                boolean hasText = entity.hasText();

                //if(entity.hasGlowInk())
                //buffer = Minecraft.getInstance().renderBuffers().outlineBufferSource();

                VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(tex.getTextureLocation()));

                Matrix4f tr = poseStack.last().pose();
                Matrix3f normal = poseStack.last().normal();
                int overlay = OverlayTexture.NO_OVERLAY;

                float z = 15.8f / 16f;

                float s = hasText ? 0.1875f : 0.25f;
                poseStack.translate(0.5, hasText ? 0.575 : 0.5, z);

                poseStack.pushPose();

                boolean glow = entity.hasGlowInk();
                if (glow) light = LightTexture.FULL_BRIGHT;

                vertexConsumer.vertex(tr, -s, -s, 0).color(1f, 1f, 1f, 1f).uv(1f, 0f).overlayCoords(overlay).uv2(light).normal(normal, 0f, 0f, -1f).endVertex();
                vertexConsumer.vertex(tr, -s, s, 0).color(1f, 1f, 1f, 1f).uv(1f, 1f).overlayCoords(overlay).uv2(light).normal(normal, 0f, 0f, -1f).endVertex();

                vertexConsumer.vertex(tr, s, s, 0).color(1f, 1f, 1f, 1f).uv(0f, 1f).overlayCoords(overlay).uv2(light).normal(normal, 0f, 0f, -1f).endVertex();
                vertexConsumer.vertex(tr, s, -s, 0).color(1f, 1f, 1f, 1f).uv(0f, 0f).overlayCoords(overlay).uv2(light).normal(normal, 0f, 0f, -1f).endVertex();

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
        boolean outline = ClientConfigs.OUTLINE.get() && recolor; //won't even attempt adding an outline if we don't grayscale first

        if (recolor || reduceColors) {
            //cleans image so we don't have similar colors
            SpriteUtils.mergeSimilarColors(image, 0.015f);
        }

        if (recolor) SpriteUtils.grayscaleImage(image);

        TextureImage originalTexture = TextureImage.of(image, (McMetaFile) null);
        TextureImage outlineTexture; //close this!
        if (outline) {
            List<Pair<Integer, Integer>> outlinePosition = new ArrayList<>();
            outlineTexture = originalTexture.makeCopy();
            //find edges position
            SpriteUtils.forEachPixel(outlineTexture.getImage(), (x, y) -> {
                var c = outlineTexture.getImage().getPixelRGBA(x, y);
                if (new RGBColor(c).alpha() != 0) {
                    if ((x == 0 || new RGBColor(image.getPixelRGBA(x - 1, y)).alpha() == 0) ||
                            (x == image.getWidth() - 1 || new RGBColor(image.getPixelRGBA(x + 1, y)).alpha() == 0) ||
                            (y == 0 || new RGBColor(image.getPixelRGBA(x, y - 1)).alpha() == 0) ||
                            (y == image.getHeight() - 1 || new RGBColor(image.getPixelRGBA(x, y + 1)).alpha() == 0)) {
                        outlinePosition.add(Pair.of(x, y));
                        //image.setPixelRGBA(x, y, dark.asRGB().mixWith(new RGBColor(c), 0.2f).toInt());
                    }
                }
            });
            SpriteUtils.forEachPixel(outlineTexture.getImage(), (x, y) -> {
                if (!outlinePosition.contains(Pair.of(x, y))) {
                    //remove inner
                    outlineTexture.getImage().setPixelRGBA(x, y, 0);
                } else {
                    //remove edges
                    originalTexture.getImage().setPixelRGBA(x, y, 0);
                }
            });


        } else {
            outlineTexture = null;
        }

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

            //reduce outline colors
            if (outlineTexture != null) {
                int maxOutlineColors = 3;
                try {
                    SpriteUtils.reduceColors(outlineTexture.getImage(), (IntUnaryOperator) j -> Math.min(j, maxOutlineColors));
                } catch (Exception ignored) {
                }
            }
        }

        if (recolor) {


            BaseColor<?> dark = new RGBColor(ColorManager.getDark(tint));
            BaseColor<?> light = new RGBColor(ColorManager.getLight(tint));

            if (ClientConfigs.COLOR_PRESET.get() != ClientConfigs.Preset.DEFAULT) {
                dark = dark.asHCL();
                light = light.asHCL();
            }

            Palette old = Palette.fromImage(originalTexture, null, 0);
            int s = old.size();
            Palette newPalette;
            if (s < 3) {
                newPalette = Palette.ofColors(List.of(light.asRGB(), dark.asRGB()));
            } else {
                newPalette = Palette.fromArc(light.asRGB(), dark.asRGB(), s + (outline ? 2 : 0));
            }

            if (outline) {
                Palette newOutlinePalette;
                if (newPalette.size() > 4) {
                    //split palette to use some colors for outline
                    newOutlinePalette = Palette.ofColors(List.of(newPalette.remove(0).rgb()));
                    var v = newPalette.remove(0);
                    newOutlinePalette.add(v);
                    newOutlinePalette.add(newPalette.getDarkest()); //they'll have 1 shared color
                    //newPalette.add(v);
                } else {
                    newOutlinePalette = newPalette.copy();
                    newOutlinePalette.add(newPalette.getDarkest().getDarkened());
                }
                fastInPlaceRecolor(outlineTexture.getImage(), Palette.fromImage(outlineTexture), newOutlinePalette);
            }
            fastInPlaceRecolor(image, old, newPalette);
        }

        if (outlineTexture != null) {
            originalTexture.applyOverlay(outlineTexture);
            outlineTexture.close();
        }
        //original isn't closed as TextureImage just wraps native image so we cant close that
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
                        () -> new LOD(camera, entity.getPos()).isVeryNear()));

        matrixStack.popPose();
    }

    @Override
    public Vec3 getRenderOffset(LabelEntity entity, float partialTicks) {
        return Vec3.ZERO;
    }

    @Override
    public ResourceLocation getTextureLocation(LabelEntity labelEntity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    protected boolean shouldShowName(LabelEntity labelEntity) {
        return false;
    }

}