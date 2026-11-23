package br.csmp.core.vazio;

import br.csmp.core.CsmpMod;
import br.csmp.core.CsmpRegistro;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Identificacao da dimensao Vazio (csmp:vazio). */
public final class Vazio {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "vazio");
    public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, ID);

    private Vazio() {}

    public static boolean eh(Level nivel) {
        return nivel != null && nivel.dimension().equals(DIMENSAO);
    }

    /**
     * Altura do chao de verdade (primeiro ar acima do chao_vazio), ignorando arvores, folhas e o que mais
     * estiver por cima. Sem isso, as estruturas nasceriam em cima da copa das arvores gigantes.
     */
    public static int alturaDoChao(WorldGenLevel nivel, int x, int z) {
        int topo = nivel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = topo; y > nivel.getMinBuildHeight() + 1; y--) {
            pos.set(x, y - 1, z);
            if (nivel.getBlockState(pos).is(CsmpRegistro.CHAO_VAZIO.get())) {
                return y;
            }
        }
        return topo;
    }
}
