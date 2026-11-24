package br.csmp.core.sanidade;

import br.csmp.core.CsmpMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: a insanidade do proprio jogador (0 a 100), so para desenhar os efeitos na tela. */
public record SanidadePayload(float insanidade) implements CustomPacketPayload {
    public static final Type<SanidadePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "sanidade"));
    public static final StreamCodec<ByteBuf, SanidadePayload> CODEC =
            ByteBufCodecs.FLOAT.map(SanidadePayload::new, SanidadePayload::insanidade);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
