package net.mehvahdjukaar.labels;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.minecraft.resources.Identifier;

public class LabelsModClient {

    public static final Identifier LABEL_MODEL = LabelsMod.res("block/label");

    public static void init() {
        ClientConfigs.init();
        ClientHelper.addStandaloneModelRegistration(LabelsModClient::registerStandaloneModels);
        ClientHelper.addEntityRenderersRegistration(LabelsModClient::registerEntityRenderers);
        ClientHelper.addClientReloadListener(ColorManager::new, LabelsMod.res("label_colors"));

    }

    @EventCalled
    private static void registerStandaloneModels(ClientHelper.StandaloneModelEvent event) {
        event.register(LABEL_MODEL);
    }

    @EventCalled
    private static void registerEntityRenderers(ClientHelper.EntityRendererEvent event) {
        //entities
        event.register(LabelsMod.LABEL.get(), LabelEntityRenderer::new);
    }
}
