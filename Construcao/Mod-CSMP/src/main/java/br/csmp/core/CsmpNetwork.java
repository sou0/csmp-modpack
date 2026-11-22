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
        registrar.playToClient(br.csmp.core.npc.AbrirProfetaPayload.TYPE, br.csmp.core.npc.AbrirProfetaPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> br.csmp.core.npc.ProfetaCliente.abrir(payload.eruditos(), payload.entidade())));
    }
}