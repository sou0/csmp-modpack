package br.csmp.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: valor atual do Medidor de Caos (0 a 100). */
public record CaosPayload(int caos) implements CustomPacketPayload {
    public static final Type<CaosPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "caos"));
    public static final StreamCodec<ByteBuf, CaosPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(CaosPayload::new, CaosPayload::caos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}