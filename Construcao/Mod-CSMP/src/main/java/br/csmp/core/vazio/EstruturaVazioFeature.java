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
            case "cabana_torta" -> cabanaTorta(nivel, base);
            case "cabana_aberta" -> cabanaAberta(nivel, base);
            case "armadilha_poco" -> armadilhaPoco(nivel, base);
            case "armadilha_flechas" -> armadilhaFlechas(nivel, base);
            case "armadilha_teia" -> armadilhaTeia(nivel, base);
            case "armadilha_fio" -> armadilhaFio(nivel, base);
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
    static void poste(WorldGenLevel n, BlockPos base) {
        caixa(n, base, 0, 0, 0, 0, 5, 0, b(Blocks.POLISHED_BLACKSTONE_WALL));
        um(n, base, 0, 6, 0, b(Blocks.POLISHED_BLACKSTONE));
        um(n, base, 0, 7, 0, b(Blocks.LANTERN));
    }

    /** Tres portas de ferro em pe, sozinhas, com um tapete vermelho na frente de cada uma. */
    static void portas(WorldGenLevel n, BlockPos base) {
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
    static void casinha(WorldGenLevel n, BlockPos base) {
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
    static void escada(WorldGenLevel n, BlockPos base) {
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
    static void sala(WorldGenLevel n, BlockPos base) {
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
    static void monolito(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -2, 0, -2, 2, 0, 2, b(Blocks.POLISHED_BLACKSTONE));
        caixa(n, base, -1, 1, 0, 1, 9, 0, b(Blocks.POLISHED_BLACKSTONE));
        um(n, base, 0, 6, -1, b(Blocks.SHROOMLIGHT));
        um(n, base, -1, 6, -1, b(Blocks.RED_CONCRETE));
        um(n, base, 1, 6, -1, b(Blocks.RED_CONCRETE));
        um(n, base, 0, 7, -1, b(Blocks.RED_CONCRETE));
        um(n, base, 0, 5, -1, b(Blocks.RED_CONCRETE));
    }

    /** Balanco de parquinho, parado, com as correntes esticadas. */
    static void balanco(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -2, 0, 0, -2, 4, 0, b(Blocks.DARK_OAK_FENCE));
        caixa(n, base, 2, 0, 0, 2, 4, 0, b(Blocks.DARK_OAK_FENCE));
        caixa(n, base, -2, 4, 0, 2, 4, 0, b(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        caixa(n, base, -1, 1, 0, -1, 3, 0, b(Blocks.CHAIN));
        caixa(n, base, 1, 1, 0, 1, 3, 0, b(Blocks.CHAIN));
        caixa(n, base, -1, 0, 0, 1, 0, 0, b(Blocks.DARK_OAK_SLAB));
    }

    /** Cabana de madeira escura, torta: paredes que saem do prumo, cama, estante riscada e uma luz fraca. */
    private static void cabanaTorta(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -3, 0, -2, 3, 3, 2, b(Blocks.DARK_OAK_PLANKS));
        caixa(n, base, -2, 0, -1, 2, 2, 1, b(Blocks.AIR));
        // telhado inclinado para um lado so, como se tivesse cedido
        for (int x = -4; x <= 4; x++) {
            int altura = 4 + (x + 4) / 4;
            caixa(n, base, x, altura - 1, -3, x, altura - 1, 3, b(Blocks.DARK_OAK_SLAB));
        }
        caixa(n, base, -3, 3, -2, 3, 3, 2, b(Blocks.DARK_OAK_PLANKS));
        caixa(n, base, -2, 3, -1, 2, 3, 1, b(Blocks.AIR));
        // parede torta: um canto empurrado para fora
        caixa(n, base, 3, 0, 2, 4, 2, 2, b(Blocks.DARK_OAK_PLANKS));
        caixa(n, base, -3, 0, 2, -3, 2, 3, b(Blocks.DARK_OAK_LOG));
        // porta: vao aberto no meio, sem porta
        caixa(n, base, 0, 0, 2, 0, 1, 2, b(Blocks.AIR));
        um(n, base, -2, 0, -1, b(Blocks.RED_BED).setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.WEST));
        um(n, base, 2, 0, -1, b(Blocks.BOOKSHELF));
        um(n, base, 2, 1, -1, b(Blocks.BOOKSHELF));
        um(n, base, 2, 0, 1, b(Blocks.CHISELED_BOOKSHELF));
        um(n, base, 0, 2, 0, b(Blocks.CHAIN));
        um(n, base, 0, 1, 0, b(Blocks.SOUL_LANTERN));
        um(n, base, -3, 2, 0, b(Blocks.RED_STAINED_GLASS_PANE));
        caixa(n, base, -2, -1, -1, 2, -1, 1, b(Blocks.DARK_OAK_PLANKS));
    }

    /** Cabana de pedra com a porta aberta e uma luz vermelha lá dentro, como se alguem tivesse acabado de sair. */
    private static void cabanaAberta(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -3, 0, -3, 3, 4, 3, b(Blocks.DEEPSLATE_BRICKS));
        caixa(n, base, -2, 0, -2, 2, 3, 2, b(Blocks.AIR));
        caixa(n, base, -4, 5, -4, 4, 5, 4, b(Blocks.POLISHED_BLACKSTONE_SLAB));
        caixa(n, base, -3, 4, -3, 3, 4, 3, b(Blocks.POLISHED_BLACKSTONE));
        // porta de ferro escancarada (sem fechar), apontando para fora
        BlockState inferior = b(Blocks.IRON_DOOR).setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN, true);
        caixa(n, base, 0, 0, 3, 0, 1, 3, b(Blocks.AIR));
        um(n, base, 0, 0, 3, inferior);
        um(n, base, 0, 1, 3, inferior.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        um(n, base, 0, 0, 0, b(Blocks.SHROOMLIGHT));
        um(n, base, 0, 1, 0, b(Blocks.RED_STAINED_GLASS));
        um(n, base, -2, 0, -2, b(Blocks.CAULDRON));
        um(n, base, 2, 0, -2, escadaDe(Blocks.OAK_STAIRS, Direction.WEST));
        um(n, base, 3, 2, 0, b(Blocks.RED_STAINED_GLASS_PANE));
        caixa(n, base, -2, -1, -2, 2, -1, 2, b(Blocks.DEEPSLATE_TILES));
    }

    /**
     * Armadilha 1: poço escondido. Por cima fica o chão falso (igual ao chão do Vazio, mas sem colisão); embaixo,
     * 2 blocos de queda até pontas de pedra. O chão do Vazio só tem 4 blocos de altura, por isso o poço é raso e
     * o dano vem das pontas.
     */
    private static void armadilhaPoco(WorldGenLevel n, BlockPos base) {
        BlockState falso = br.csmp.core.CsmpRegistro.CHAO_FALSO.get().defaultBlockState();
        BlockState ponta = b(Blocks.POINTED_DRIPSTONE)
                .setValue(net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION, Direction.UP);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                um(n, base, dx, -2, dz, b(Blocks.AIR));
                um(n, base, dx, -3, dz, ponta);
                um(n, base, dx, -1, dz, falso);
            }
        }
        // um galho seco ao lado, como as raizes de sempre: nada denuncia o buraco
        um(n, base, 2, 0, 1, b(Blocks.DEAD_BUSH));
    }

    /**
     * Armadilha 4: fio quase invisível entre dois tocos escuros. Quem encosta fica cego e preso por alguns
     * segundos (o efeito é aplicado por {@link ArmadilhasVazio}).
     */
    private static void armadilhaFio(WorldGenLevel n, BlockPos base) {
        BlockState toco = br.csmp.core.CsmpRegistro.TRONCO_NEGRO.get().defaultBlockState();
        caixa(n, base, -3, 0, 0, -3, 1, 0, toco);
        caixa(n, base, 3, 0, 0, 3, 1, 0, toco);
        BlockState gancho = b(Blocks.TRIPWIRE_HOOK)
                .setValue(net.minecraft.world.level.block.TripWireHookBlock.ATTACHED, true);
        um(n, base, -2, 0, 0, gancho.setValue(net.minecraft.world.level.block.TripWireHookBlock.FACING, Direction.EAST));
        um(n, base, 2, 0, 0, gancho.setValue(net.minecraft.world.level.block.TripWireHookBlock.FACING, Direction.WEST));
        BlockState fio = b(Blocks.TRIPWIRE)
                .setValue(net.minecraft.world.level.block.TripWireBlock.ATTACHED, true)
                .setValue(net.minecraft.world.level.block.TripWireBlock.EAST, true)
                .setValue(net.minecraft.world.level.block.TripWireBlock.WEST, true);
        caixa(n, base, -1, 0, 0, 1, 0, 0, fio);
    }

    /**
     * Armadilha 2: uma placa de pressão no chão com dois dispensers escondidos em postes de pedra, apontados
     * para ela. Quem pisa leva flechas lentas.
     */
    private static void armadilhaFlechas(WorldGenLevel n, BlockPos base) {
        um(n, base, 0, 0, 0, b(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE));
        BlockPos[] posicoes = {base.offset(2, 0, 0), base.offset(-2, 0, 0)};
        Direction[] faces = {Direction.WEST, Direction.EAST};
        for (int i = 0; i < 2; i++) {
            BlockPos pos = posicoes[i];
            // poste de 2 blocos; o dispenser fica na altura da cintura, ao lado da placa (a 1 de distancia)
            BlockPos lado = pos.relative(faces[i]);
            n.setBlock(pos.below(), b(Blocks.POLISHED_BLACKSTONE), 2);
            n.setBlock(pos, b(Blocks.POLISHED_BLACKSTONE), 2);
            n.setBlock(lado, b(Blocks.DISPENSER).setValue(net.minecraft.world.level.block.DispenserBlock.FACING, faces[i]), 2);
            n.setBlock(pos.above(), b(Blocks.POLISHED_BLACKSTONE_WALL), 2);
            // durante a geracao o bloco ainda nao tem block entity; guardo o conteudo como NBT no chunk
            net.minecraft.nbt.CompoundTag dados = new net.minecraft.nbt.CompoundTag();
            dados.putString("id", "minecraft:dispenser");
            dados.putInt("x", lado.getX());
            dados.putInt("y", lado.getY());
            dados.putInt("z", lado.getZ());
            net.minecraft.nbt.ListTag itens = new net.minecraft.nbt.ListTag();
            net.minecraft.nbt.CompoundTag flecha = new net.minecraft.nbt.CompoundTag();
            flecha.putByte("Slot", (byte) 0);
            flecha.putString("id", "minecraft:arrow");
            flecha.putInt("count", 64);
            itens.add(flecha);
            dados.put("Items", itens);
            n.getChunk(lado).setBlockEntityNbt(dados);
        }
    }

    /** Armadilha 3: dois postes e uma cortina de teias entre eles; quem passa fica preso e devagar. */
    private static void armadilhaTeia(WorldGenLevel n, BlockPos base) {
        caixa(n, base, -3, 0, 0, -3, 3, 0, b(Blocks.POLISHED_BLACKSTONE_WALL));
        caixa(n, base, 3, 0, 0, 3, 3, 0, b(Blocks.POLISHED_BLACKSTONE_WALL));
        caixa(n, base, -2, 0, 0, 2, 2, 1, b(Blocks.COBWEB));
    }

    /** Fonte seca: um anel de pedra com uma poca vermelha no lugar da agua. */
    static void fonte(WorldGenLevel n, BlockPos base) {
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
