package net.mehvahdjukaar.moonlight2.api.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class GenericSimpleResourceReloadListener implements PreparableReloadListener {
    private final String pathSuffix;
    private final int suffixLength;
    private final String directory;

    public GenericSimpleResourceReloadListener(String path, String suffix) {
        this.directory = path;
        this.pathSuffix = suffix;
        this.suffixLength = pathSuffix.length();
    }

    @Override
    final public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager manager,
                                                ProfilerFiller workerProfiler, ProfilerFiller mainProfiler,
                                                Executor workerExecutor, Executor mainExecutor) {
        var list = prepare(manager, mainProfiler);
        this.apply(list, manager, workerProfiler);

        return CompletableFuture.supplyAsync(() -> null, workerExecutor)
                .thenCompose(stage::wait)
                .thenAcceptAsync((noResult) -> {
                }, mainExecutor);
    }

    protected List<ResourceLocation> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        List<ResourceLocation> list = new ArrayList<>();
        int i = this.directory.length() + 1;

        for (ResourceLocation resourceLocation : resourceManager.listResources(this.directory, stringx -> stringx.endsWith(pathSuffix))) {
            String string = resourceLocation.getPath();
            ResourceLocation resourceLocation2 = new ResourceLocation(
                    resourceLocation.getNamespace(), string.substring(i, string.length() - suffixLength)
            );
            list.add(resourceLocation2);
        }

        return list;
    }

    public abstract void apply(List<ResourceLocation> locations, ResourceManager manager, ProfilerFiller filler);
}
