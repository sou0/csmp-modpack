package br.csmp.core.vazio;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Monta o Vazio chunk a chunk seguindo o {@link VazioMapa}: primeiro as ruas (asfalto rachado, faixa no meio, calcada,
 * bueiros e postes), depois as arvores (longe das ruas e dos lotes) e por fim as construcoes cujo centro cai neste
 * chunk. Assim nada nasce dentro de nada.
 */
public class VazioMundoFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceLocation ARVORE = ResourceLocation.fromNamespaceAndPath("csmp", "arvore_negra");
    /** Meia largura do asfalto e da calcada (em blocos, a partir do meio da rua). */
    private static final double ASFALTO = 1.6D;
    private static final double CALCADA = 2.6D;
    private static final double ESPACO_DOS_POSTES = 13.0D;

    public VazioMundoFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
        WorldGenLevel n = contexto.level();
        long semente = n.getSeed();
        ChunkPos chunk = new ChunkPos(contexto.origin());
        int x0 = chunk.getMinBlockX();
        int z0 = chunk.getMinBlockZ();
        List<VazioMapa.Rua> ruas = VazioMapa.ruas(semente, x0 - 40, z0 - 40, x0 + 56, z0 + 56);
        List<VazioMapa.Lote> perto = VazioMapa.lotes(semente, x0 - 40, z0 - 40, x0 + 56, z0 + 56);
        Random r = new Random(chunk.toLong() ^ semente);
        for (VazioMapa.Rua rua : ruas) {
            desenharRua(n, rua, x0, z0, new Random(rua.semente()));
        }
        plantarArvores(contexto, x0, z0, ruas, perto, contexto.random());
        for (VazioMapa.Lote lote : perto) {
            if (lote.x() >= x0 && lote.x() < x0 + 16 && lote.z() >= z0 && lote.z() < z0 + 16) {
                ConstrucoesVazio.construir(n, lote);
            }
        }
        return true;
    }

    private static boolean noChunk(int x, int z, int x0, int z0) {
        return x >= x0 && x < x0 + 16 && z >= z0 && z < z0 + 16;
    }

    /** Desenha so a parte da rua que cai neste chunk; o resto e desenhado pelos chunks vizinhos com o mesmo calculo. */
    private static void desenharRua(WorldGenLevel n, VazioMapa.Rua rua, int x0, int z0, Random r) {
        double comprimento = rua.comprimento();
        int passos = Math.max(8, (int) (comprimento * 3.0D));
        int y = VazioMapa.Y - 1;
        double proximoPoste = ESPACO_DOS_POSTES * 0.5D;
        boolean ladoDoPoste = r.nextBoolean();
        for (int i = 0; i <= passos; i++) {
            double fracao = i / (double) passos;
            double t = rua.t0() + (rua.t1() - rua.t0()) * fracao;
            double arco = comprimento * fracao;
            double[] p = rua.ponto(t);
            double[] nn = rua.normal(t);
            for (double w = -CALCADA; w <= CALCADA + 1.0E-6D; w += 0.5D) {
                int x = (int) Math.floor(p[0] + nn[0] * w);
                int z = (int) Math.floor(p[1] + nn[1] * w);
                if (!noChunk(x, z, x0, z0)) {
                    continue;
                }
                long k = VazioMapa.h(rua.semente(), x, z, 77);
                double sorte = VazioMapa.u(k);
                BlockState bloco;
                if (Math.abs(w) <= ASFALTO) {
                    boolean faixa = Math.abs(w) < 0.3D && Math.floorMod((int) (arco / 2.0D), 3) == 0;
                    if (faixa) {
                        bloco = sorte < 0.25D ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                    } else if (sorte < 0.10D) {
                        bloco = Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
                    } else if (sorte < 0.14D) {
                        bloco = br.csmp.core.CsmpRegistro.CHAO_VAZIO.get().defaultBlockState(); // o chao vazando pelo asfalto
                    } else {
                        bloco = sorte < 0.55D ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    }
                } else {
                    bloco = sorte < 0.15D ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                            : sorte < 0.25D ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                }
                BlockPos q = new BlockPos(x, y, z);
                n.setBlock(q, bloco, 2);
                // nada cresce em cima da rua
                for (int dy = 1; dy <= 3; dy++) {
                    if (!n.getBlockState(q.above(dy)).isAir()) {
                        n.setBlock(q.above(dy), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                // bueiro de vez em quando, no meio do asfalto
                if (Math.abs(w) < 0.3D && VazioMapa.u(k * 5 + 3) < 0.012D) {
                    n.setBlock(q.above(), Blocks.IRON_TRAPDOOR.defaultBlockState(), 2);
                }
            }
            if (arco >= proximoPoste) {
                proximoPoste += ESPACO_DOS_POSTES + r.nextInt(5);
                ladoDoPoste = !ladoDoPoste;
                double lado = ladoDoPoste ? 1.0D : -1.0D;
                int px = (int) Math.floor(p[0] + nn[0] * lado * (CALCADA + 0.6D));
                int pz = (int) Math.floor(p[1] + nn[1] * lado * (CALCADA + 0.6D));
                if (noChunk(px, pz, x0, z0)) {
                    Direction paraRua = cardinal(-nn[0] * lado, -nn[1] * lado);
                    ConstrucoesVazio.poste(n, new BlockPos(px, VazioMapa.Y, pz), paraRua, new Random(VazioMapa.h(rua.semente(), px, pz, 9)));
                }
            }
        }
    }

    private static Direction cardinal(double dx, double dz) {
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /** Arvores finas e altas, bem juntas, mas nunca em cima de rua ou dentro de um lote. */
    private static void plantarArvores(FeaturePlaceContext<NoneFeatureConfiguration> contexto, int x0, int z0,
                                       List<VazioMapa.Rua> ruas, List<VazioMapa.Lote> lotes, RandomSource r) {
        WorldGenLevel n = contexto.level();
        Registry<ConfiguredFeature<?, ?>> registro = n.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> arvore = registro.get(ARVORE);
        if (arvore == null) {
            return;
        }
        int quantidade = 7 + r.nextInt(5);
        for (int i = 0; i < quantidade; i++) {
            int x = x0 + r.nextInt(16);
            int z = z0 + r.nextInt(16);
            boolean livre = true;
            for (VazioMapa.Lote l : lotes) {
                if (Math.hypot(l.x() - x, l.z() - z) < l.raio() + 4) {
                    livre = false;
                    break;
                }
            }
            if (livre) {
                for (VazioMapa.Rua rua : ruas) {
                    if (rua.distancia(x, z) < CALCADA + 3.5D) {
                        livre = false;
                        break;
                    }
                }
            }
            if (livre) {
                arvore.place(n, contexto.chunkGenerator(), r, new BlockPos(x, VazioMapa.Y, z));
            }
        }
    }
}
