package br.csmp.core;

import br.csmp.core.mob.MonstroCorruptoModel;
import br.csmp.core.mob.MonstroCorruptoRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Registros so do cliente (modelos e renderizadores). */
@EventBusSubscriber(modid = CsmpMod.ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void camadas(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MonstroCorruptoModel.LAYER, MonstroCorruptoModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderizadores(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CsmpRegistro.MONSTRO_CORRUPTO.get(), MonstroCorruptoRenderer::new);
    }
}