package br.csmp.core.santuario;

import br.csmp.core.CsmpMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Pacotes do menu de reviver (servidor abre a lista; cliente devolve a escolha). */
public final class RitualPayloads {
    private RitualPayloads() {}

    /** Um preso que pode ser escolhido: perto de um farol aceso ligado a esta estatua. */
    public record Preso(UUID id, String nome, float insanidade) {
        public static final StreamCodec<ByteBuf, Preso> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Preso::id,
                ByteBufCodecs.STRING_UTF8, Preso::nome,
                ByteBufCodecs.FLOAT, Preso::insanidade,
                Preso::new);
    }

    /** Servidor -> cliente: abre o menu com os presos que podem ser revividos por esta estatua. */
    public record AbrirMenu(BlockPos base, boolean amuleto, List<Preso> presos) implements CustomPacketPayload {
        public static final Type<AbrirMenu> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "ritual_menu"));
        public static final StreamCodec<ByteBuf, AbrirMenu> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, AbrirMenu::base,
                ByteBufCodecs.BOOL, AbrirMenu::amuleto,
                Preso.CODEC.apply(ByteBufCodecs.list(16)), AbrirMenu::presos,
                AbrirMenu::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: quem foi escolhido para voltar. */
    public record Escolha(BlockPos base, UUID preso, boolean amuleto) implements CustomPacketPayload {
        public static final Type<Escolha> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "ritual_escolha"));
        public static final StreamCodec<ByteBuf, Escolha> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Escolha::base,
                UUIDUtil.STREAM_CODEC, Escolha::preso,
                ByteBufCodecs.BOOL, Escolha::amuleto,
                Escolha::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
