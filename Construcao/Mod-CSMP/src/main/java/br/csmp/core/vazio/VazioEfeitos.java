package br.csmp.core.vazio;

import br.csmp.core.CsmpConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Efeitos de ceu do Vazio (client): sem sol, lua, estrelas, skybox e, por padrao, sem nuvens e sem a chuva
 * desenhada pelo jogo. So fica a nevoa vermelha. As nuvens podem voltar pela config do cliente
 * (csmp-client.toml, "nuvensNoVazio").
 */
public class VazioEfeitos extends DimensionSpecialEffects {

    public VazioEfeitos() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    private static boolean nuvens() {
        try {
            return CsmpConfig.NUVENS_NO_VAZIO.get();
        } catch (IllegalStateException naoCarregada) {
            return false;
        }
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 cor, float brilho) {
        return cor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public float getCloudHeight() {
        return nuvens() ? 192.0F : Float.NaN;
    }

    @Override
    public boolean renderSky(ClientLevel nivel, int ticks, float parcial, Matrix4f modelo, Camera camera,
                             Matrix4f projecao, boolean nevoa, Runnable preparar) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel nivel, int ticks, float parcial, LightTexture luz,
                                     double x, double y, double z) {
        return true;
    }
}
