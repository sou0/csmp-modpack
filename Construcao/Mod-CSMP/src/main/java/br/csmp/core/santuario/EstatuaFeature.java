package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.vazio.Vazio;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Direction;
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
 * Cemiterio com a base da estatua no centro de um circulo de ritual. A mesma construcao nas duas dimensoes:
 * no Vazio e toda escura e a estatua ja esta inteira e brilhando; no overworld e de pedra comum, com
 * escombros, e a estatua esta quebrada (precisa ser montada).
 * Em modo regional (so Vazio) aparece de forma espalhada, uma por celula de 8x8 chunks com 40% de chance.
 */
public class EstatuaFeature extends Feature<EstatuaFeature.Config> {
    private static final int CELULA = 8;
    private static final int CHANCE = 40;

    public record Config(String estilo, boolean regional) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
                Codec.STRING.fieldOf("estilo").forGetter(Config::estilo),
                Codec.BOOL.fieldOf("regional").forGetter(Config::regional)
        ).apply(instancia, Config::new));
    }

    public EstatuaFeature(Codec<Config> codec) {
        super(codec);
    }

    private static long mistura(long semente, int x, int z) {
        long h = semente ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
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
        boolean vazio = "vazio".equals(contexto.config().estilo());
        int x;
        int z;
        if (contexto.config().regional()) {
            int chunkX = o.getX() >> 4;
            int chunkZ = o.getZ() >> 4;
            int rx = Math.floorDiv(chunkX, CELULA);
            int rz = Math.floorDiv(chunkZ, CELULA);
            long h = mistura(nivel.getSeed(), rx, rz);
            int alvoX = rx * CELULA + (int) Math.floorMod(h >>> 8, (long) CELULA);
            int alvoZ = rz * CELULA + (int) Math.floorMod(h >>> 24, (long) CELULA);
            if (chunkX != alvoX || chunkZ != alvoZ || Math.floorMod(h >>> 44, 100L) >= CHANCE) {
                return false;
            }
            x = chunkX * 16 + 8;
            z = chunkZ * 16 + 8;
        } else {
            x = o.getX();
            z = o.getZ();
        }
        int y = vazio ? Vazio.alturaDoChao(nivel, x, z) : encontrarChao(nivel, x, z);
        construir(nivel, new BlockPos(x, y, z), vazio);
        return true;
    }

    /** Primeiro ar acima do chao de verdade: ignora folhas, troncos e plantas, mas para na agua. */
    public static int encontrarChao(WorldGenLevel nivel, int x, int z) {
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

    private static BlockState b(Block bloco) {
        return bloco.defaultBlockState();
    }

    /** Paleta de uma das duas versoes do cemiterio. */
    private record Paleta(BlockState piso, BlockState pisoVariante, BlockState anel, BlockState raio,
                          BlockState caminho, BlockState terra, BlockState terraVariante, BlockState lapide,
                          BlockState lapideTopo, BlockState muro, BlockState luz, BlockState vela, BlockState flor,
                          BlockState fundacao) {}

    private static final Paleta VAZIO = new Paleta(
            b(Blocks.POLISHED_BLACKSTONE), b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS),
            b(Blocks.CHISELED_POLISHED_BLACKSTONE), b(Blocks.POLISHED_BLACKSTONE_BRICKS),
            b(Blocks.BLACKSTONE), b(Blocks.COARSE_DIRT), b(Blocks.ROOTED_DIRT),
            b(Blocks.POLISHED_BLACKSTONE_BRICK_WALL), b(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB),
            b(Blocks.POLISHED_BLACKSTONE_WALL), b(Blocks.SOUL_LANTERN),
            b(Blocks.BLACK_CANDLE).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true)
                    .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3),
            b(Blocks.WITHER_ROSE), b(Blocks.BLACKSTONE));

    private static final Paleta SUPERFICIE = new Paleta(
            b(Blocks.STONE_BRICKS), b(Blocks.MOSSY_STONE_BRICKS),
            b(Blocks.CHISELED_STONE_BRICKS), b(Blocks.CRACKED_STONE_BRICKS),
            b(Blocks.GRAVEL), b(Blocks.COARSE_DIRT), b(Blocks.PODZOL),
            b(Blocks.COBBLESTONE_WALL), b(Blocks.STONE_BRICK_SLAB),
            b(Blocks.MOSSY_COBBLESTONE_WALL), b(Blocks.LANTERN),
            b(Blocks.CANDLE).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 2),
            b(Blocks.DEAD_BUSH), b(Blocks.DIRT));

    private static final int RAIO_LIMPO = 13;
    private static final int RAIO_MURO = 11;

    /**
     * Monta o cemiterio. y = primeiro ar acima do chao; a base fica no nivel do chao (y - 1) e a estatua em pe
     * sobre ela. Partes: limpeza e fundacao (o terreno nunca fica flutuando), circulo de ritual com quatro raios
     * diagonais e velas, quatro caminhos, tumulos em fileiras, muro baixo com portoes e lanternas. Muros e lapides
     * sao ajustados no fim para se ligarem aos vizinhos.
     */
    public static void construir(WorldGenLevel n, BlockPos centro, boolean vazio) {
        Random r = new Random(centro.asLong());
        Paleta p = vazio ? VAZIO : SUPERFICIE;
        int y = centro.getY();
        List<BlockPos> ajustar = new ArrayList<>();

        // 1) limpeza (arvores, mato) e fundacao firme ate encontrar chao solido
        for (int dx = -RAIO_LIMPO; dx <= RAIO_LIMPO; dx++) {
            for (int dz = -RAIO_LIMPO; dz <= RAIO_LIMPO; dz++) {
                if (dx * dx + dz * dz > RAIO_LIMPO * RAIO_LIMPO) {
                    continue;
                }
                for (int dy = 0; dy <= 22; dy++) {
                    BlockPos q = centro.offset(dx, dy, dz);
                    if (!n.getBlockState(q).isAir()) {
                        n.setBlock(q, b(Blocks.AIR), 2);
                    }
                }
                for (int dy = -2; dy >= -8; dy--) {
                    BlockPos q = centro.offset(dx, dy, dz);
                    if (q.getY() <= n.getMinBuildHeight()) {
                        break;
                    }
                    BlockState abaixo = n.getBlockState(q);
                    if (abaixo.isSolidRender(n, q) && abaixo.getFluidState().isEmpty()) {
                        break;
                    }
                    n.setBlock(q, p.fundacao(), 2);
                }
            }
        }

        // 2) chao: circulo de ritual, caminhos e terra do cemiterio
        for (int dx = -RAIO_LIMPO; dx <= RAIO_LIMPO; dx++) {
            for (int dz = -RAIO_LIMPO; dz <= RAIO_LIMPO; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RAIO_LIMPO) {
                    continue;
                }
                BlockPos q = centro.offset(dx, -1, dz);
                boolean diagonal = Math.abs(dx) == Math.abs(dz) && d > 1.5D && d < 5.0D;
                boolean caminho = (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) && d > 6.5D;
                BlockState estado;
                if (d <= 1.5D) {
                    estado = p.piso();
                } else if (diagonal) {
                    estado = p.raio();
                } else if (d <= 3.6D) {
                    estado = r.nextInt(6) == 0 ? p.pisoVariante() : p.piso();
                } else if (d <= 4.6D) {
                    estado = p.raio();
                } else if (d <= 5.4D) {
                    estado = r.nextInt(5) == 0 ? p.pisoVariante() : p.piso();
                } else if (d <= 6.5D) {
                    estado = p.anel();
                } else if (caminho) {
                    estado = r.nextInt(7) == 0 ? p.terra() : p.caminho();
                } else {
                    estado = r.nextInt(4) == 0 ? p.terraVariante() : p.terra();
                }
                n.setBlock(q, estado, 2);
            }
        }
        n.setBlock(centro.below(), vazio ? CsmpRegistro.SIGILO.get().defaultBlockState()
                : CsmpRegistro.BASE_ESTATUA.get().defaultBlockState(), 2);

        // 3) velas na ponta dos quatro raios diagonais
        for (int[] c : new int[][] {{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
            n.setBlock(centro.offset(c[0], 0, c[1]), p.vela(), 2);
        }

        // 4) tumulos em fileiras, fora do circulo e fora dos caminhos
        for (int gx = -10; gx <= 10; gx += 3) {
            for (int gz = -10; gz <= 10; gz += 4) {
                int tx = gx + r.nextInt(2);
                int tz = gz;
                double d = Math.sqrt(tx * tx + tz * tz);
                if (d < 7.5D || d > RAIO_MURO - 2.0D || Math.abs(tx) <= 2 || Math.abs(tz) <= 2 || r.nextInt(5) == 0) {
                    continue;
                }
                tumulo(n, centro.offset(tx, 0, tz), p, r, ajustar);
            }
        }

        // 5) muro baixo em volta, com portoes nos quatro caminhos e lanternas nos pilares do portao
        for (int i = 0; i < 360; i += 2) {
            double ang = Math.toRadians(i);
            int mx = (int) Math.round(Math.cos(ang) * RAIO_MURO);
            int mz = (int) Math.round(Math.sin(ang) * RAIO_MURO);
            if (Math.abs(mx) <= 1 || Math.abs(mz) <= 1) {
                continue;
            }
            if (!vazio && r.nextInt(9) == 0) {
                continue; // no overworld o muro esta caindo aos pedacos
            }
            BlockPos q = centro.offset(mx, 0, mz);
            n.setBlock(q, p.muro(), 2);
            ajustar.add(q);
        }
        for (int[] c : new int[][] {{RAIO_MURO, 2}, {RAIO_MURO, -2}, {-RAIO_MURO, 2}, {-RAIO_MURO, -2},
                {2, RAIO_MURO}, {-2, RAIO_MURO}, {2, -RAIO_MURO}, {-2, -RAIO_MURO}}) {
            BlockPos pilar = centro.offset(c[0], 0, c[1]);
            n.setBlock(pilar, p.lapide(), 2);
            n.setBlock(pilar.above(), p.lapide(), 2);
            n.setBlock(pilar.above(2), p.luz(), 2);
            ajustar.add(pilar);
            ajustar.add(pilar.above());
        }

        // 6) abandono: no overworld, escombros da estatua quebrada; no Vazio, tudo intacto
        if (!vazio) {
            for (int i = 0; i < 6; i++) {
                int dx = r.nextInt(5) - 2;
                int dz = r.nextInt(5) - 2;
                if (Math.abs(dx) + Math.abs(dz) < 2 || Math.abs(dx) == Math.abs(dz)) {
                    continue;
                }
                n.setBlock(centro.offset(dx, 0, dz), r.nextBoolean() ? b(Blocks.COBBLESTONE_SLAB) : b(Blocks.MOSSY_COBBLESTONE_SLAB), 2);
            }
        }

        for (BlockPos q : ajustar) {
            BlockState estado = n.getBlockState(q);
            for (Direction dir : Direction.values()) {
                BlockPos vizinho = q.relative(dir);
                estado = estado.updateShape(dir, n.getBlockState(vizinho), n, q, vizinho);
            }
            n.setBlock(q, estado, 2);
        }

        // 7) a estatua em si (entidade): no Vazio inteira e brilhando; no overworld quebrada
        EstatuaEntity estatua = vazio ? null : CsmpRegistro.ESTATUA.get().create(n.getLevel());
        if (estatua != null) {
            estatua.moveTo(centro.getX() + 0.5D, y, centro.getZ() + 0.5D, 180.0F, 0.0F);
            estatua.setYBodyRot(180.0F);
            estatua.setYHeadRot(180.0F);
            if (vazio) {
                estatua.definirPartes(EstatuaEntity.TUDO);
                estatua.definirBrilho(true);
            }
            n.addFreshEntity(estatua);
        }
    }

    /** Um tumulo: lapide de 1 ou 2 blocos com tampa, monte de terra na frente e, as vezes, uma flor seca. */
    private static void tumulo(WorldGenLevel n, BlockPos pe, Paleta p, Random r, List<BlockPos> ajustar) {
        int altura = r.nextInt(3) == 0 ? 2 : 1;
        for (int h = 0; h < altura; h++) {
            n.setBlock(pe.above(h), p.lapide(), 2);
            ajustar.add(pe.above(h));
        }
        n.setBlock(pe.above(altura), p.lapideTopo(), 2);
        n.setBlock(pe.offset(0, -1, 1), p.terraVariante(), 2);
        n.setBlock(pe.offset(0, -1, 2), p.terraVariante(), 2);
        if (r.nextInt(3) == 0) {
            n.setBlock(pe.offset(0, 0, 1), p.flor(), 2);
        }
    }
}
