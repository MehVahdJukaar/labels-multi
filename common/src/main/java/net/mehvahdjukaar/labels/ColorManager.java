package net.mehvahdjukaar.labels;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.resources.textures.SpriteUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorManager extends SimplePreparableReloadListener<List<Integer>> {

    private static final Map<DyeColor, Pair<Integer, Integer>> COLORS = new HashMap<>();

    public static int getDark(@Nullable DyeColor color) {
        Pair<Integer, Integer> integerIntegerPair = COLORS.get(color);
        return integerIntegerPair.getSecond();
    }

    public static int getLight(@Nullable DyeColor color) {
        return COLORS.get(color).getFirst();
    }

    @Override
    protected List<Integer> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        ClientConfigs.Preset p = ClientConfigs.COLOR_PRESET.get();
        return SpriteUtils.parsePaletteStrip(resourceManager,
                LabelsMod.res("textures/entity/" + p.getName() + "label_colors.png"),
                DyeColor.values().length * 2 + 2);
    }

    @Override
    public void apply(List<Integer> palette, ResourceManager manager, ProfilerFiller filler) {
        var i = palette.iterator();
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
