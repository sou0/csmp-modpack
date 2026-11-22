package br.csmp.core.npc;

import br.csmp.core.CsmpMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: abre a tela do Profeta, com o numero de Eruditos ja libertados. */
public record AbrirProfetaPayload(int eruditos) implements CustomPacketPayload {
    public static final Type<AbrirProfetaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "abrir_profeta"));
    public static final StreamCodec<ByteBuf, AbrirProfetaPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(AbrirProfetaPayload::new, AbrirProfetaPayload::eruditos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
