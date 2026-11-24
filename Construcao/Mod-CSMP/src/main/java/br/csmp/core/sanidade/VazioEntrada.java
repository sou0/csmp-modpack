package br.csmp.core.sanidade;

import br.csmp.core.vazio.Vazio;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/** Onde quem cai no Vazio aparece: na mesma coordenada (x, z) em que morreu, sobre o chao do Vazio. */
final class VazioEntrada {
    private VazioEntrada() {}

    static BlockPos pontoDeEntrada(ServerLevel vazio, ServerPlayer jogador) {
        int x = jogador.blockPosition().getX();
        int z = jogador.blockPosition().getZ();
        vazio.getChunk(x >> 4, z >> 4);
        int y = vazio.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(y, 4), z);
    }
}
