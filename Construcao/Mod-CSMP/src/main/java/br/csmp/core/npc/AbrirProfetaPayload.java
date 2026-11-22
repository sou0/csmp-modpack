package br.csmp.core.npc;

import br.csmp.core.CsmpMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Servidor -> cliente: abre a tela do Profeta.
 * eruditos = quantos Eruditos ja foram libertados; entidade = id do NPC (para mostrar o retrato).
 */
public record AbrirProfetaPayload(int eruditos, int entidade) implements CustomPacketPayload {
    public static final Type<AbrirProfetaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "abrir_profeta"));
    public static final StreamCodec<ByteBuf, AbrirProfetaPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbrirProfetaPayload::eruditos,
            ByteBufCodecs.VAR_INT, AbrirProfetaPayload::entidade,
            AbrirProfetaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
