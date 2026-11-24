package br.csmp.core.mundo;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.santuario.EstatuaFeature;
import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Faz a Floresta das Betulas Indoceis, um chunk por vez. Roda em todo chunk do overworld e so age nos que ficam
 * dentro de alguma floresta (veja {@link Bosques}): pinta o bioma, planta betulas, poe os olhos nos troncos,
 * monta o cemiterio com a estatua no chunk do centro e esconde minidungeons com uma peca rachada.
 */
public class BosqueFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> BETULA =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("birch"));
    private static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> BETULA_ALTA =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("birch_tall"));

    public BosqueFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
        WorldGenLevel nivel = contexto.level();
        ChunkPos chunk = new ChunkPos(contexto.origin());
        int meioX = chunk.getMinBlockX() + 8;
        int meioZ = chunk.getMinBlockZ() + 8;
        int[] centro = Bosques.maisPerto(nivel.getSeed(), meioX, meioZ);
        if (Bosques.distanciaDaBorda(centro, meioX, meioZ) > 24.0D) {
            return false;
        }
        RandomSource aleatorio = contexto.random();
        pintarBioma(nivel, chunk, centro);
        plantarBetulas(contexto, chunk, centro, aleatorio);
        colocarOlhos(nivel, chunk, centro, aleatorio);
        boolean ehCentro = chunk.getMinBlockX() + 8 == centro[0] && chunk.getMinBlockZ() + 8 == centro[1];
        if (ehCentro) {
            int y = EstatuaFeature.encontrarChao(nivel, centro[0], centro[1]);
            EstatuaFeature.construir(nivel, new BlockPos(centro[0], y, centro[1]), false);
        } else if (Bosques.distanciaDaBorda(centro, meioX, meioZ) < -30.0D && Math.floorMod(chunk.toLong() * 0x9E3779B97F4A7C15L >>> 20, 6L) == 0L) {
            masmorra(nivel, meioX, meioZ);
        }
        return true;
    }

    /** Troca o bioma das colunas que ficam dentro da floresta (de baixo a cima do chunk). */
    private static void pintarBioma(WorldGenLevel nivel, ChunkPos chunk, int[] centro) {
        Registry<Biome> registro = nivel.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> bioma = registro.getHolderOrThrow(Bosques.BIOMA);
        ChunkAccess dados = nivel.getChunk(chunk.x, chunk.z);
        for (int s = 0; s < dados.getSectionsCount(); s++) {
            LevelChunkSection secao = dados.getSection(s);
            for (int qx = 0; qx < 4; qx++) {
                for (int qz = 0; qz < 4; qz++) {
                    int x = chunk.getMinBlockX() + qx * 4 + 2;
                    int z = chunk.getMinBlockZ() + qz * 4 + 2;
                    if (Bosques.distanciaDaBorda(centro, x, z) > 0.0D) {
                        continue;
                    }
                    for (int qy = 0; qy < 4; qy++) {
                        @SuppressWarnings("unchecked")
                        net.minecraft.world.level.chunk.PalettedContainer<Holder<Biome>> biomas =
                                (net.minecraft.world.level.chunk.PalettedContainer<Holder<Biome>>) secao.getBiomes();
                        biomas.set(qx, qy, qz, bioma);
                    }
                }
            }
        }
        dados.setUnsaved(true);
    }

    private static boolean dentro(int[] centro, int x, int z) {
        return Bosques.distanciaDaBorda(centro, x, z) <= 0.0D;
    }

    private void plantarBetulas(FeaturePlaceContext<NoneFeatureConfiguration> contexto, ChunkPos chunk, int[] centro, RandomSource r) {
        WorldGenLevel nivel = contexto.level();
        Registry<ConfiguredFeature<?, ?>> registro = nivel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        int tentativas = 14;
        for (int i = 0; i < tentativas; i++) {
            int x = chunk.getMinBlockX() + r.nextInt(16);
            int z = chunk.getMinBlockZ() + r.nextInt(16);
            if (!dentro(centro, x, z) || Math.hypot(x - centro[0], z - centro[1]) < 13.0D) {
                continue;
            }
            int y = nivel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!nivel.getBlockState(pos.below()).is(BlockTags.DIRT)) {
                continue;
            }
            ResourceKey<ConfiguredFeature<?, ?>> chave = r.nextInt(3) == 0 ? BETULA_ALTA : BETULA;
            registro.getHolder(chave).ifPresent(arvore ->
                    arvore.value().place(nivel, contexto.chunkGenerator(), r, pos));
        }
    }

    /** Troca alguns troncos de betula (de 1 a 4 blocos acima do chao) por troncos com olho. */
    private static void colocarOlhos(WorldGenLevel nivel, ChunkPos chunk, int[] centro, RandomSource r) {
        BlockState olho = CsmpRegistro.BETULA_OLHO.get().defaultBlockState();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getMinBlockX() + dx;
                int z = chunk.getMinBlockZ() + dz;
                if (!dentro(centro, x, z)) {
                    continue;
                }
                // o heightmap da geracao nao conta as arvores: a base do tronco fica no primeiro ar acima do chao
                int base = nivel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if (!nivel.getBlockState(new BlockPos(x, base, z)).is(Blocks.BIRCH_LOG) || r.nextInt(5) != 0) {
                    continue;
                }
                BlockPos pos = new BlockPos(x, base + 1 + r.nextInt(4), z);
                if (nivel.getBlockState(pos).is(Blocks.BIRCH_LOG)) {
                    nivel.setBlock(pos, olho, 2);
                    nivel.scheduleTick(pos, CsmpRegistro.BETULA_OLHO.get(), 20 + r.nextInt(40));
                }
            }
        }
    }

    /**
     * Sala escondida debaixo da floresta: 9x9 com paredes de ardosia, sculk no chao e um bau com uma peca rachada.
     * Em cima, na superficie, fica uma marca de sculk para quem souber procurar.
     */
    private static void masmorra(WorldGenLevel nivel, int cx, int cz) {
        int superficie = nivel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        int y = Math.max(nivel.getMinBuildHeight() + 12, Math.min(superficie - 14, 40));
        Random r = new Random(((long) cx << 32) ^ cz);
        BlockState parede = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = 0; dy <= 5; dy++) {
                    boolean casca = Math.abs(dx) == 5 || Math.abs(dz) == 5 || dy == 0 || dy == 5;
                    BlockPos pos = new BlockPos(cx + dx, y + dy, cz + dz);
                    nivel.setBlock(pos, casca ? (r.nextInt(5) == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : parede)
                            : Blocks.AIR.defaultBlockState(), 2);
                }
                if (Math.abs(dx) < 5 && Math.abs(dz) < 5 && r.nextInt(3) == 0) {
                    nivel.setBlock(new BlockPos(cx + dx, y + 1, cz + dz), Blocks.SCULK.defaultBlockState(), 2);
                }
            }
        }
        BlockPos bau = new BlockPos(cx, y + 1, cz);
        nivel.setBlock(bau, Blocks.CHEST.defaultBlockState(), 2);
        CompoundTag dados = new CompoundTag();
        dados.putString("id", "minecraft:chest");
        dados.putInt("x", bau.getX());
        dados.putInt("y", bau.getY());
        dados.putInt("z", bau.getZ());
        ListTag itens = new ListTag();
        CompoundTag peca = new CompoundTag();
        peca.putByte("Slot", (byte) 13);
        peca.putString("id", "csmp:peca_rachada");
        peca.putInt("count", 1);
        itens.add(peca);
        CompoundTag ouro = new CompoundTag();
        ouro.putByte("Slot", (byte) 11);
        ouro.putString("id", "minecraft:gold_ingot");
        ouro.putInt("count", 2 + r.nextInt(4));
        itens.add(ouro);
        dados.put("Items", itens);
        nivel.getChunk(bau).setBlockEntityNbt(dados);
        nivel.setBlock(new BlockPos(cx + 3, y + 1, cz + 3), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        nivel.setBlock(new BlockPos(cx - 3, y + 1, cz - 3), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        // marca na superficie
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos topo = new BlockPos(cx + dx, superficie - 1, cz + dz);
                if (nivel.getBlockState(topo).is(BlockTags.DIRT) || nivel.getBlockState(topo).is(Blocks.PODZOL)) {
                    nivel.setBlock(topo, Blocks.SCULK.defaultBlockState(), 2);
                }
            }
        }
    }
}
