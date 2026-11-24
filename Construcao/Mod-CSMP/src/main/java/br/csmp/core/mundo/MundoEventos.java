package br.csmp.core.mundo;

import br.csmp.core.CsmpMod;
import br.csmp.core.vazio.Vazio;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Regras do mundo sem animais perto do sculk. */
@EventBusSubscriber(modid = CsmpMod.ID)
public final class MundoEventos {
    private MundoEventos() {}

    /**
     * Animais passivos nao nascem (nem na geracao do mundo nem depois) num chunk que tem sculk na superficie:
     * o chunk inteiro do ponto de sculk fica sem animais.
     */
    @SubscribeEvent
    public static void semAnimaisPertoDoSculk(FinalizeSpawnEvent evento) {
        if (!(evento.getEntity() instanceof Animal)) {
            return;
        }
        MobSpawnType tipo = evento.getSpawnType();
        if (tipo != MobSpawnType.NATURAL && tipo != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        ServerLevelAccessor nivel = evento.getLevel();
        if (Vazio.eh(nivel.getLevel())) {
            return;
        }
        int minX = ((int) Math.floor(evento.getX()) >> 4) << 4;
        int minZ = ((int) Math.floor(evento.getZ()) >> 4) << 4;
        for (int dx = 1; dx < 16; dx += 3) {
            for (int dz = 1; dz < 16; dz += 3) {
                int x = minX + dx;
                int z = minZ + dz;
                int y = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (nivel.getBlockState(new BlockPos(x, y, z)).is(Blocks.SCULK)) {
                    evento.setSpawnCancelled(true);
                    return;
                }
            }
        }
    }
}
