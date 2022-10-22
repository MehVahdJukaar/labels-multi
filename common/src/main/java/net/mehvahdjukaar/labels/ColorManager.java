package net.mehvahdjukaar.labels;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight2.api.client.GenericSimpleResourceReloadListener;
import net.mehvahdjukaar.moonlight2.api.resources.textures.SpriteUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorManager extends GenericSimpleResourceReloadListener {

    public static final ColorManager RELOAD_INSTANCE = new ColorManager();

    private static final Map<DyeColor, Pair<Integer,Integer>> COLORS = new HashMap<>();

    public static int getDark(@Nullable DyeColor color) {
        return COLORS.get(color).getSecond();
    }

    public static int getLight(@Nullable DyeColor color) {
        return COLORS.get(color).getFirst();
    }


    public ColorManager() {
        super("textures/entity", "label_colors.png");
    }

    @Override
    public void apply(List<ResourceLocation> locations, ResourceManager manager, ProfilerFiller filler) {

        var s = ClientConfigs.COLOR_PRESET.get();
        for (var res : locations) {
            if(res.getPath().equals(s.getName())) {
                var l = SpriteUtils.parsePaletteStrip(manager,
                        new ResourceLocation(res.getNamespace(), "textures/entity/"+res.getPath()+"label_colors.png"), 32 + 2);
                var i = l.iterator();
                COLORS.put(null, Pair.of(i.next(), i.next()));
                for (var d : DyeColor.values()) {
                    COLORS.put(d, Pair.of(i.next(), i.next()));
                }
            }
        }
    }
}
