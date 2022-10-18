package net.mehvahdjukaar.labels;

import net.mehvahdjukaar.moonlight.api.client.GenericSimpleResourceReloadListener;
import net.mehvahdjukaar.moonlight.api.resources.textures.SpriteUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;

public class ColorManager extends GenericSimpleResourceReloadListener {

    public static final ColorManager RELOAD_INSTANCE = new ColorManager();

    public static int getDark() {
        return DARK;
    }

    public static int getLight() {
        return LIGHT;
    }

    private static int DARK = 0;
    private static int LIGHT = 0;

    public ColorManager() {
        super("textures/entity", "label_colors.png");
    }

    @Override
    public void apply(List<ResourceLocation> locations, ResourceManager manager, ProfilerFiller filler) {

        for (var res : locations) {
            var l = SpriteUtils.parsePaletteStrip(manager,
                    new ResourceLocation(res.getNamespace(),"textures/entity/label_colors.png"), 2);
            DARK = l.get(0);
            LIGHT = l.get(1);
        }

    }
}
