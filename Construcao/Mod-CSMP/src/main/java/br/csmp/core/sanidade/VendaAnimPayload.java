package br.csmp.core.sanidade;

import br.csmp.core.CsmpMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> clientes: toca a animacao de arrancar a venda no jogador com este id. */
public record VendaAnimPayload(int entidade) implements CustomPacketPayload {
    public static final Type<VendaAnimPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "venda_anim"));
    public static final StreamCodec<ByteBuf, VendaAnimPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(VendaAnimPayload::new, VendaAnimPayload::entidade);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
