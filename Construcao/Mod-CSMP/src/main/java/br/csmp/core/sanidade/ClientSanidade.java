package br.csmp.core.sanidade;

import br.csmp.core.CsmpMod;
import br.csmp.core.CsmpRegistro;
import br.csmp.core.npc.VazioNpcEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
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
 *  25+ overlay escuro/vermelho com TV antiga (cresce com a insanidade) e sussurros; 50+ tela ondulando; 75+ tremor,
 *  piscadas de TV e uma
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

    private static final ResourceLocation SOMBRA = textura("insanidade_sombra");
    private static final ResourceLocation VERMELHO = textura("insanidade_vermelho");
    private static final ResourceLocation CHIADO = textura("tv_chiado");
    private static final ResourceLocation LINHAS = textura("tv_linhas");
    private static final java.util.Random SORTE = new java.util.Random();
    private static int chiadoU = 0;
    private static int chiadoV = 0;
    private static long trocaChiado = 0L;
    private static long piscaAte = 0L;

    private static ResourceLocation textura(String nome) {
        return ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "textures/gui/" + nome + ".png");
    }

    /**
     * Overlay da insanidade pedido pelo lider: bordas escuras com fumaca e um veu vermelho manchado que vai ficando
     * mais visivel e mais vermelho conforme a insanidade sobe, com efeito de TV antiga por cima (linhas, chiado,
     * faixa rolando e piscadas nas faixas altas). As texturas ficam em textures/gui e podem ser trocadas.
     */
    @SubscribeEvent
    public static void desenhar(RenderGuiEvent.Pre evento) {
        if (faixa() == 0 || Minecraft.getInstance().options.hideGui) {
            return;
        }
        var g = evento.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        long agora = System.currentTimeMillis();
        double t = (agora % 100000L) / 1000.0D;
        // 0 em 25 de insanidade, 1 em 100
        float s = Math.max(0.0F, Math.min(1.0F, (insanidade - 25.0F) / 75.0F));
        float respiro = (float) (0.90D + 0.10D * Math.sin(t * 0.9D));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        cobrir(g, SOMBRA, w, h, (0.45F + 0.50F * s) * respiro);
        cobrir(g, VERMELHO, w, h, (0.12F + 0.68F * s * s) * respiro);

        // TV antiga: linhas sempre, chiado que muda ~20 vezes por segundo
        cobrir(g, LINHAS, w, h, 0.06F + 0.16F * s);
        if (agora >= trocaChiado) {
            chiadoU = SORTE.nextInt(256);
            chiadoV = SORTE.nextInt(256);
            trocaChiado = agora + 50L;
        }
        g.setColor(1.0F, 1.0F, 1.0F, 0.05F + 0.13F * s);
        g.blit(CHIADO, 0, 0, w, h, chiadoU, chiadoV, 256, 256, 512, 512);
        // faixa clara que desce devagar, como imagem fora de sintonia
        int altura = Math.max(8, h / 9);
        int y = (int) ((t * 0.12D % 1.0D) * (h + altura)) - altura;
        int a = (int) (255 * (0.03F + 0.06F * s));
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.fillGradient(0, y, w, y + altura / 2, 0x00FFFFFF, (a << 24) | 0xFFE0E0);
        g.fillGradient(0, y + altura / 2, w, y + altura, (a << 24) | 0xFFE0E0, 0x00FFFFFF);
        // nas faixas altas a imagem "cai" por um instante de vez em quando
        if (faixa() >= 3 && agora >= piscaAte + 400L && SORTE.nextInt(faixa() >= 4 ? 90 : 220) == 0) {
            piscaAte = agora + 60L + SORTE.nextInt(90);
        }
        if (agora < piscaAte) {
            g.fill(0, 0, w, h, 0x9A050000);
            g.setColor(1.0F, 1.0F, 1.0F, 0.35F);
            g.blit(CHIADO, 0, 0, w, h, chiadoV, chiadoU, 256, 256, 512, 512);
        }
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void cobrir(net.minecraft.client.gui.GuiGraphics g, ResourceLocation tex, int w, int h, float alfa) {
        if (alfa <= 0.0F) {
            return;
        }
        g.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alfa));
        g.blit(tex, 0, 0, w, h, 0.0F, 0.0F, 512, 512, 512, 512);
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
