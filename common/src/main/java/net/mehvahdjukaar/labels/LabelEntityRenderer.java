package net.mehvahdjukaar.labels;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.DynamicTextureRenderer;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderableDynamicTexture;
import net.mehvahdjukaar.moonlight.api.client.util.LOD;
import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.IntUnaryOperator;


public class LabelEntityRenderer extends EntityRenderer<LabelEntity, LabelEntityRenderer.LabelRenderState> {

    private final RandomSource random = RandomSource.create();
    private final Font font;
    private final Camera camera;

    public LabelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
        this.camera = Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    public static class LabelRenderState extends EntityRenderState {
        public float yRot;
        public float xRot;
        public Vector3f facingNormal = new Vector3f();
        @Nullable
        public Identifier texture;
        public boolean hasText;
        public boolean glowInk;
        @Nullable
        public DyeColor color;
        public boolean veryNear;
        public FormattedCharSequence[] text;
        public float textScale;
    }

    @Override
    public LabelRenderState createRenderState() {
        return new LabelRenderState();
    }

    @Override
    public void extractRenderState(LabelEntity entity, LabelRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
        state.facingNormal = entity.getDirection().step();
        state.hasText = entity.hasText();
        state.glowInk = entity.hasGlowInk();
        state.color = entity.getColor();
        state.veryNear = LOD.at(camera, entity.blockPosition()).isVeryNear();

        state.texture = null;
        Item item = entity.getItem().getItem();
        Identifier id = entity.getTextureId();
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
            if (tex != null) state.texture = tex.getTextureLocation();
        }

        if (state.hasText) {
            if (entity.needsVisualUpdate()) {
                float paperHeight = 1 - (2 * 0.45f);
                float paperWidth = 1 - (2 * 0.275f);
                var pair = TextUtil.fitLinesToBox(font, entity.getItem().getHoverName(), paperWidth, paperHeight);
                entity.setLabelText(pair.getFirst().toArray(FormattedCharSequence[]::new));
                entity.setLabelTextScale(pair.getSecond());
            }
            state.text = entity.getLabelText();
            state.textScale = entity.getLabelTextScale();
        }
    }

    @Override
    public void submit(LabelRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot));

        poseStack.translate(0, -0, -0.5 + 1 / 32f);
        poseStack.translate(-0.5, -0.5, -0.5);

        BlockStateModel model = ClientHelper.getStandaloneModel(LabelsModClient.LABEL_MODEL);
        if (model != null) {
            RenderUtil.submitBlockModel(poseStack, collector, model, null, null, null, random, true,
                    state.lightCoords, OverlayTexture.NO_OVERLAY);
        }

        if (state.texture != null) {
            boolean hasText = state.hasText;
            boolean glow = state.glowInk;
            int light = glow ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;

            float z = 15.8f / 16f;
            float s = hasText ? 0.1875f : 0.25f;
            poseStack.translate(0.5, hasText ? 0.575 : 0.5, z);

            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(state.texture), (pose, buffer) -> {
                buffer.addVertex(pose, -s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f);
                buffer.addVertex(pose, -s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f);

                buffer.addVertex(pose, s, s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f);
                buffer.addVertex(pose, s, -s, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 0f, -1f);
            });

            if (hasText) submitLabelText(poseStack, collector, state, glow, light);
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
        boolean inside = ClientConfigs.OUTLINE_POSITION.get() == ClientConfigs.OutlinePosition.INSIDE;
        int thickness = ClientConfigs.OUTLINE_THICKNESS.get();
        boolean[] outlineMask = outline ? findOutlinePixels(image, thickness, inside) : null;

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
                    if (outlineMask[y * image.getWidth() + x]) image.setPixelABGR(x, y, outlineColor);
                });
            }
        }
        //image isn't closed as TextureImage just wraps native image so we cant close that
    }

    //Marks the pixels the outline should cover. Reads only off the alpha channel, so growing it can never feed back into itself.
    //OUTSIDE marks transparent pixels around the item, INSIDE marks the item's own edge pixels. Thickness grows the ring by that many pixels.
    private static boolean[] findOutlinePixels(NativeImage image, int thickness, boolean inside) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] mask = new boolean[width * height];
        boolean[] frontier = new boolean[width * height];

        //first ring right at the silhouette boundary
        SpriteUtils.forEachPixel(image, (x, y) -> {
            boolean transparent = isTransparent(image, x, y);
            boolean onOutline;
            if (inside) {
                //solid pixel touching transparency or the image border
                onOutline = !transparent && (x == 0 || x == width - 1 || y == 0 || y == height - 1 ||
                        isTransparent(image, x - 1, y) || isTransparent(image, x + 1, y) ||
                        isTransparent(image, x, y - 1) || isTransparent(image, x, y + 1));
            } else {
                //transparent pixel touching the item
                onOutline = transparent && ((x > 0 && !isTransparent(image, x - 1, y)) ||
                        (x < width - 1 && !isTransparent(image, x + 1, y)) ||
                        (y > 0 && !isTransparent(image, x, y - 1)) ||
                        (y < height - 1 && !isTransparent(image, x, y + 1)));
            }
            if (onOutline) {
                mask[y * width + x] = true;
                frontier[y * width + x] = true;
            }
        });

        //dilate the ring for the remaining thickness. OUTSIDE spreads into transparent pixels, INSIDE into solid ones.
        boolean[] current = frontier;
        for (int step = 1; step < thickness; step++) {
            boolean[] next = new boolean[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!current[y * width + x]) continue;
                    growOutline(image, mask, next, x - 1, y, width, inside);
                    growOutline(image, mask, next, x + 1, y, width, inside);
                    growOutline(image, mask, next, x, y - 1, width, inside);
                    growOutline(image, mask, next, x, y + 1, width, inside);
                }
            }
            current = next;
        }
        return mask;
    }

    private static void growOutline(NativeImage image, boolean[] mask, boolean[] next, int x, int y, int width, boolean inside) {
        if (x < 0 || y < 0 || x >= width || y >= image.getHeight()) return;
        int idx = y * width + x;
        if (mask[idx]) return;
        //INSIDE eats into the item, OUTSIDE fills the surrounding transparency
        if (isTransparent(image, x, y) == inside) return;
        mask[idx] = true;
        next[idx] = true;
    }

    private static boolean isTransparent(NativeImage image, int x, int y) {
        return RGBColor.getA(image.getPixelABGR(x, y)) == 0;
    }

    //like with respriter but faster as palettes are already same size
    private static void fastInPlaceRecolor(NativeImage image, Palette old, Palette newPalette) {
        assert old.size() <= newPalette.size() : "Palettes must have same size";
        SpriteUtils.forEachPixel(image, (x, y) -> {

            int c = image.getPixelABGR(x, y);
            //manual recolor cause faster since we are iterating anyway
            for (int i = 0; i < old.size(); i++) {
                if (old.getValues().get(i).value() == c) {
                    c = newPalette.getValues().get(i).value();
                    image.setPixelABGR(x, y, c);
                    break;
                }
            }
        });
    }

    private void submitLabelText(PoseStack poseStack, SubmitNodeCollector collector,
                                 LabelRenderState state, boolean glow, int light) {
        poseStack.scale(-1, 1, -1);

        poseStack.pushPose();
        poseStack.translate(0, 0.25, 0);

        poseStack.translate(0, -0.475, 0);

        float scale = state.textScale;
        poseStack.scale(scale, -scale, scale);

        DyeColor c = DyeColor.BLACK;

        if (ClientConfigs.COLORED_TEXT.get()) {
            if (state.color != null) c = state.color;
        }

        TextUtil.submitAllLines(state.text, 10, font, poseStack, collector,
                TextUtil.renderProperties(c, glow, 1.5f, light, Style.EMPTY,
                        state.facingNormal, () -> state.veryNear));

        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(LabelEntity labelEntity, double distanceToCameraSq) {
        return false;
    }

}
