package br.csmp.core.mixin;

import br.csmp.core.mundo.Bosques;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O gerador so decora (arvores, minerios, feicoes) os biomas que a fonte de biomas diz que podem existir. A Floresta das
 * Betulas Indoceis entra depois da escolha normal de biomas (ver BosquePintura), entao ela e acrescentada aqui a lista
 * do overworld; sem isso, os chunks dela nasceriam sem nenhuma decoracao.
 */
@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin {
    private static final Map<BiomeSource, Set<Holder<Biome>>> CSMP$COM_BOSQUE = new WeakHashMap<>();

    @Inject(method = "possibleBiomes", at = @At("RETURN"), cancellable = true)
    private void csmp$incluirBosque(CallbackInfoReturnable<Set<Holder<Biome>>> retorno) {
        BiomeSource fonte = (BiomeSource) (Object) this;
        Set<Holder<Biome>> original = retorno.getReturnValue();
        synchronized (CSMP$COM_BOSQUE) {
            Set<Holder<Biome>> pronto = CSMP$COM_BOSQUE.get(fonte);
            if (pronto != null) {
                retorno.setReturnValue(pronto);
                return;
            }
        }
        if (original.stream().noneMatch(bioma -> bioma.is(Biomes.PLAINS))) {
            return; // nao e o overworld
        }
        MinecraftServer servidor = ServerLifecycleHooks.getCurrentServer();
        if (servidor == null) {
            return;
        }
        var registro = servidor.registryAccess().registry(Registries.BIOME);
        if (registro.isEmpty()) {
            return;
        }
        var nosso = registro.get().getHolder(Bosques.BIOMA);
        if (nosso.isEmpty()) {
            return;
        }
        Set<Holder<Biome>> comBosque = new HashSet<>(original);
        comBosque.add(nosso.get());
        Set<Holder<Biome>> imutavel = Set.copyOf(comBosque);
        synchronized (CSMP$COM_BOSQUE) {
            CSMP$COM_BOSQUE.put(fonte, imutavel);
        }
        retorno.setReturnValue(imutavel);
    }
}
