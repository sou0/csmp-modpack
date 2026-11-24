package br.csmp.core.santuario;

import br.csmp.core.CsmpMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Tremor de camera curto que vai sumindo (ease-out), pedido pelo servidor. */
@EventBusSubscriber(modid = CsmpMod.ID, value = Dist.CLIENT)
public final class ClientTremor {
    private static long inicio = 0L;
    private static int duracao = 0;
    private static float forca = 0.0F;

    private ClientTremor() {}

    public static void tremer(int ticks, float novaForca) {
        var nivel = Minecraft.getInstance().level;
        if (nivel == null) {
            return;
        }
        inicio = nivel.getGameTime();
        duracao = Math.max(1, ticks);
        forca = novaForca;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles evento) {
        var nivel = Minecraft.getInstance().level;
        if (nivel == null || duracao == 0) {
            return;
        }
        double t = nivel.getGameTime() - inicio + evento.getPartialTick();
        if (t >= duracao) {
            duracao = 0;
            return;
        }
        double resto = 1.0D - t / duracao;
        double intensidade = forca * resto * resto;
        evento.setPitch(evento.getPitch() + (float) (Math.sin(t * 2.9D) * intensidade));
        evento.setYaw(evento.getYaw() + (float) (Math.sin(t * 2.3D + 1.7D) * intensidade));
        evento.setRoll(evento.getRoll() + (float) (Math.sin(t * 3.7D + 0.4D) * intensidade * 0.6D));
    }
}
