package br.csmp.core.mundo;

import br.csmp.core.CsmpMod;
import br.csmp.core.CsmpRegistro;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Ponto de sculk: coloca um pequeno ponto de sculk e deixa o entorno "morto":
 * as arvores viram arvores mortas (troncos negros, galhos tortos e uma copa rala de folhas negras)
 * e a grama vira terra seca. O sculk esta num estado dormente, mas ja deixa resquicios.
 */
public class PontoSculkFeature extends Feature<PontoSculkFeature.Config> {

    public record Config(int raio) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
                Codec.intRange(2, 12).fieldOf("raio").forGetter(Config::raio)
        ).apply(instancia, Config::new));
    }

    public PontoSculkFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> contexto) {
        WorldGenLevel nivel = contexto.level();
        BlockPos origem = contexto.origin();
        RandomSource aleatorio = contexto.random();

        var configurados = nivel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var sculk = configurados.get(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "sculk_superficie"));
        if (sculk == null || !sculk.place(nivel, contexto.chunkGenerator(), aleatorio, origem)) {
            return false;
        }
        secarEntorno(nivel, origem, contexto.config().raio(), aleatorio);
        return true;
    }

    private static boolean podeMexer(WorldGenLevel nivel, BlockPos pos) {
        return nivel.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && nivel.ensureCanWrite(pos);
    }

    private static BlockState troncoNegro(Direction.Axis eixo) {
        return CsmpRegistro.TRONCO_NEGRO.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, eixo);
    }

    private static BlockState folhaNegra() {
        return CsmpRegistro.FOLHAS_NEGRAS.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void secarEntorno(WorldGenLevel nivel, BlockPos origem, int raio, RandomSource aleatorio) {
        int raio2 = raio * raio;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<BlockPos> troncos = new ArrayList<>();
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dz = -raio; dz <= raio; dz++) {
                if (dx * dx + dz * dz > raio2) {
                    continue;
                }
                int x = origem.getX() + dx;
                int z = origem.getZ() + dz;
                if (!nivel.hasChunk(x >> 4, z >> 4)) {
                    continue; // fora da area que a geracao deixa mexer
                }
                for (int y = origem.getY() - 3; y <= origem.getY() + 26; y++) {
                    pos.set(x, y, z);
                    if (!nivel.ensureCanWrite(pos)) {
                        continue;
                    }
                    BlockState estado = nivel.getBlockState(pos);
                    if (estado.is(BlockTags.LEAVES)) {
                        // copa rala: 1 em cada 4 folhas vira folha negra, o resto some
                        nivel.setBlock(pos, aleatorio.nextInt(4) == 0 ? folhaNegra() : Blocks.AIR.defaultBlockState(), 2);
                    } else if (estado.is(Blocks.VINE)) {
                        nivel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    } else if (estado.is(BlockTags.LOGS)) {
                        Direction.Axis eixo = estado.hasProperty(RotatedPillarBlock.AXIS)
                                ? estado.getValue(RotatedPillarBlock.AXIS) : Direction.Axis.Y;
                        nivel.setBlock(pos, troncoNegro(eixo), 2);
                        troncos.add(pos.immutable());
                    } else if (estado.is(Blocks.GRASS_BLOCK)) {
                        nivel.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                    } else if (estado.is(Blocks.SHORT_GRASS) || estado.is(Blocks.TALL_GRASS)
                            || estado.is(Blocks.FERN) || estado.is(Blocks.LARGE_FERN)
                            || estado.is(BlockTags.FLOWERS)) {
                        // um terco vira arbusto seco, o resto some
                        BlockState novo = aleatorio.nextInt(3) == 0 ? Blocks.DEAD_BUSH.defaultBlockState()
                                : Blocks.AIR.defaultBlockState();
                        nivel.setBlock(pos, novo, 2);
                    }
                }
            }
        }
        for (BlockPos tronco : troncos) {
            ramificar(nivel, tronco, aleatorio);
        }
    }

    /** Da forma de arvore morta: o topo do tronco ganha galhos tortos e alguns troncos ganham um toco lateral. */
    private static void ramificar(WorldGenLevel nivel, BlockPos tronco, RandomSource aleatorio) {
        boolean topo = !nivel.getBlockState(tronco.above()).is(CsmpRegistro.TRONCO_NEGRO.get());
        int galhos = topo ? 2 + aleatorio.nextInt(3) : (aleatorio.nextInt(12) == 0 ? 1 : 0);
        for (int i = 0; i < galhos; i++) {
            Direction direcao = Direction.Plane.HORIZONTAL.getRandomDirection(aleatorio);
            int comprimento = 1 + aleatorio.nextInt(topo ? 3 : 2);
            BlockPos atual = tronco;
            for (int passo = 1; passo <= comprimento; passo++) {
                atual = atual.relative(direcao);
                if (passo == 2 && aleatorio.nextBoolean()) {
                    atual = atual.above(); // o galho entorta para cima
                }
                if (!podeMexer(nivel, atual) || !nivel.getBlockState(atual).isAir()) {
                    break;
                }
                nivel.setBlock(atual, troncoNegro(direcao.getAxis()), 2);
                if (passo == comprimento && aleatorio.nextBoolean()) {
                    BlockPos ponta = atual.above();
                    if (podeMexer(nivel, ponta) && nivel.getBlockState(ponta).isAir()) {
                        nivel.setBlock(ponta, folhaNegra(), 2);
                    }
                }
            }
        }
    }
}
