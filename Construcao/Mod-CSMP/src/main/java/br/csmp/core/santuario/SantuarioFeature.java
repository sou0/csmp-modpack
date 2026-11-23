package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Santuario japones (jinja): dois torii, lanternas de pedra, o salao principal com telhado de quatro aguas
 * (o altar fica no meio dele) e o santuario interno com o espelho, no fundo. E EXATAMENTE a mesma construcao
 * nas duas dimensoes, so mudam os blocos: no overworld e um santuario normal (torii vermelho, paredes brancas,
 * madeira, telhado de ardosia); no Vazio e todo preto. Como o ritual troca o terreno em volta, depois da troca
 * cada lado fica com a versao do outro e da para ver na hora que inverteu.
 *
 * Em modo "regional" ele aparece de forma rara e na MESMA coordenada (x, z) nas duas dimensoes, porque o
 * lugar sai so da semente do mundo e da regiao. Em modo "direto" (para testes) ele e colocado exatamente
 * onde mandarem.
 *
 * Eixos: o altar fica em (0, 1, 0). A entrada (torii) e para o lado +z, o santuario interno fica em -z.
 * Tudo cabe num disco de 12 blocos de raio em volta do altar (o ritual troca um raio maior que isso).
 */
public class SantuarioFeature extends Feature<SantuarioFeature.Config> {
    /** Tamanho de uma regiao, em chunks: cada regiao tem no maximo um santuario. */
    private static final int REGIAO = 12;
    /** Chance (em %) de uma regiao ter santuario. */
    private static final int CHANCE = 60;
    /** Raio do terreno que o santuario limpa e firma. */
    private static final int RAIO = 12;

    public record Config(String estilo, boolean regional) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
                Codec.STRING.fieldOf("estilo").forGetter(Config::estilo),
                Codec.BOOL.fieldOf("regional").forGetter(Config::regional)
        ).apply(instancia, Config::new));
    }

    /** O que cada papel da construcao usa em cada estilo. */
    private record Paleta(
            BlockState piso, BlockState caminho, BlockState cascalho, BlockState fundacao,
            BlockState vermelho, BlockState preto, BlockState parede, BlockState madeira,
            BlockState cerca, BlockState muro, BlockState cobertura, BlockState luz, BlockState espelho,
            Block degrau, Block telhado, Block telhadoEscada) {}

    private static final BlockState LAJE_PRETA = Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState();

    private static final Paleta VAZIO = new Paleta(
            Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.BLACK_CONCRETE_POWDER.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.BLACK_CONCRETE.defaultBlockState(),
            Blocks.COAL_BLOCK.defaultBlockState(),
            Blocks.BLACK_TERRACOTTA.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState(),
            Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true),
            Blocks.OBSIDIAN.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.BLACKSTONE, Blocks.BLACKSTONE_STAIRS);

    private static final Paleta SUPERFICIE = new Paleta(
            Blocks.STONE_BRICKS.defaultBlockState(),
            Blocks.POLISHED_ANDESITE.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.RED_CONCRETE.defaultBlockState(),
            Blocks.BLACK_CONCRETE.defaultBlockState(),
            Blocks.WHITE_CONCRETE.defaultBlockState(),
            Blocks.SPRUCE_PLANKS.defaultBlockState(),
            Blocks.SPRUCE_FENCE.defaultBlockState(),
            Blocks.STONE_BRICK_WALL.defaultBlockState(),
            Blocks.STONE_BRICK_SLAB.defaultBlockState(),
            Blocks.LANTERN.defaultBlockState(),
            Blocks.GOLD_BLOCK.defaultBlockState(),
            Blocks.SPRUCE_STAIRS, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILE_STAIRS);

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
            base = new BlockPos(x, "vazio".equals(contexto.config().estilo())
                    ? br.csmp.core.vazio.Vazio.alturaDoChao(nivel, x, z) : encontrarChao(nivel, x, z), z);
        } else {
            base = o;
        }
        new Construtor(nivel, base, paleta).construir();
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

    /** Monta o santuario. Guarda os blocos que dependem dos vizinhos (escadas, cercas, muros) para ajustar no fim. */
    private static final class Construtor {
        private final WorldGenLevel n;
        private final BlockPos base;
        private final Paleta p;
        private final List<BlockPos> formas = new ArrayList<>();

        Construtor(WorldGenLevel n, BlockPos base, Paleta p) {
            this.n = n;
            this.base = base;
            this.p = p;
        }

        private void um(int dx, int dy, int dz, BlockState estado) {
            this.n.setBlock(this.base.offset(dx, dy, dz), estado, 2);
        }

        /** Bloco que muda de forma conforme os vizinhos: guarda para ajustar depois. */
        private void forma(int dx, int dy, int dz, BlockState estado) {
            um(dx, dy, dz, estado);
            this.formas.add(this.base.offset(dx, dy, dz));
        }

        private static BlockState escada(Block bloco, Direction sobe) {
            return bloco.defaultBlockState().setValue(StairBlock.FACING, sobe).setValue(StairBlock.HALF, Half.BOTTOM);
        }

        private void construir() {
            limparEFirmar();
            piso();
            salao();
            santuarioInterno();
            torii(10);
            toro(3, 7);
            toro(-3, 7);
            toro(8, 0);
            toro(-8, 0);
            this.n.setBlock(this.base.offset(0, 1, 0), CsmpRegistro.ALTAR_SANTUARIO.get().defaultBlockState(), 2);
            ajustarFormas();
        }

        /** Limpa o espaco e firma o chao (no overworld o terreno nao e plano). */
        private void limparEFirmar() {
            for (int dx = -RAIO; dx <= RAIO; dx++) {
                for (int dz = -RAIO; dz <= RAIO; dz++) {
                    if (dx * dx + dz * dz > RAIO * RAIO) {
                        continue;
                    }
                    for (int dy = 0; dy <= 14; dy++) {
                        um(dx, dy, dz, Blocks.AIR.defaultBlockState());
                    }
                    for (int dy = -2; dy >= -14; dy--) {
                        BlockState abaixo = this.n.getBlockState(this.base.offset(dx, dy, dz));
                        boolean oco = abaixo.isAir() || abaixo.canBeReplaced() || !abaixo.getFluidState().isEmpty()
                                || abaixo.is(BlockTags.LEAVES) || abaixo.is(BlockTags.LOGS);
                        if (!oco) {
                            break;
                        }
                        um(dx, dy, dz, this.p.fundacao());
                    }
                }
            }
        }

        /** Patio: pedra em volta do salao, cascalho (jardim) mais longe e o caminho ate a entrada. */
        private void piso() {
            for (int dx = -RAIO; dx <= RAIO; dx++) {
                for (int dz = -RAIO; dz <= RAIO; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > RAIO * RAIO) {
                        continue;
                    }
                    BlockState bloco = d2 <= 72 ? this.p.piso() : this.p.cascalho();
                    if (Math.abs(dx) <= 1 && dz >= 5) {
                        bloco = this.p.caminho();
                    }
                    um(dx, -1, dz, bloco);
                }
            }
        }

        /** Salao principal (haiden): plataforma, pilares, parede do fundo, grade, escada e telhado. O altar fica no centro. */
        private void salao() {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    um(dx, 0, dz, this.p.madeira());
                }
            }
            // escada da frente
            for (int dx = -1; dx <= 1; dx++) {
                forma(dx, 0, 5, escada(this.p.degrau(), Direction.NORTH));
            }
            // pilares vermelhos
            int[][] pilares = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}, {-4, 0}, {4, 0}, {-2, 4}, {2, 4}};
            for (int[] c : pilares) {
                for (int dy = 1; dy <= 4; dy++) {
                    um(c[0], dy, c[1], this.p.vermelho());
                }
            }
            // parede do fundo, com a porta para o santuario interno
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = 1; dy <= 4; dy++) {
                    um(dx, dy, -4, this.p.parede());
                }
            }
            for (int dy = 1; dy <= 3; dy++) {
                um(0, dy, -4, Blocks.AIR.defaultBlockState());
            }
            // grades baixas dos lados e da frente (a entrada fica aberta)
            for (int dz = -3; dz <= 3; dz++) {
                if (dz == 0) {
                    continue;
                }
                forma(-4, 1, dz, this.p.cerca());
                forma(4, 1, dz, this.p.cerca());
            }
            forma(-3, 1, 4, this.p.cerca());
            forma(3, 1, 4, this.p.cerca());
            // viga em volta, no alto dos pilares
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 4) {
                        um(dx, 5, dz, this.p.preto());
                    }
                }
            }
            // luzes dentro do salao
            um(-2, 1, -2, this.p.luz());
            um(2, 1, -2, this.p.luz());
            um(-2, 1, 2, this.p.luz());
            um(2, 1, 2, this.p.luz());
            telhado(0, 0, 6, 6);
            // passagem coberta ate o santuario interno
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -6; dz <= -5; dz++) {
                    um(dx, 0, dz, this.p.madeira());
                }
            }
        }

        /** Santuario interno (honden), atras do salao, com o espelho sagrado. */
        private void santuarioInterno() {
            int cz = -9;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = cz - 2; dz <= cz + 2; dz++) {
                    um(dx, 0, dz, this.p.madeira());
                    boolean borda = Math.abs(dx) == 2 || Math.abs(dz - cz) == 2;
                    boolean canto = Math.abs(dx) == 2 && Math.abs(dz - cz) == 2;
                    for (int dy = 1; dy <= 3; dy++) {
                        if (canto) {
                            um(dx, dy, dz, this.p.vermelho());
                        } else if (borda) {
                            um(dx, dy, dz, this.p.parede());
                        }
                    }
                }
            }
            um(0, 1, cz + 2, Blocks.AIR.defaultBlockState());
            um(0, 2, cz + 2, Blocks.AIR.defaultBlockState());
            um(0, 1, cz - 1, this.p.espelho());
            um(-1, 1, cz - 1, this.p.luz());
            um(1, 1, cz - 1, this.p.luz());
            telhado(0, cz, 4, 3);
        }

        /**
         * Telhado de quatro aguas: cada camada e um anel de escadas (sobem para o centro) com o miolo preenchido,
         * e cada camada de cima e um bloco mais estreita. No fim, um pinaculo.
         */
        private void telhado(int cx, int cz, int dyInicial, int meia) {
            for (int k = 0; k < meia; k++) {
                int e = meia - k;
                int dy = dyInicial + k;
                for (int dx = -e; dx <= e; dx++) {
                    for (int dz = -e; dz <= e; dz++) {
                        int m = Math.max(Math.abs(dx), Math.abs(dz));
                        if (m == e) {
                            Direction sobe = Math.abs(dz) == e
                                    ? (dz < 0 ? Direction.SOUTH : Direction.NORTH)
                                    : (dx < 0 ? Direction.EAST : Direction.WEST);
                            forma(cx + dx, dy, cz + dz, escada(this.p.telhadoEscada(), sobe));
                        } else {
                            um(cx + dx, dy, cz + dz, this.p.telhado().defaultBlockState());
                        }
                    }
                }
            }
            forma(cx, dyInicial + meia, cz, this.p.muro());
        }

        /** Portao torii: dois pilares, duas vigas e o telhadinho curvo em cima. A passagem fica no meio. */
        private void torii(int z) {
            for (int s = -3; s <= 3; s += 6) {
                um(s, 0, z, this.p.preto());
                for (int dy = 1; dy <= 5; dy++) {
                    um(s, dy, z, this.p.vermelho());
                }
            }
            for (int dx = -2; dx <= 2; dx++) {
                um(dx, 3, z, this.p.vermelho());
                um(dx, 5, z, this.p.vermelho());
            }
            for (int dx = -4; dx <= 4; dx++) {
                um(dx, 6, z, this.p.preto());
            }
            for (int dx = -5; dx <= 5; dx++) {
                um(dx, 7, z, LAJE_PRETA);
            }
        }

        /** Lanterna de pedra (toro). */
        private void toro(int x, int z) {
            um(x, 0, z, this.p.piso());
            forma(x, 1, z, this.p.muro());
            um(x, 2, z, this.p.luz());
            um(x, 3, z, this.p.cobertura());
        }

        /** Escadas, cercas e muros se ligam aos vizinhos; recalcula depois que tudo esta no lugar. */
        private void ajustarFormas() {
            for (BlockPos pos : this.formas) {
                BlockState estado = this.n.getBlockState(pos);
                for (Direction d : Direction.values()) {
                    BlockPos vizinho = pos.relative(d);
                    estado = estado.updateShape(d, this.n.getBlockState(vizinho), this.n, pos, vizinho);
                }
                this.n.setBlock(pos, estado, 2);
            }
        }
    }
}
