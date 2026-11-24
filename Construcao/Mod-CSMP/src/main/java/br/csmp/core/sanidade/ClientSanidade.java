package br.csmp.core.sanidade;

import br.csmp.core.CsmpMod;
import br.csmp.core.CsmpRegistro;
import br.csmp.core.npc.VazioNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Efeitos na tela da insanidade (sem barra, o jogador nao ve o numero):
 *  25+ vinheta escura e sussurros; 50+ tela ondulando e cores pulsando; 75+ tremor, vinheta vermelha e uma
 *  "sombra fantasma" que so o proprio jogador ve; 100 tudo junto e mais forte.
 */
@EventBusSubscriber(modid = CsmpMod.ID, value = Dist.CLIENT)
public final class ClientSanidade {
    private static float insanidade = 0.0F;
    private static VazioNpcEntity fantasma;
    private static long fantasmaAte = 0L;
    private static int contadorDeIds = 0;

    private ClientSanidade() {}

    public static void atualizar(float valor) {
        insanidade = valor;
    }

    private static int faixa() {
        return ServerSanidade.faixa(insanidade);
    }

    @SubscribeEvent
    public static void desenhar(RenderGuiEvent.Pre evento) {
        int faixa = faixa();
        if (faixa == 0 || Minecraft.getInstance().options.hideGui) {
            return;
        }
        var g = evento.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        double t = (System.currentTimeMillis() % 100000L) / 1000.0D;
        // a intensidade acompanha a insanidade dentro da faixa e "respira" devagar
        float base = Math.min(1.0F, insanidade / 100.0F);
        float respiro = (float) (0.85D + 0.15D * Math.sin(t * 0.9D));
        int cor = faixa >= 3 ? 0x5A0810 : 0x08040E;
        float alfaMax = Math.min(0.82F, (0.30F + 0.55F * base) * respiro);
        vinheta(g, w, h, cor, alfaMax, 0.34F + 0.10F * faixa);
        if (faixa >= 2) {
            // cores pulsando: um veu roxo que respira, mais rapido nas faixas altas
            int pulso = (int) (10 + 9 * Math.sin(t * (faixa >= 4 ? 2.6D : 1.3D)));
            g.fill(0, 0, w, h, (pulso << 24) | (faixa >= 3 ? 0x4A0E58 : 0x1A0C38));
        }
    }

    /**
     * Vinheta suave: molduras concentricas cada vez mais transparentes (queda quadratica), sem degraus visiveis.
     * {@code alcance} e a fracao da menor dimensao da tela que a vinheta ocupa a partir das bordas.
     */
    private static void vinheta(net.minecraft.client.gui.GuiGraphics g, int w, int h, int cor, float alfaMax, float alcance) {
        int passos = 40;
        int tamanho = (int) (Math.min(w, h) * alcance);
        float largura = Math.max(1.0F, tamanho / (float) passos);
        for (int i = 0; i < passos; i++) {
            float queda = 1.0F - i / (float) passos;
            int a = (int) (255 * alfaMax * queda * queda);
            if (a <= 0) {
                continue;
            }
            int c = (a << 24) | cor;
            int dentro = (int) (i * largura);
            int fora = (int) ((i + 1) * largura);
            g.fill(dentro, dentro, w - dentro, fora, c);
            g.fill(dentro, h - fora, w - dentro, h - dentro, c);
            g.fill(dentro, fora, fora, h - fora, c);
            g.fill(w - fora, fora, w - dentro, h - fora, c);
        }
    }

    @SubscribeEvent
    public static void tremor(ViewportEvent.ComputeCameraAngles evento) {
        int faixa = faixa();
        if (faixa < 3) {
            return;
        }
        double t = (System.currentTimeMillis() % 100000L) / 1000.0D;
        // tremores em surtos (nao o tempo todo), para incomodar sem enjoar
        double surto = Math.pow(Math.max(0.0D, Math.sin(t * 0.45D)), 3.0D);
        float forca = (float) ((faixa >= 4 ? 1.6D : 0.8D) * surto);
        evento.setRoll(evento.getRoll() + (float) Math.sin(t * 7.0D) * forca);
        evento.setYaw(evento.getYaw() + (float) Math.sin(t * 5.3D + 1.0D) * forca * 0.5F);
    }

    @SubscribeEvent
    public static void ondulacao(ComputeFovModifierEvent evento) {
        int faixa = faixa();
        if (faixa < 2) {
            return;
        }
        double t = (System.currentTimeMillis() % 100000L) / 1000.0D;
        evento.setNewFovModifier(evento.getNewFovModifier() * (1.0F + 0.035F * (float) Math.sin(t * 1.4D) * (faixa - 1)));
    }

    /** A sombra fantasma: aparece perto do jogador so no cliente dele, olha para ele e some depois de alguns segundos. */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel nivel = mc.level;
        LocalPlayer jogador = mc.player;
        if (nivel == null || jogador == null) {
            fantasma = null;
            return;
        }
        long tempo = nivel.getGameTime();
        if (fantasma != null && (tempo > fantasmaAte || faixa() < 3)) {
            nivel.removeEntity(fantasma.getId(), Entity.RemovalReason.DISCARDED);
            fantasma = null;
        }
        if (fantasma == null && faixa() >= 3 && !mc.isPaused()
                && nivel.random.nextInt(faixa() >= 4 ? 300 : 700) == 0) {
            double ang = nivel.random.nextDouble() * Math.PI * 2.0D;
            double dist = 9.0D + nivel.random.nextDouble() * 6.0D;
            double x = jogador.getX() + Math.cos(ang) * dist;
            double z = jogador.getZ() + Math.sin(ang) * dist;
            VazioNpcEntity nova = new VazioNpcEntity(CsmpRegistro.SOMBRA.get(), nivel);
            nova.setId(2_000_000_000 + (contadorDeIds++ % 1000));
            nova.setPos(x, jogador.getY(), z);
            double dx = jogador.getX() - x;
            double dz = jogador.getZ() - z;
            float rot = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
            nova.setYRot(rot);
            nova.setYHeadRot(rot);
            nova.setYBodyRot(rot);
            nivel.addEntity(nova);
            fantasma = nova;
            fantasmaAte = tempo + 120L + nivel.random.nextInt(100);
        }
    }
}
