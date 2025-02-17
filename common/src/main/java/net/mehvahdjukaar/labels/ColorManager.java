package net.mehvahdjukaar.labels;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.resources.textures.SpriteUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorManager extends SimplePreparableReloadListener<List<ResourceLocation>> {

    private static final Map<DyeColor, Pair<Integer, Integer>> COLORS = new HashMap<>();

    public static int getDark(@Nullable DyeColor color) {
        return COLORS.get(color).getSecond();
    }

    public static int getLight(@Nullable DyeColor color) {
        return COLORS.get(color).getFirst();
    }


    @Override
    protected List<ResourceLocation> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        FileToIdConverter fileToIdConverter = new FileToIdConverter("textures/entity", ".png");
        var iter = fileToIdConverter.listMatchingResources(resourceManager).entrySet().iterator();
        List<ResourceLocation> list = new ArrayList<>();
        iter.forEachRemaining((entry) -> list.add(entry.getKey()));
        return list;
    }


    @Override
    public void apply(List<ResourceLocation> locations, ResourceManager manager, ProfilerFiller filler) {

        var s = ClientConfigs.COLOR_PRESET.get();
        for (var res : locations) {
            if (res.getPath().equals(s.getName())) {
                var l = SpriteUtils.parsePaletteStrip(manager,
                        res.withPath("textures/entity/" + res.getPath() + "label_colors.png"),
                        DyeColor.values().length * 2 + 2);
                var i = l.iterator();
                COLORS.put(null, Pair.of(i.next(), i.next()));
                for (var d : DyeColor.values()) {
                    if (i.hasNext()) {
                        COLORS.put(d, Pair.of(i.next(), i.next()));
                    } else {
                        //default for tinted
                        int first = d.getFireworkColor();
                        int second = d.getTextColor();
                        if (first == second) second = d.getMapColor().col;
                        if (first == second) {
                            second = FastColor.ARGB32.multiply(first, 0x101010ff);
                        }
                        COLORS.put(d, Pair.of(first, second));
                    }
                }
            }
        }
    }
}
