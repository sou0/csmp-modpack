package br.csmp.core.mundo;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Faz da Floresta das Betulas Indoceis um bioma de verdade: logo depois de o gerador escolher os biomas de um chunk
 * (antes do terreno, da superficie e da decoracao), as colunas dentro de uma floresta passam a ser do nosso bioma.
 * Assim o chunk ja nasce com as arvores, a grama, os sons e as cores dele, e nao com as do bioma original.
 * Chamado pelo mixin do gerador de ruido.
 */
public final class BosquePintura {
    private BosquePintura() {}

    public static void pintar(ChunkGenerator gerador, ChunkAccess chunk) {
        MinecraftServer servidor = ServerLifecycleHooks.getCurrentServer();
        if (servidor == null) {
            return;
        }
        ServerLevel overworld = servidor.overworld();
        if (overworld == null || overworld.getChunkSource().getGenerator() != gerador) {
            return; // so o overworld tem florestas
        }
        ChunkPos pos = chunk.getPos();
        int meioX = pos.getMinBlockX() + 8;
        int meioZ = pos.getMinBlockZ() + 8;
        int[] centro = Bosques.maisPerto(overworld, meioX, meioZ);
        if (centro == null || Bosques.distanciaDaBorda(centro, meioX, meioZ) > 24.0D) {
            return;
        }
        Holder<Biome> bioma = overworld.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Bosques.BIOMA);
        for (int s = 0; s < chunk.getSectionsCount(); s++) {
            LevelChunkSection secao = chunk.getSection(s);
            @SuppressWarnings("unchecked")
            PalettedContainer<Holder<Biome>> biomas = (PalettedContainer<Holder<Biome>>) secao.getBiomes();
            for (int qx = 0; qx < 4; qx++) {
                for (int qz = 0; qz < 4; qz++) {
                    int x = pos.getMinBlockX() + qx * 4 + 2;
                    int z = pos.getMinBlockZ() + qz * 4 + 2;
                    if (Bosques.distanciaDaBorda(centro, x, z) > 0.0D) {
                        continue;
                    }
                    for (int qy = 0; qy < 4; qy++) {
                        biomas.set(qx, qy, qz, bioma);
                    }
                }
            }
        }
    }
}
