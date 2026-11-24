package br.csmp.core.santuario;

import br.csmp.core.CsmpMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: treme a camera por alguns ticks (estatua sendo montada, quebrando, ritual). */
public record TremorPayload(int ticks, float forca) implements CustomPacketPayload {
    public static final Type<TremorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "tremor"));
    public static final StreamCodec<ByteBuf, TremorPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TremorPayload::ticks,
            ByteBufCodecs.FLOAT, TremorPayload::forca,
            TremorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
