package br.csmp.core.santuario;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Menu da estatua: mostra quem esta preso no Vazio perto de um farol aceso ligado a ela e quao insano estava ao cair.
 * Escolher alguem inicia o ritual. Mesmo estilo do Profeta: painel escuro com moldura e animacao de entrada.
 */
public class RitualTela extends Screen {
    private static final int LARGURA = 260;
    private static final int LINHA = 26;
    private static final int OURO = 0xFFD9B44A;
    private static final int VERMELHO = 0xFFB0303A;

    private final RitualPayloads.AbrirMenu menu;
    private final long abertura = System.currentTimeMillis();

    public RitualTela(RitualPayloads.AbrirMenu menu) {
        super(Component.translatable("gui.csmp.ritual.titulo"));
        this.menu = menu;
    }

    /** Chamado pelo pacote do servidor (so no cliente). */
    public static void abrir(RitualPayloads.AbrirMenu menu) {
        Minecraft.getInstance().setScreen(new RitualTela(menu));
    }

    private int altura() {
        return 64 + Math.max(1, this.menu.presos().size()) * LINHA;
    }

    @Override
    protected void init() {
        int x = (this.width - LARGURA) / 2;
        int y = (this.height - altura()) / 2;
        List<RitualPayloads.Preso> presos = this.menu.presos();
        for (int i = 0; i < presos.size(); i++) {
            RitualPayloads.Preso preso = presos.get(i);
            addRenderableWidget(Button.builder(Component.translatable("gui.csmp.ritual.reviver"), botao -> {
                PacketDistributor.sendToServer(new RitualPayloads.Escolha(this.menu.base(), preso.id(), this.menu.amuleto()));
                onClose();
            }).bounds(x + LARGURA - 80, y + 40 + i * LINHA, 68, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.csmp.ritual.cancelar"), botao -> onClose())
                .bounds(x + (LARGURA - 80) / 2, y + altura() - 26, 80, 18).build());
    }

    /** O painel e desenhado junto com o fundo, para os botoes ficarem por cima (o jogo so deixa borrar o fundo 1 vez). */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.renderBackground(g, mouseX, mouseY, parcial);
        float a = Mth.clamp((System.currentTimeMillis() - this.abertura) / 220.0F, 0.0F, 1.0F);
        int x = (this.width - LARGURA) / 2;
        int y = (this.height - altura()) / 2 + (int) ((1.0F - a) * 10);
        int h = altura();
        g.fill(x + 3, y + 3, x + LARGURA + 3, y + h + 3, 0x70000000);
        g.fillGradient(x, y, x + LARGURA, y + h, 0xF21B0F14, 0xF20A0508);
        g.fill(x, y, x + LARGURA, y + 1, OURO);
        g.fill(x, y + h - 1, x + LARGURA, y + h, OURO);
        g.fill(x, y, x + 1, y + h, OURO);
        g.fill(x + LARGURA - 1, y, x + LARGURA, y + h, OURO);
        g.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), x + LARGURA / 2, y + 10, OURO);
        Component modo = Component.translatable(this.menu.amuleto() ? "gui.csmp.ritual.modo_amuleto" : "gui.csmp.ritual.modo_troca");
        g.drawCenteredString(this.font, modo, x + LARGURA / 2, y + 23, this.menu.amuleto() ? 0xFFBFAF8F : VERMELHO);
        List<RitualPayloads.Preso> presos = this.menu.presos();
        for (int i = 0; i < presos.size(); i++) {
            RitualPayloads.Preso preso = presos.get(i);
            int ly = y + 40 + i * LINHA;
            g.fill(x + 8, ly - 2, x + LARGURA - 8, ly + 22, 0x40000000);
            g.drawString(this.font, preso.nome(), x + 14, ly + 1, 0xFFEDE6D6, false);
            int insano = Math.round(preso.insanidade());
            g.drawString(this.font, Component.translatable("gui.csmp.ritual.insanidade", insano), x + 14, ly + 11,
                    insano >= 75 ? VERMELHO : insano >= 50 ? 0xFFD08A3A : 0xFF9A93A8, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
