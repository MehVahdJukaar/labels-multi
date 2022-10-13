package net.mehvahdjukaar.labels;

import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderedTexturesManager;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigSpec;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.util.math.colors.RGBColor;

import java.util.function.Supplier;

public class ClientConfigs {

    public static void init(){

    }

    public static ConfigSpec CONFIG_SPEC;

    public static Supplier<Boolean> HAS_TEXT;
    public static Supplier<Boolean> OUTLINE;
    public static Supplier<Boolean> IS_RECOLORED;
    public static Supplier<Boolean> REDUCE_COLORS;
    public static Supplier<Integer> TEXTURE_SIZE;
    public static Supplier<Integer> DARK_COLOR;
    public static Supplier<Integer> LIGHT_COLOR;

    static {
        ConfigBuilder builder = ConfigBuilder.create(LabelsMod.res("client"), ConfigType.CLIENT);

        HAS_TEXT = builder.comment("Draws item name on labels").define("draw_item_name", false);
        TEXTURE_SIZE = builder.comment("Item texture resolution. You might want to keep this multiples of 16")
                .define("texture_resolution", 16, 8, 1024);

        builder.push("color_settings");

        var dark = new RGBColor(64 / 255f, 34 / 255f, 0 / 255f, 1);
        //HCLColor light = new RGBColor(196 / 255f, 155 / 255f, 88 / 255f, 1).asHCL();
        var light = new RGBColor(235 / 255f, 213 / 255f, 178 / 255f, 1);


        IS_RECOLORED = builder.comment("Greyscales then recolors each item using the below provided colors").define("recolor_texture", true);
        DARK_COLOR = builder.comment("First color to use for recoloring. Middle colors are interpolated between the two")
                .defineColor("dark_color", dark.toInt());
        LIGHT_COLOR = builder.comment("Second color to use for recoloring. Middle colors are interpolated between the two")
                .defineColor("light_color", light.toInt());

        REDUCE_COLORS = builder.comment("Reduce colors of original image before processing. Makes 3d blocks more 2d like by giving them a limited palette")
                        .define("limit_palette", true);
        OUTLINE = builder.comment("Adds an outline to label images").define("outline",true);
        builder.pop();

        builder.onChange(RenderedTexturesManager::clearCache);

        CONFIG_SPEC = builder.buildAndRegister();
    }

}
