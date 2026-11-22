package br.csmp.core.mixin;

import br.csmp.core.vazio.AtmosferaVazio;
import br.csmp.core.vazio.Vazio;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Na dimensao Vazio, o ceu e as nuvens ficam vermelhos. O Create: Skys le a cor das nuvens e do ceu
 * destes mesmos metodos, entao as nuvens 3D dele tambem ficam vermelhas.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void csmp$nuvensVermelhas(float parcial, CallbackInfoReturnable<Vec3> retorno) {
        ClientLevel nivel = (ClientLevel) (Object) this;
        if (Vazio.eh(nivel)) {
            retorno.setReturnValue(AtmosferaVazio.corNuvem(nivel.getGameTime(), parcial));
        }
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void csmp$ceuVermelho(Vec3 camera, float parcial, CallbackInfoReturnable<Vec3> retorno) {
        ClientLevel nivel = (ClientLevel) (Object) this;
        if (Vazio.eh(nivel)) {
            retorno.setReturnValue(AtmosferaVazio.corCeu(nivel.getGameTime(), parcial));
        }
    }
}
