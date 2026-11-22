package br.csmp.core.npc;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Tela do Profeta, no estilo RPG: menu com "Missoes" e "Conversar".
 * Visual simples e provisorio (paineis desenhados por codigo); as texturas do escritor entram depois.
 */
public class ProfetaTela extends Screen {
    private enum Modo { MENU, MISSOES, CONVERSA }

    private static final int LARGURA = 280;
    private static final int ALTURA = 200;
    private static final int COR_BORDA = 0xFFC9A227;
    private static final int COR_FUNDO = 0xF0140E1E;
    private static final int COR_TITULO = 0xFFFFD866;
    private static final int COR_TEXTO = 0xFFE6E0D0;
    private static final int COR_APAGADO = 0xFF8A8478;

    private final int eruditos;
    private final ProfetaDados dados;
    private Modo modo = Modo.MENU;
    private int fala = 0;

    public ProfetaTela(int eruditos) {
        super(Component.translatable("entity.csmp.profeta"));
        this.eruditos = eruditos;
        this.dados = ProfetaDados.carregar();
    }

    private int esquerda() {
        return (this.width - LARGURA) / 2;
    }

    private int topo() {
        return (this.height - ALTURA) / 2;
    }

    @Override
    protected void init() {
        int x = esquerda();
        int y = topo();
        int largura = LARGURA - 40;
        int linhaBotao = y + ALTURA - 30;
        switch (this.modo) {
            case MENU -> {
                addRenderableWidget(Button.builder(Component.translatable("gui.csmp.profeta.missoes"),
                        b -> trocar(Modo.MISSOES)).bounds(x + 20, y + 84, largura, 20).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.csmp.profeta.conversar"),
                        b -> {
                            this.fala = 0;
                            trocar(Modo.CONVERSA);
                        }).bounds(x + 20, y + 112, largura, 20).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.csmp.profeta.sair"),
                        b -> onClose()).bounds(x + 20, y + 140, largura, 20).build());
            }
            case MISSOES -> addRenderableWidget(Button.builder(Component.translatable("gui.csmp.profeta.voltar"),
                    b -> trocar(Modo.MENU)).bounds(x + 20, linhaBotao, largura, 20).build());
            case CONVERSA -> {
                boolean ultima = this.fala >= this.dados.conversa().size() - 1;
                addRenderableWidget(Button.builder(Component.translatable("gui.csmp.profeta.voltar"),
                        b -> trocar(Modo.MENU)).bounds(x + 20, linhaBotao, 90, 20).build());
                if (!ultima) {
                    addRenderableWidget(Button.builder(Component.translatable("gui.csmp.profeta.proxima"),
                            b -> {
                                this.fala++;
                                rebuildWidgets();
                            }).bounds(x + LARGURA - 110, linhaBotao, 90, 20).build());
                }
            }
        }
    }

    private void trocar(Modo novo) {
        this.modo = novo;
        rebuildWidgets();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.renderBackground(g, mouseX, mouseY, parcial);
        int x = esquerda();
        int y = topo();
        g.fill(x - 2, y - 2, x + LARGURA + 2, y + ALTURA + 2, COR_BORDA);
        g.fill(x, y, x + LARGURA, y + ALTURA, COR_FUNDO);
        g.fill(x + 4, y + 4, x + LARGURA - 4, y + 5, COR_BORDA);
        g.fill(x + 4, y + 30, x + LARGURA - 4, y + 31, COR_BORDA);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.render(g, mouseX, mouseY, parcial);
        int x = esquerda();
        int y = topo();
        g.drawCenteredString(this.font, this.title, this.width / 2, y + 14, COR_TITULO);
        switch (this.modo) {
            case MENU -> {
                g.drawCenteredString(this.font, Component.translatable("gui.csmp.profeta.saudacao"),
                        this.width / 2, y + 48, COR_TEXTO);
                desenharEruditos(g, y + 64);
            }
            case MISSOES -> desenharMissoes(g, x + 14, y + 40);
            case CONVERSA -> {
                String texto = this.dados.conversa().get(Math.min(this.fala, this.dados.conversa().size() - 1));
                desenharTexto(g, texto, x + 18, y + 48, LARGURA - 36, COR_TEXTO);
            }
        }
    }

    private void desenharEruditos(GuiGraphics g, int y) {
        Component linha = Component.translatable("gui.csmp.profeta.eruditos", this.eruditos,
                ServerProfeta.TOTAL_ERUDITOS);
        g.drawCenteredString(this.font, linha, this.width / 2, y, COR_APAGADO);
    }

    private void desenharMissoes(GuiGraphics g, int x, int yInicial) {
        int y = yInicial;
        int largura = LARGURA - 28;
        for (ProfetaDados.Missao missao : this.dados.missoes()) {
            boolean bloqueada = "bloqueada".equals(missao.estado());
            boolean concluida = "concluida".equals(missao.estado());
            String marca = concluida ? "[x] " : (bloqueada ? "[-] " : "[ ] ");
            g.drawString(this.font, Component.literal(marca + missao.titulo()), x, y,
                    bloqueada ? COR_APAGADO : COR_TITULO, false);
            y += 11;
            y = desenharTexto(g, missao.descricao(), x + 10, y, largura - 10, bloqueada ? COR_APAGADO : COR_TEXTO);
            y += 5;
            if (y > topo() + ALTURA - 62) {
                break;
            }
        }
        desenharEruditos(g, topo() + ALTURA - 52);
    }

    /** Escreve o texto quebrando linhas; devolve o y logo depois da ultima linha. */
    private int desenharTexto(GuiGraphics g, String texto, int x, int y, int largura, int cor) {
        List<FormattedCharSequence> linhas = this.font.split(Component.literal(texto), largura);
        for (FormattedCharSequence linha : linhas) {
            g.drawString(this.font, linha, x, y, cor, false);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
