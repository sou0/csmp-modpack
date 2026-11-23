package br.csmp.core.vazio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Estruturas soltas do Vazio, no clima de espaco liminar: um poste que ninguem acendeu, portas que
 * nao levam a lugar nenhum, uma casinha vazia, uma escada para o nada, uma sala branca com uma cadeira,
 * um monolito com olho, um balanco parado e uma poca de sangue. Cada uma e uma "feicao" (tipo = nome).
 */
public class EstruturaVazioFeature extends Feature<EstruturaVazioFeature.Config> {

    public record Config(String tipo) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
                Codec.STRING.fieldOf("tipo").forGetter(Config::tipo)
        ).apply(instancia, Config::new));
    }

    public EstruturaVazioFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> contexto) {
        WorldGenLevel nivel = contexto.level();
        BlockPos o = contexto.origin();
        // mantem a estrutura perto do meio do chunk (a geracao so deixa escrever perto)
        int x = ((o.getX() >> 4) << 4) + 5 + Math.floorMod(o.getX(), 6);
        int z = ((o.getZ() >> 4) << 4) + 5 + Math.floorMod(o.getZ(), 6);
        int y = Vazio.alturaDoChao(nivel, x, z);
        BlockPos base = new BlockPos(x, y, z);
        switch (contexto.config().tipo()) {
            case "poste" -> poste(nivel, base);
            case "portas" -> portas(nivel, base);
            case "casinha" -> casinha(nivel, base);
            case "escada" -> escada(nivel, base);
            case "sala" -> sala(nivel, base);
            case "monolito" -> monolito(nivel, base);
            case "balanco" -> balanco(nivel, base);
            case "fonte" -> fonte(nivel, base);
            default -> {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ ajudantes

    private static BlockState b(Block bloco) {
        return bloco.defaultBlockState();
    }

    private static void um(WorldGenLevel n, BlockPos base, int dx, int dy, int dz, BlockState estado) {
        n.setBlock(base.offset(dx, dy, dz), estado, 2);
    }

    private static void caixa(WorldGenLevel n, BlockPos base, int x1, int y1, int z1, int x2, int y2, int z2, BlockState estado) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    um(n, base, x, y, z, estado);
                }
            }
        }
    }

    private static void porta(WorldGenLevel n, BlockPos base, int dx, int dy, int dz, Direction direcao) {
        BlockState inferior = b(Blocks.IRON_DOOR).setValue(DoorBlock.FACING, direcao).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState superior = inferior.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        um(n, base, dx, dy, dz, inferior);
        um(n, base, dx, dy + 1, dz, superior);
    }

    private static BlockState escadaDe(Block bloco, Direction direcao) {
        return b(bloco).setValue(StairBlock.FACING, direcao);
    }

    // ------------------------------------------------------------------ estruturas

    /** Poste solitario: uma lanterna no alto de um poste de pedra escura. */
    private static void poste(WorldGenLevel n, BlockPos base) {
        caixa(n, base, 0, 0, 0, 0, 5, 0, b(Blocks.POLISHED_BLACKSTONE_WALL));
        um(n, base, 0, 6, 0, b(Blocks.POLISHED_BLACKSTONE));
        um(n, base, 0, 7, 0, b(Blocks.LANTERN));
    }

    /** Tres portas de ferro em pe, sozinhas, com um tapete vermelho na frente de cada uma. */
    private static void portas(WorldGenLevel n, BlockPos base) {
        for (int i = -1; i <= 1; i++) {
            int x = i * 4;
            caixa(n, base, x - 1, 0, 0, x - 1, 3, 0, b(Blocks.BLACK_CONCRETE));
            caixa(n, base, x + 1, 0, 0, x + 1, 3, 0, b(Blocks.BLACK_CONCRETE));
            caixa(n, base, x - 1, 3, 0, x + 1, 3, 0, b(Blocks.BLACK_CONCRETE));
            porta(n, base, x, 0, 0, Direction.SOUTH);
            um(n, base, x, 0, 1, b(Blocks.RED_CARPET));
        }
    }

    /** Casinha branca e vazia, com janela vermelha, tapete e uma cadeira de frente para a parede. */
    private static void casinha(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -3, 0, -3, 3, 3, 3, b(Blocks.WHITE_CONCRETE));
        caixa(n, base, -2, 0, -2, 2, 3, 2, b(Blocks.AIR));
        caixa(n, base, -3, 4, -3, 3, 4, 3, b(Blocks.WHITE_CONCRETE));
        caixa(n, base, 0, 0, 3, 0, 1, 3, b(Blocks.AIR)); // porta aberta
        um(n, base, 3, 2, 0, b(Blocks.RED_STAINED_GLASS_PANE));
        um(n, base, -3, 2, 0, b(Blocks.RED_STAINED_GLASS_PANE));
        caixa(n, base, -2, 0, -2, 2, 0, 2, b(Blocks.RED_CARPET));
        um(n, base, 0, 0, -2, escadaDe(Blocks.OAK_STAIRS, Direction.NORTH));
    }

    /** Escada que sobe ate uma moldura de porta no ar. Do outro lado, so o vazio. */
    private static void escada(WorldGenLevel n, BlockPos base) {
        for (int i = 0; i < 10; i++) {
            int x = -5 + i;
            um(n, base, x, i, 0, escadaDe(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST));
            if (i > 0) {
                caixa(n, base, x, 0, 0, x, i - 1, 0, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
            }
        }
        caixa(n, base, 5, 0, 0, 5, 9, 0, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
        caixa(n, base, 5, 10, -1, 5, 13, -1, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
        caixa(n, base, 5, 10, 1, 5, 13, 1, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
        caixa(n, base, 5, 13, -1, 5, 13, 1, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
    }

    /** Sala branca, fechada, com uma cadeira e uma mesinha no meio e luz fria no teto. */
    private static void sala(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -4, 0, -4, 4, 4, 4, b(Blocks.WHITE_CONCRETE));
        caixa(n, base, -3, 0, -3, 3, 4, 3, b(Blocks.AIR));
        caixa(n, base, -4, 5, -4, 4, 5, 4, b(Blocks.WHITE_CONCRETE));
        caixa(n, base, -3, -1, -3, 3, -1, 3, b(Blocks.WHITE_CONCRETE));
        porta(n, base, 0, 0, 4, Direction.SOUTH);
        um(n, base, 0, 0, 0, escadaDe(Blocks.OAK_STAIRS, Direction.NORTH));
        um(n, base, 2, 0, 0, b(Blocks.OAK_FENCE));
        um(n, base, 2, 1, 0, b(Blocks.OAK_PRESSURE_PLATE));
        for (int sx = -2; sx <= 2; sx += 4) {
            for (int sz = -2; sz <= 2; sz += 4) {
                um(n, base, sx, 4, sz, b(Blocks.SEA_LANTERN));
            }
        }
    }

    /** Monolito preto com um olho vermelho aceso. */
    private static void monolito(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -2, 0, -2, 2, 0, 2, b(Blocks.POLISHED_BLACKSTONE));
        caixa(n, base, -1, 1, 0, 1, 9, 0, b(Blocks.POLISHED_BLACKSTONE));
        um(n, base, 0, 6, -1, b(Blocks.SHROOMLIGHT));
        um(n, base, -1, 6, -1, b(Blocks.RED_CONCRETE));
        um(n, base, 1, 6, -1, b(Blocks.RED_CONCRETE));
        um(n, base, 0, 7, -1, b(Blocks.RED_CONCRETE));
        um(n, base, 0, 5, -1, b(Blocks.RED_CONCRETE));
    }

    /** Balanco de parquinho, parado, com as correntes esticadas. */
    private static void balanco(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -2, 0, 0, -2, 4, 0, b(Blocks.DARK_OAK_FENCE));
        caixa(n, base, 2, 0, 0, 2, 4, 0, b(Blocks.DARK_OAK_FENCE));
        caixa(n, base, -2, 4, 0, 2, 4, 0, b(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        caixa(n, base, -1, 1, 0, -1, 3, 0, b(Blocks.CHAIN));
        caixa(n, base, 1, 1, 0, 1, 3, 0, b(Blocks.CHAIN));
        caixa(n, base, -1, 0, 0, 1, 0, 0, b(Blocks.DARK_OAK_SLAB));
    }

    /** Fonte seca: um anel de pedra com uma poca vermelha no lugar da agua. */
    private static void fonte(WorldGenLevel n, BlockPos base) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double distancia = Math.sqrt(dx * dx + dz * dz);
                if (distancia <= 2.3D) {
                    um(n, base, dx, -1, dz, b(Blocks.NETHER_WART_BLOCK));
                } else if (distancia <= 3.4D) {
                    um(n, base, dx, 0, dz, b(Blocks.STONE_BRICKS));
                }
            }
        }
    }
}
