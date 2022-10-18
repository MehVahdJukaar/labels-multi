package net.mehvahdjukaar.labels;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientPlatformHelper;
import net.minecraft.resources.ResourceLocation;

public class LabelsModClient {

    public static final ResourceLocation LABEL_MODEL = LabelsMod.res("block/label");

    public static void init() {
        ClientConfigs.init();
        ClientPlatformHelper.addSpecialModelRegistration(LabelsModClient::registerSpecialModels);
        ClientPlatformHelper.addEntityRenderersRegistration(LabelsModClient::registerEntityRenderers);
        ClientPlatformHelper.addClientReloadListener(ColorManager.RELOAD_INSTANCE, LabelsMod.res("label_colors"));

    }

    @EventCalled
    private static void registerSpecialModels(ClientPlatformHelper.SpecialModelEvent event) {
        event.register(LABEL_MODEL);
    }

    @EventCalled
    private static void registerEntityRenderers(ClientPlatformHelper.EntityRendererEvent event) {
        //entities
        event.register(LabelsMod.LABEL.get(), LabelEntityRenderer::new);
    }
}
