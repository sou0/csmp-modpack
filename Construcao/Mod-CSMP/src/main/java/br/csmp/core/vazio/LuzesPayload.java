package br.csmp.core.vazio;

import br.csmp.core.CsmpMod;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Servidor -> jogadores no Vazio: onde estao os jogadores do overworld. Cada um vira uma "luz"
 * que se move no Vazio de acordo com a movimentacao do jogador la fora.
 */
public record LuzesPayload(List<Luz> luzes) implements CustomPacketPayload {

    public record Luz(long idAlto, long idBaixo, double x, double z) {}

    public static final Type<LuzesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "luzes"));

    public static final StreamCodec<ByteBuf, LuzesPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeInt(p.luzes().size());
                for (Luz l : p.luzes()) {
                    buf.writeLong(l.idAlto());
                    buf.writeLong(l.idBaixo());
                    buf.writeDouble(l.x());
                    buf.writeDouble(l.z());
                }
            },
            buf -> {
                int n = Math.min(buf.readInt(), 256);
                List<Luz> lista = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    lista.add(new Luz(buf.readLong(), buf.readLong(), buf.readDouble(), buf.readDouble()));
                }
                return new LuzesPayload(lista);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
