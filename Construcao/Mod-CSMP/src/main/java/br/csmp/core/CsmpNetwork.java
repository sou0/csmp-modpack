package br.csmp.core;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CsmpNetwork {
    private CsmpNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // optional(): quem nao tem o mod (ou o servidor sem ele) ainda consegue conectar
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(CaosPayload.TYPE, CaosPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientCaos.set(payload.caos())));
        registrar.playToClient(br.csmp.core.vazio.LuzesPayload.TYPE, br.csmp.core.vazio.LuzesPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> br.csmp.core.vazio.ClientLuzes.atualizar(payload)));
        registrar.playToClient(br.csmp.core.sanidade.SanidadePayload.TYPE, br.csmp.core.sanidade.SanidadePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> br.csmp.core.sanidade.ClientSanidade.atualizar(payload.insanidade())));
        registrar.playToClient(br.csmp.core.santuario.TremorPayload.TYPE, br.csmp.core.santuario.TremorPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> br.csmp.core.santuario.ClientTremor.tremer(payload.ticks(), payload.forca())));
        registrar.playToClient(br.csmp.core.santuario.RitualPayloads.AbrirMenu.TYPE, br.csmp.core.santuario.RitualPayloads.AbrirMenu.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> br.csmp.core.santuario.RitualTela.abrir(payload)));
        registrar.playToServer(br.csmp.core.santuario.RitualPayloads.Escolha.TYPE, br.csmp.core.santuario.RitualPayloads.Escolha.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer jogador) {
                        br.csmp.core.santuario.ServerSantuario.escolher(jogador, payload.base(), payload.preso(), payload.amuleto());
                    }
                }));
        registrar.playToClient(br.csmp.core.npc.AbrirProfetaPayload.TYPE, br.csmp.core.npc.AbrirProfetaPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> br.csmp.core.npc.ProfetaCliente.abrir(payload.eruditos(), payload.entidade())));
    }
}