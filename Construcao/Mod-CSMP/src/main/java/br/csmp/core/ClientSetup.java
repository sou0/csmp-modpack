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

    /** Ceu proprio do Vazio (sem sol, lua, estrelas, nuvens nem chuva desenhada). */
    @SubscribeEvent
    public static void efeitosDeDimensao(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(br.csmp.core.vazio.Vazio.ID, new br.csmp.core.vazio.VazioEfeitos());
    }

    @SubscribeEvent
    public static void blocosComDesenho(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CsmpRegistro.SIGILO_BE.get(), br.csmp.core.santuario.SigiloRenderer::new);
    }

    @SubscribeEvent
    public static void camadas(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MonstroCorruptoModel.LAYER, MonstroCorruptoModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderizadores(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CsmpRegistro.MONSTRO_CORRUPTO.get(), MonstroCorruptoRenderer::new);
        event.registerEntityRenderer(CsmpRegistro.ESTATUA.get(), br.csmp.core.santuario.EstatuaRenderer::new);
        event.registerEntityRenderer(CsmpRegistro.PROFETA.get(), br.csmp.core.npc.ProfetaRenderer::new);
        event.registerEntityRenderer(CsmpRegistro.SOMBRA.get(), br.csmp.core.npc.VazioNpcRenderer::new);
        event.registerEntityRenderer(CsmpRegistro.CRIANCA.get(), br.csmp.core.npc.VazioNpcRenderer::new);
        event.registerEntityRenderer(CsmpRegistro.SENTINELA.get(), br.csmp.core.npc.VazioNpcRenderer::new);
    }
}