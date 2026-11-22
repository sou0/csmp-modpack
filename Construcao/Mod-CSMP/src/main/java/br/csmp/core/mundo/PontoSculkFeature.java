package br.csmp.core.mundo;

import br.csmp.core.CsmpMod;
import br.csmp.core.CsmpRegistro;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Ponto de sculk: coloca um pequeno ponto de sculk e deixa o entorno "morto":
 * as folhas somem, os troncos viram troncos negros e a grama vira terra seca.
 * O sculk esta num estado dormente, mas ja deixa resquicios.
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

    private static void secarEntorno(WorldGenLevel nivel, BlockPos origem, int raio, RandomSource aleatorio) {
        int raio2 = raio * raio;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
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
                    if (estado.is(BlockTags.LEAVES) || estado.is(Blocks.VINE)) {
                        nivel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    } else if (estado.is(BlockTags.LOGS)) {
                        BlockState negro = CsmpRegistro.TRONCO_NEGRO.get().defaultBlockState();
                        if (estado.hasProperty(RotatedPillarBlock.AXIS)) {
                            negro = negro.setValue(RotatedPillarBlock.AXIS, estado.getValue(RotatedPillarBlock.AXIS));
                        }
                        nivel.setBlock(pos, negro, 2);
                    } else if (estado.is(Blocks.GRASS_BLOCK)) {
                        nivel.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                    } else if (estado.is(Blocks.SHORT_GRASS) || estado.is(Blocks.TALL_GRASS)
                            || estado.is(Blocks.FERN) || estado.is(Blocks.LARGE_FERN)
                            || estado.is(BlockTags.FLOWERS)) {
                        // metade vira arbusto seco, metade some
                        BlockState novo = aleatorio.nextInt(3) == 0 ? Blocks.DEAD_BUSH.defaultBlockState()
                                : Blocks.AIR.defaultBlockState();
                        nivel.setBlock(pos, novo, 2);
                    }
                }
            }
        }
    }
}
