package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Santuario: a mesma construcao nas duas dimensoes, so mudam os blocos (branco e frio no Vazio,
 * pedra e musgo no overworld). Em modo "regional" ele aparece de forma rara e na MESMA coordenada
 * (x, z) nas duas dimensoes, porque o lugar sai so da semente do mundo e da regiao. Em modo "direto"
 * (para testes) ele e colocado exatamente onde mandarem.
 */
public class SantuarioFeature extends Feature<SantuarioFeature.Config> {
    /** Tamanho de uma regiao, em chunks: cada regiao tem no maximo um santuario. */
    private static final int REGIAO = 12;
    /** Chance (em %) de uma regiao ter santuario. */
    private static final int CHANCE = 60;

    public record Config(String estilo, boolean regional) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
                Codec.STRING.fieldOf("estilo").forGetter(Config::estilo),
                Codec.BOOL.fieldOf("regional").forGetter(Config::regional)
        ).apply(instancia, Config::new));
    }

    /** Blocos de cada estilo. */
    private record Paleta(Block piso, Block dais, Block pilar, Block muro, Block luz, Block obelisco, Block fundacao) {}

    private static final Paleta VAZIO = new Paleta(Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR,
            Blocks.QUARTZ_SLAB, Blocks.SHROOMLIGHT, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK);
    private static final Paleta SUPERFICIE = new Paleta(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS,
            Blocks.STONE_BRICK_SLAB, Blocks.LANTERN, Blocks.STONE_BRICKS, Blocks.COBBLESTONE);

    public SantuarioFeature(Codec<Config> codec) {
        super(codec);
    }

    private static long mistura(long semente, int rx, int rz) {
        long h = semente ^ (rx * 0x9E3779B97F4A7C15L) ^ (rz * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> contexto) {
        WorldGenLevel nivel = contexto.level();
        BlockPos o = contexto.origin();
        Paleta paleta = "vazio".equals(contexto.config().estilo()) ? VAZIO : SUPERFICIE;
        BlockPos base;
        if (contexto.config().regional()) {
            int chunkX = o.getX() >> 4;
            int chunkZ = o.getZ() >> 4;
            int rx = Math.floorDiv(chunkX, REGIAO);
            int rz = Math.floorDiv(chunkZ, REGIAO);
            long h = mistura(nivel.getSeed(), rx, rz);
            int alvoX = rx * REGIAO + (int) Math.floorMod(h >>> 8, (long) REGIAO);
            int alvoZ = rz * REGIAO + (int) Math.floorMod(h >>> 24, (long) REGIAO);
            if (chunkX != alvoX || chunkZ != alvoZ || Math.floorMod(h >>> 44, 100L) >= CHANCE) {
                return false;
            }
            int x = chunkX * 16 + 8;
            int z = chunkZ * 16 + 8;
            base = new BlockPos(x, encontrarChao(nivel, x, z), z);
        } else {
            base = o;
        }
        construir(nivel, base, paleta);
        return true;
    }

    /** Primeiro ar acima do chao de verdade: ignora folhas, troncos e plantas, mas para na agua. */
    private static int encontrarChao(WorldGenLevel nivel, int x, int z) {
        int y = nivel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (y > nivel.getMinBuildHeight() + 2) {
            pos.set(x, y - 1, z);
            BlockState abaixo = nivel.getBlockState(pos);
            boolean atravessavel = abaixo.isAir() || abaixo.is(BlockTags.LEAVES) || abaixo.is(BlockTags.LOGS)
                    || abaixo.is(Blocks.VINE) || (abaixo.canBeReplaced() && abaixo.getFluidState().isEmpty());
            if (!atravessavel) {
                break;
            }
            y--;
        }
        return y;
    }

    private static void um(WorldGenLevel n, BlockPos base, int dx, int dy, int dz, Block bloco) {
        n.setBlock(base.offset(dx, dy, dz), bloco.defaultBlockState(), 2);
    }

    private static void construir(WorldGenLevel n, BlockPos base, Paleta p) {
        // limpa o espaco e firma o chao (no overworld o terreno nao e plano)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 0; dy <= 7; dy++) {
                    um(n, base, dx, dy, dz, Blocks.AIR);
                }
                for (int dy = -2; dy >= -14; dy--) {
                    BlockState abaixo = n.getBlockState(base.offset(dx, dy, dz));
                    boolean oco = abaixo.isAir() || abaixo.canBeReplaced() || !abaixo.getFluidState().isEmpty()
                            || abaixo.is(BlockTags.LEAVES) || abaixo.is(BlockTags.LOGS);
                    if (!oco) {
                        break;
                    }
                    um(n, base, dx, dy, dz, p.fundacao());
                }
                um(n, base, dx, -1, dz, p.piso());
            }
        }
        // dais 5x5 e o altar no meio
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                um(n, base, dx, 0, dz, p.dais());
            }
        }
        n.setBlock(base.offset(0, 1, 0), CsmpRegistro.ALTAR_SANTUARIO.get().defaultBlockState(), 2);
        // quatro pilares com luz no topo
        for (int sx = -4; sx <= 4; sx += 8) {
            for (int sz = -4; sz <= 4; sz += 8) {
                for (int dy = 0; dy <= 5; dy++) {
                    um(n, base, sx, dy, sz, p.pilar());
                }
                um(n, base, sx, 6, sz, p.luz());
            }
        }
        // murinhos baixos entre os pilares, com uma entrada no meio de cada lado
        for (int d = -3; d <= 3; d++) {
            if (Math.abs(d) <= 1) {
                continue;
            }
            um(n, base, d, 0, -4, p.muro());
            um(n, base, d, 0, 4, p.muro());
            um(n, base, -4, 0, d, p.muro());
            um(n, base, 4, 0, d, p.muro());
        }
        // obelisco alto atras, para o santuario poder ser visto de longe
        for (int dy = 0; dy <= 12; dy++) {
            um(n, base, 0, dy, -6, p.obelisco());
        }
        um(n, base, 0, 13, -6, p.luz());
    }
}
