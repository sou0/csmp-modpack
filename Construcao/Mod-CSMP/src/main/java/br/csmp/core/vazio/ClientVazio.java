package br.csmp.core.vazio;

import br.csmp.core.CsmpMod;
import br.csmp.core.CsmpSons;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * Efeitos so do cliente na dimensao Vazio: nevoa vermelha e fechada, chuva vermelha que vai e volta,
 * sustos de terror e as luzes dos jogadores do overworld.
 */
@EventBusSubscriber(modid = CsmpMod.ID, value = Dist.CLIENT)
public final class ClientVazio {
    private static long proximoSusto = 0L;
    private static boolean estavaNoVazio = false;

    private ClientVazio() {}

    @SubscribeEvent
    public static void corDaNevoa(ViewportEvent.ComputeFogColor evento) {
        ClientLevel nivel = Minecraft.getInstance().level;
        if (!Vazio.eh(nivel)) {
            return;
        }
        Vec3 cor = AtmosferaVazio.corNevoa(nivel.getGameTime(), (float) evento.getPartialTick());
        evento.setRed((float) cor.x);
        evento.setGreen((float) cor.y);
        evento.setBlue((float) cor.z);
    }

    @SubscribeEvent
    public static void distanciaDaNevoa(ViewportEvent.RenderFog evento) {
        ClientLevel nivel = Minecraft.getInstance().level;
        if (!Vazio.eh(nivel) || evento.getMode() != FogRenderer.FogMode.FOG_TERRAIN || evento.getType() != FogType.NONE) {
            return;
        }
        evento.setNearPlaneDistance(0.0F);
        evento.setFarPlaneDistance(AtmosferaVazio.distanciaDaNevoa(nivel.getGameTime(), (float) evento.getPartialTick()));
        evento.setFogShape(FogShape.SPHERE);
        evento.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel nivel = mc.level;
        LocalPlayer jogador = mc.player;
        if (nivel == null || jogador == null || !Vazio.eh(nivel)) {
            if (estavaNoVazio) {
                estavaNoVazio = false;
                ClientLuzes.limpar();
                proximoSusto = 0L;
            }
            return;
        }
        estavaNoVazio = true;
        if (mc.isPaused()) {
            return;
        }
        long tempo = nivel.getGameTime();
        float chuva = AtmosferaVazio.chuva(tempo, 0.0F);
        if (chuva > 0.02F) {
            chuvaVermelha(nivel, jogador, chuva);
            if (tempo % 70L == 0L && chuva > 0.25F) {
                nivel.playLocalSound(jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.WEATHER_RAIN,
                        SoundSource.WEATHER, 0.5F * chuva, 0.6F, false);
            }
        }
        sustos(nivel, jogador, tempo);
        ClientLuzes.tick(nivel, jogador, tempo);
    }

    private static void chuvaVermelha(ClientLevel nivel, LocalPlayer jogador, float chuva) {
        RandomSource r = nivel.random;
        int quantidade = (int) (14.0F * chuva);
        for (int i = 0; i < quantidade; i++) {
            double x = jogador.getX() + (r.nextDouble() - 0.5D) * 24.0D;
            double z = jogador.getZ() + (r.nextDouble() - 0.5D) * 24.0D;
            double y = jogador.getY() + 6.0D + r.nextDouble() * 8.0D;
            nivel.addParticle(new DustParticleOptions(new Vector3f(0.85F, 0.02F, 0.05F), 1.1F), x, y, z, 0.0D, -0.9D, 0.0D);
        }
    }

    /** De tempos em tempos, um som ruim vem de algum lugar perto, sem avisar. */
    private static void sustos(ClientLevel nivel, LocalPlayer jogador, long tempo) {
        RandomSource r = nivel.random;
        if (proximoSusto == 0L) {
            proximoSusto = tempo + 400L + r.nextInt(600);
            return;
        }
        if (tempo < proximoSusto) {
            return;
        }
        proximoSusto = tempo + 600L + r.nextInt(1800);
        SoundEvent[] opcoes = {CsmpSons.SUSTO_CORACAO.get(), CsmpSons.SUSTO_GRITO.get(), CsmpSons.SUSTO_PASSOS.get()};
        SoundEvent som = opcoes[r.nextInt(opcoes.length)];
        double angulo = r.nextDouble() * Math.PI * 2.0D;
        double distancia = 6.0D + r.nextDouble() * 4.0D;
        nivel.playLocalSound(jogador.getX() + Math.cos(angulo) * distancia, jogador.getY(),
                jogador.getZ() + Math.sin(angulo) * distancia, som, SoundSource.AMBIENT,
                1.0F, 0.9F + r.nextFloat() * 0.2F, false);
    }
}
