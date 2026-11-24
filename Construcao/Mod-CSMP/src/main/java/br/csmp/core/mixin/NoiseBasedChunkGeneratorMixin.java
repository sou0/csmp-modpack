package br.csmp.core.mixin;

import br.csmp.core.mundo.BosquePintura;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Depois que o gerador escolhe os biomas de um chunk, aplica a Floresta das Betulas Indoceis (bioma real). */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

    @Inject(method = "createBiomes", at = @At("RETURN"), cancellable = true)
    private void csmp$florestaDasBetulas(RandomState estado, Blender mistura, StructureManager estruturas, ChunkAccess chunk,
                                        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> retorno) {
        NoiseBasedChunkGenerator gerador = (NoiseBasedChunkGenerator) (Object) this;
        retorno.setReturnValue(retorno.getReturnValue().thenApply(pronto -> {
            BosquePintura.pintar(gerador, pronto);
            return pronto;
        }));
    }
}
