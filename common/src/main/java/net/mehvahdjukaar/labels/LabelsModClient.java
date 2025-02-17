package net.mehvahdjukaar.labels;

import net.mehvahdjukaar.moonlight.api.client.util.RenderUtil;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class LabelsModClient {

    public static final ModelResourceLocation LABEL_MODEL = RenderUtil.getStandaloneModelLocation(LabelsMod.res("block/label"));

    public static void init() {
        ClientConfigs.init();
        ClientHelper.addSpecialModelRegistration(LabelsModClient::registerSpecialModels);
        ClientHelper.addEntityRenderersRegistration(LabelsModClient::registerEntityRenderers);
        ClientHelper.addClientReloadListener(ColorManager::new, LabelsMod.res("label_colors"));

    }

    @EventCalled
    private static void registerSpecialModels(ClientHelper.SpecialModelEvent event) {
        event.register(LABEL_MODEL);
    }

    @EventCalled
    private static void registerEntityRenderers(ClientHelper.EntityRendererEvent event) {
        //entities
        event.register(LabelsMod.LABEL.get(), LabelEntityRenderer::new);
    }
}
