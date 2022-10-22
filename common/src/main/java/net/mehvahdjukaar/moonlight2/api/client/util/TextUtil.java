package net.mehvahdjukaar.moonlight2.api.client.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TextUtil {

    private static final FormattedCharSequence CURSOR_MARKER = FormattedCharSequence.forward("_", Style.EMPTY);

    //IDK how this works anymore...

    /**
     * Scales and splits the given lines such that they fix in the given area with the maximum possible scale
     *
     * @param text   original text
     * @param width  box width
     * @param height box height
     * @return Pair of split lines and scale at which they should be rendered
     */
    public static Pair<List<FormattedCharSequence>, Float> fitLinesToBox(Font font, FormattedText text, float width, float height) {
        int scalingFactor;
        List<FormattedCharSequence> splitLines;
        int fontWidth = font.width(text);

        float maxLines;
        do {
            scalingFactor = Mth.floor(Mth.sqrt((fontWidth * 8f) / (width * height)));

            splitLines = font.split(text, Mth.floor(width * scalingFactor));
            //tempPageLines = RenderComponentsUtil.splitText(txt, MathHelper.floor(lx * scalingfactor), font, true, true);

            maxLines = height * scalingFactor / 8f;
            fontWidth += 1;
            // when lines fully filled @scaling factor > actual lines -> no overflow lines
        } while (maxLines < splitLines.size());

        return Pair.of(splitLines, 1f / scalingFactor);
    }

    //not server safe don't use
    @Deprecated(forRemoval = true)
    public static String getReadableName(String name) {
        return Arrays.stream((name).replace(":", "_").split("_"))
                .map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    public static FormattedText parseText(String s) {
        try {
            FormattedText mutableComponent = Component.Serializer.fromJson(s);
            if (mutableComponent != null) {
                return mutableComponent;
            }
        } catch (Exception ignored) {
        }
        return FormattedText.of(s);
    }



    /**
     * Render text line in world
     */
    public static void renderLine(FormattedCharSequence formattedCharSequences, Font font, float yOffset, PoseStack poseStack,
                                  MultiBufferSource buffer, RenderTextProperties properties) {
        if (formattedCharSequences == null) return;
        float x = (float) (-font.width(formattedCharSequences) / 2);
        renderLineInternal(formattedCharSequences, font, x, yOffset, poseStack.last().pose(), buffer, properties);
    }

    /**
     * Renders multiple lines in world
     */
    public static void renderAllLines(FormattedCharSequence[] charSequences, int ySeparation, Font font, PoseStack poseStack,
                                      MultiBufferSource buffer, RenderTextProperties properties) {
        for (int i = 0; i < charSequences.length; i++) {
            renderLine(charSequences[i], font, ySeparation * i, poseStack, buffer, properties);
        }
    }

    private static void renderLineInternal(FormattedCharSequence formattedCharSequences, Font font, float xOffset, float yOffset,
                                           Matrix4f matrix4f, MultiBufferSource buffer, RenderTextProperties properties) {
        if (properties.glowing) {
            font.drawInBatch8xOutline(formattedCharSequences, xOffset, yOffset, properties.textColor, properties.darkenedColor,
                    matrix4f, buffer, properties.light);
        } else {
            font.drawInBatch(formattedCharSequences, xOffset, yOffset, properties.darkenedColor, false,
                    matrix4f, buffer, false, 0, properties.light);
        }
    }


    private static int getDarkenedColor(int color, boolean glowing) {
        if (color == DyeColor.BLACK.getTextColor() && glowing) return 0xFFF0EBCC;
        return getDarkenedColor(color, 0.4f);
    }

    private static int getDarkenedColor(int color, float amount) {
        int j = (int) ((double) NativeImage.getR(color) * amount);
        int k = (int) ((double) NativeImage.getG(color) * amount);
        int l = (int) ((double) NativeImage.getB(color) * amount);
        return NativeImage.combine(0, l, k, j);
    }

    /**
     * bundles all data needed to render a generic text line. Useful for signs like blocks
     */
    public record RenderTextProperties(int textColor, int darkenedColor, boolean glowing, int light, Style style) {

        public RenderTextProperties(DyeColor color, boolean glowing, int combinedLight, Style style, Supplier<Boolean> isVeryNear) {
            this(color.getTextColor(),
                    getDarkenedColor(color.getTextColor(), glowing),
                    glowing && (isVeryNear.get() || color == DyeColor.BLACK),
                    glowing ? combinedLight : LightTexture.FULL_BRIGHT, style);

        }
    }

}
