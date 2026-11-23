package br.csmp.core.mixin;

import br.csmp.core.vazio.AtmosferaVazio;
import br.csmp.core.vazio.Vazio;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
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

    /**
     * Luz "de baixo": no Vazio as faces de baixo dos blocos sao as mais claras e as de cima as mais escuras
     * (o contrario do normal). Assim da para enxergar, a luz parece vir do chao, mas o chao em si fica escuro.
     */
    @Inject(method = "getShade", at = @At("HEAD"), cancellable = true)
    private void csmp$luzDeBaixo(Direction direcao, boolean sombreado, CallbackInfoReturnable<Float> retorno) {
        ClientLevel nivel = (ClientLevel) (Object) this;
        if (!sombreado || !Vazio.eh(nivel)) {
            return;
        }
        retorno.setReturnValue(switch (direcao) {
            case DOWN -> 1.0F;
            case UP -> 0.45F;
            case NORTH, SOUTH -> 0.75F;
            case WEST, EAST -> 0.65F;
        });
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void csmp$ceuVermelho(Vec3 camera, float parcial, CallbackInfoReturnable<Vec3> retorno) {
        ClientLevel nivel = (ClientLevel) (Object) this;
        if (Vazio.eh(nivel)) {
            retorno.setReturnValue(AtmosferaVazio.corCeu(nivel.getGameTime(), parcial));
        }
    }
}
