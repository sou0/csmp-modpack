package br.csmp.core.npc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

/**
 * Tela do Profeta, no estilo RPG/MMORPG:
 *  - retrato do NPC (o modelo de verdade, acompanhando o mouse) e nome;
 *  - texto digitado letra por letra, com respostas clicaveis (ou teclas 1 a 9);
 *  - lista de missoes com rolagem e painel de detalhes;
 *  - as 8 gemas dos Eruditos.
 * Tudo desenhado por codigo; as texturas do escritor podem entrar depois.
 */
public class ProfetaTela extends Screen {
    private enum Modo { MENU, MISSOES, DIALOGO }

    private record Opcao(String texto, Runnable acao) {}

    private record Area(int x, int y, int w, int h) {
        boolean contem(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private record Layout(int x, int y, int w, int h, int cx, int cy, int cw, int ch,
                          int hFala, int yOpcoes, int yRodape) {}

    // --- medidas e cores ---
    private static final int COL_RETRATO = 118;
    private static final int ALTURA_OPCAO = 18;
    private static final int PASSO_OPCAO = 20;
    private static final int ALTURA_LINHA_MISSAO = 24;
    private static final long MS_POR_LETRA = 22L;
    private static final int OURO = 0xFFD9B44A;
    private static final int OURO_CLARO = 0xFFFFE08A;
    private static final int OURO_ESCURO = 0xFF7A5F1C;
    private static final int TEXTO = 0xFFEDE6D6;
    private static final int APAGADO = 0xFF9A93A8;
    private static final int FUNDO_TOPO = 0xF21B1030;
    private static final int FUNDO_BASE = 0xF20B0616;
    private static final int CAIXA = 0xB0120A20;
    private static final int VERDE = 0xFF7FD68B;

    private final int eruditos;
    private final int entidadeId;
    private final ProfetaDados dados;
    private final long abertura = System.currentTimeMillis();

    private Modo modo = Modo.MENU;
    private String texto = "";
    private List<Opcao> opcoes = List.of();
    private long inicioTexto;
    private int ultimoSom;
    private int selecionada;
    private int missaoSel;
    private double rolagem;

    public ProfetaTela(int eruditos, int entidadeId) {
        super(Component.translatable("entity.csmp.profeta"));
        this.eruditos = eruditos;
        this.entidadeId = entidadeId;
        this.dados = ProfetaDados.carregar();
        irParaMenu();
    }

    // ------------------------------------------------------------------ cenas e navegacao

    private static long agora() {
        return System.currentTimeMillis();
    }

    private static String t(String chave, Object... args) {
        return Component.translatable(chave, args).getString();
    }

    private void definirCena(String fala, List<Opcao> novas) {
        this.texto = fala;
        this.opcoes = novas;
        this.inicioTexto = agora();
        this.ultimoSom = 0;
        this.selecionada = 0;
    }

    private void irParaMenu() {
        this.modo = Modo.MENU;
        definirCena(this.dados.saudacao(), List.of(
                new Opcao(t("gui.csmp.profeta.missoes"), this::abrirMissoes),
                new Opcao(t("gui.csmp.profeta.conversar"), () -> irParaNo("inicio")),
                new Opcao(t("gui.csmp.profeta.sair"), this::onClose)));
    }

    private void abrirMissoes() {
        this.modo = Modo.MISSOES;
        this.missaoSel = Mth.clamp(this.missaoSel, 0, Math.max(0, this.dados.missoes().size() - 1));
        this.rolagem = 0;
        garantirVisivel();
    }

    private void irParaNo(String id) {
        ProfetaDados.No no = this.dados.dialogo().get(id);
        if (no == null) {
            irParaMenu();
            return;
        }
        this.modo = Modo.DIALOGO;
        List<Opcao> novas = new ArrayList<>();
        for (ProfetaDados.Resposta resposta : no.opcoes()) {
            String destino = resposta.vai();
            novas.add(new Opcao(resposta.texto(), () -> {
                if ("menu".equals(destino)) {
                    irParaMenu();
                } else if ("fechar".equals(destino)) {
                    onClose();
                } else {
                    irParaNo(destino);
                }
            }));
        }
        if (novas.isEmpty()) {
            novas.add(new Opcao(t("gui.csmp.profeta.voltar"), this::irParaMenu));
        }
        definirCena(no.fala(), novas);
    }

    private int revelados() {
        long decorrido = agora() - this.inicioTexto;
        return (int) Math.min(this.texto.length(), Math.max(0L, decorrido / MS_POR_LETRA));
    }

    private boolean terminou() {
        return revelados() >= this.texto.length();
    }

    private void completar() {
        this.inicioTexto = agora() - (long) this.texto.length() * MS_POR_LETRA - 1L;
    }

    private void tocar(float tom, float volume) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), tom, volume));
    }

    private void executar(int indice) {
        if (indice < 0 || indice >= this.opcoes.size()) {
            return;
        }
        this.selecionada = indice;
        tocar(1.1F, 0.6F);
        this.opcoes.get(indice).acao().run();
    }

    // ------------------------------------------------------------------ layout

    private Layout layout() {
        int w = Math.min(this.width - 24, 440);
        int h = Math.min(this.height - 24, 262);
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        int cx = x + COL_RETRATO + 8;
        int cy = y + 34;
        int cw = w - COL_RETRATO - 16;
        int ch = h - 34 - 8;
        int yRodape = y + h - 8 - 16;
        int hFala = Mth.clamp(ch - 20 - 8 - this.opcoes.size() * PASSO_OPCAO - 4, 56, 112);
        int yOpcoes = cy + hFala + 8;
        return new Layout(x, y, w, h, cx, cy, cw, ch, hFala, yOpcoes, yRodape);
    }

    private Area areaOpcao(Layout l, int indice) {
        return new Area(l.cx(), l.yOpcoes() + indice * PASSO_OPCAO, l.cw(), ALTURA_OPCAO);
    }

    private Area areaFechar(Layout l) {
        return new Area(l.x() + l.w() - 19, l.y() + 6, 13, 13);
    }

    private Area areaLista(Layout l) {
        return new Area(l.cx(), l.cy(), 132, l.ch() - 26);
    }

    private Area areaDetalhe(Layout l) {
        return new Area(l.cx() + 138, l.cy(), l.cw() - 138, l.ch() - 26);
    }

    private Area areaVoltar(Layout l) {
        return new Area(l.cx(), l.yRodape(), 86, ALTURA_OPCAO - 2);
    }

    private double maxRolagem(Layout l) {
        return Math.max(0, this.dados.missoes().size() * ALTURA_LINHA_MISSAO - areaLista(l).h());
    }

    private void garantirVisivel() {
        Layout l = layout();
        double topo = this.missaoSel * ALTURA_LINHA_MISSAO;
        double base = topo + ALTURA_LINHA_MISSAO;
        Area lista = areaLista(l);
        if (topo < this.rolagem) {
            this.rolagem = topo;
        } else if (base > this.rolagem + lista.h()) {
            this.rolagem = base - lista.h();
        }
        this.rolagem = Mth.clamp(this.rolagem, 0, maxRolagem(l));
    }

    // ------------------------------------------------------------------ desenho

    private void borda(GuiGraphics g, int x, int y, int w, int h, int cor) {
        g.fill(x, y, x + w, y + 1, cor);
        g.fill(x, y + h - 1, x + w, y + h, cor);
        g.fill(x, y, x + 1, y + h, cor);
        g.fill(x + w - 1, y, x + w, y + h, cor);
    }

    private void molduraDupla(GuiGraphics g, int x, int y, int w, int h) {
        borda(g, x - 2, y - 2, w + 4, h + 4, OURO_ESCURO);
        borda(g, x - 1, y - 1, w + 2, h + 2, OURO);
        int[][] cantos = {{x - 4, y - 4}, {x + w - 1, y - 4}, {x - 4, y + h - 1}, {x + w - 1, y + h - 1}};
        for (int[] canto : cantos) {
            g.fill(canto[0], canto[1], canto[0] + 5, canto[1] + 5, OURO);
            g.fill(canto[0] + 1, canto[1] + 1, canto[0] + 4, canto[1] + 4, OURO_ESCURO);
        }
    }

    private static int mistura(int a, int b, float f) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) Mth.lerp(f, ar, br) << 16) | ((int) Mth.lerp(f, ag, bg) << 8) | (int) Mth.lerp(f, ab, bb);
    }

    private void gema(GuiGraphics g, int x, int y, int cor, int brilho) {
        for (int i = 0; i < 9; i++) {
            int largura = 1 + 2 * Math.min(i, 8 - i);
            int inicio = x + 4 - largura / 2;
            g.fill(inicio, y + i, inicio + largura, y + i + 1, cor);
        }
        if (brilho != 0) {
            g.fill(x + 3, y + 2, x + 5, y + 4, brilho);
        }
    }

    private void desenharEruditos(GuiGraphics g, int x, int y) {
        String rotulo = t("gui.csmp.profeta.eruditos");
        g.drawString(this.font, rotulo, x, y + 4, APAGADO, false);
        int inicio = x + this.font.width(rotulo) + 8;
        float pulso = 0.5F + 0.5F * Mth.sin(agora() / 380.0F);
        for (int i = 0; i < ServerProfeta.TOTAL_ERUDITOS; i++) {
            boolean livre = i < this.eruditos;
            int cor = livre ? mistura(OURO, OURO_CLARO, pulso) : 0xFF2A2238;
            gema(g, inicio + i * 13, y + 3, cor, livre ? 0xCCFFFFFF : 0);
        }
        g.drawString(this.font, this.eruditos + "/" + ServerProfeta.TOTAL_ERUDITOS,
                inicio + ServerProfeta.TOTAL_ERUDITOS * 13 + 4, y + 4, TEXTO, false);
    }

    private void desenharPoeira(GuiGraphics g, Layout l) {
        long tempo = agora();
        for (int i = 0; i < 18; i++) {
            int px = l.x() + 6 + (int) ((i * 97L + tempo / 55L) % Math.max(1, l.w() - 12));
            int py = l.y() + l.h() - 6 - (int) ((tempo / (18L + (i % 5) * 6L) + i * 41L) % Math.max(1, l.h() - 12));
            int alfa = 36 + (int) (36 * (0.5 + 0.5 * Math.sin(tempo / 420.0 + i)));
            g.fill(px, py, px + 2, py + 2, (alfa << 24) | 0xD9B44A);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        renderBackground(g, mouseX, mouseY, parcial);
        Layout l = layout();
        float a = Mth.clamp((agora() - this.abertura) / 240.0F, 0.0F, 1.0F);
        float suave = 1.0F - (1.0F - a) * (1.0F - a);

        g.pose().pushPose();
        g.pose().translate(0.0F, (1.0F - suave) * 14.0F, 0.0F);

        // painel
        g.fill(l.x() + 4, l.y() + 4, l.x() + l.w() + 4, l.y() + l.h() + 4, 0x70000000);
        g.fillGradient(l.x(), l.y(), l.x() + l.w(), l.y() + l.h(), FUNDO_TOPO, FUNDO_BASE);
        desenharPoeira(g, l);
        molduraDupla(g, l.x(), l.y(), l.w(), l.h());
        desenharCabecalho(g, l, mouseX, mouseY);
        desenharRetrato(g, l, mouseX, mouseY);

        // som da digitacao
        if (this.modo != Modo.MISSOES) {
            int rev = revelados();
            if (rev > this.ultimoSom + 2 && rev < this.texto.length()) {
                this.ultimoSom = rev;
                tocar(1.5F + 0.4F * (float) Math.random(), 0.12F);
            }
        }

        switch (this.modo) {
            case MISSOES -> desenharMissoes(g, l, mouseX, mouseY);
            default -> desenharConversa(g, l, mouseX, mouseY);
        }
        g.pose().popPose();
    }

    private void desenharCabecalho(GuiGraphics g, Layout l, int mouseX, int mouseY) {
        g.fillGradient(l.x() + 3, l.y() + 3, l.x() + l.w() - 3, l.y() + 27, 0xFF2D1B52, 0xFF1A0F30);
        g.fill(l.x() + 3, l.y() + 27, l.x() + l.w() - 3, l.y() + 28, OURO);
        g.pose().pushPose();
        g.pose().translate((float) (l.x() + 14), (float) (l.y() + 8), 0.0F);
        g.pose().scale(1.25F, 1.25F, 1.0F);
        g.drawString(this.font, Component.literal(t("entity.csmp.profeta").toUpperCase())
                .withStyle(ChatFormatting.BOLD), 0, 0, OURO_CLARO, true);
        g.pose().popPose();
        if (!this.dados.titulo().isEmpty()) {
            int largura = this.font.width(this.dados.titulo());
            g.drawString(this.font, this.dados.titulo(), l.x() + l.w() - 28 - largura, l.y() + 10, APAGADO, false);
        }
        Area fechar = areaFechar(l);
        boolean sobre = fechar.contem(mouseX, mouseY);
        g.fill(fechar.x(), fechar.y(), fechar.x() + fechar.w(), fechar.y() + fechar.h(), sobre ? 0xFF8C2B2B : 0xFF3A2650);
        borda(g, fechar.x(), fechar.y(), fechar.w(), fechar.h(), sobre ? 0xFFFF8A8A : OURO_ESCURO);
        g.drawString(this.font, "x", fechar.x() + 4, fechar.y() + 3, sobre ? 0xFFFFFFFF : TEXTO, false);
    }

    private void desenharRetrato(GuiGraphics g, Layout l, int mouseX, int mouseY) {
        int rx = l.x() + 8;
        int ry = l.y() + 34;
        int rw = COL_RETRATO - 8;
        int rh = l.h() - 34 - 8;
        g.fillGradient(rx, ry, rx + rw, ry + rh, 0xFF241540, 0xFF0F0820);
        borda(g, rx, ry, rw, rh, OURO_ESCURO);
        // halo atras do NPC
        g.fillGradient(rx + 1, ry + 1, rx + rw - 1, ry + rh / 2, 0x33D9B44A, 0x00D9B44A);

        Minecraft mc = Minecraft.getInstance();
        Entity entidade = mc.level == null ? null : mc.level.getEntity(this.entidadeId);
        if (entidade instanceof LivingEntity vivo) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, rx + 2, ry + 4, rx + rw - 2, ry + rh - 28,
                    44, 0.0625F, (float) mouseX, (float) mouseY, vivo);
        } else {
            g.drawCenteredString(this.font, "?", rx + rw / 2, ry + rh / 2 - 12, OURO);
        }
        // placa com o nome
        int py = ry + rh - 26;
        g.fillGradient(rx + 1, py, rx + rw - 1, ry + rh - 1, 0xFF3A2268, 0xFF1A0F30);
        g.fill(rx + 1, py, rx + rw - 1, py + 1, OURO);
        g.drawCenteredString(this.font, Component.literal(t("entity.csmp.profeta")).withStyle(ChatFormatting.BOLD),
                rx + rw / 2, py + 5, OURO_CLARO);
        g.drawCenteredString(this.font, t("gui.csmp.profeta.papel"), rx + rw / 2, py + 15, APAGADO);
    }

    private void desenharConversa(GuiGraphics g, Layout l, int mouseX, int mouseY) {
        // caixa de fala
        g.fill(l.cx(), l.cy(), l.cx() + l.cw(), l.cy() + l.hFala(), CAIXA);
        borda(g, l.cx(), l.cy(), l.cw(), l.hFala(), OURO_ESCURO);
        g.fill(l.cx() + 1, l.cy() + 1, l.cx() + l.cw() - 1, l.cy() + 2, 0x44D9B44A);

        int restante = revelados();
        List<FormattedText> linhas = this.font.getSplitter().splitLines(this.texto, l.cw() - 20, Style.EMPTY);
        int y = l.cy() + 10;
        for (FormattedText linha : linhas) {
            String s = linha.getString();
            int mostrar = Math.min(restante, s.length());
            if (mostrar > 0) {
                g.drawString(this.font, s.substring(0, mostrar), l.cx() + 10, y, TEXTO, false);
            }
            restante -= s.length();
            y += 11;
            if (restante <= 0 || y > l.cy() + l.hFala() - 12) {
                break;
            }
        }
        boolean pronto = terminou();
        if (!pronto) {
            String dica = t("gui.csmp.profeta.pular");
            g.drawString(this.font, dica, l.cx() + l.cw() - this.font.width(dica) - 8,
                    l.cy() + l.hFala() - 12, 0xFF6E6680, false);
        }

        // respostas (so aparecem quando o texto termina)
        if (pronto) {
            for (int i = 0; i < this.opcoes.size(); i++) {
                Area area = areaOpcao(l, i);
                boolean destaque = area.contem(mouseX, mouseY) || i == this.selecionada;
                if (destaque) {
                    g.fillGradient(area.x(), area.y(), area.x() + area.w(), area.y() + area.h(), 0x66D9B44A, 0x22D9B44A);
                    g.fill(area.x(), area.y(), area.x() + 2, area.y() + area.h(), OURO_CLARO);
                } else {
                    g.fill(area.x(), area.y(), area.x() + area.w(), area.y() + area.h(), 0x55120A20);
                }
                borda(g, area.x(), area.y(), area.w(), area.h(), destaque ? OURO : 0xFF3A2C58);
                g.drawString(this.font, (i + 1) + ".", area.x() + 7, area.y() + 5, OURO, false);
                g.drawString(this.font, this.font.plainSubstrByWidth(this.opcoes.get(i).texto(), area.w() - 34),
                        area.x() + 22, area.y() + 5, destaque ? 0xFFFFFFFF : 0xFFD2CADB, false);
            }
        }
        desenharEruditos(g, l.cx(), l.yRodape());
    }

    private void desenharMissoes(GuiGraphics g, Layout l, int mouseX, int mouseY) {
        Area lista = areaLista(l);
        Area detalhe = areaDetalhe(l);

        // lista rolavel
        g.fill(lista.x(), lista.y(), lista.x() + lista.w(), lista.y() + lista.h(), CAIXA);
        borda(g, lista.x(), lista.y(), lista.w(), lista.h(), OURO_ESCURO);
        g.enableScissor(lista.x() + 1, lista.y() + 1, lista.x() + lista.w() - 1, lista.y() + lista.h() - 1);
        List<ProfetaDados.Missao> missoes = this.dados.missoes();
        for (int i = 0; i < missoes.size(); i++) {
            int ly = lista.y() + 1 + i * ALTURA_LINHA_MISSAO - (int) this.rolagem;
            if (ly + ALTURA_LINHA_MISSAO < lista.y() || ly > lista.y() + lista.h()) {
                continue;
            }
            ProfetaDados.Missao m = missoes.get(i);
            boolean sel = i == this.missaoSel;
            boolean sobre = lista.contem(mouseX, mouseY) && mouseY >= ly && mouseY < ly + ALTURA_LINHA_MISSAO;
            if (sel) {
                g.fillGradient(lista.x() + 1, ly, lista.x() + lista.w() - 1, ly + ALTURA_LINHA_MISSAO - 1, 0x66D9B44A, 0x22D9B44A);
                g.fill(lista.x() + 1, ly, lista.x() + 3, ly + ALTURA_LINHA_MISSAO - 1, OURO_CLARO);
            } else if (sobre) {
                g.fill(lista.x() + 1, ly, lista.x() + lista.w() - 1, ly + ALTURA_LINHA_MISSAO - 1, 0x33D9B44A);
            }
            desenharMarca(g, lista.x() + 8, ly + 6, m.estado());
            boolean bloqueada = "bloqueada".equals(m.estado());
            g.drawString(this.font, this.font.plainSubstrByWidth(m.titulo(), lista.w() - 34),
                    lista.x() + 24, ly + 8, bloqueada ? APAGADO : (sel ? 0xFFFFFFFF : TEXTO), false);
        }
        g.disableScissor();
        double max = maxRolagem(l);
        if (max > 0) {
            int trilhaAltura = lista.h() - 4;
            int barra = Math.max(14, (int) (trilhaAltura * (lista.h() / (lista.h() + max))));
            int topoBarra = lista.y() + 2 + (int) ((trilhaAltura - barra) * (this.rolagem / max));
            g.fill(lista.x() + lista.w() - 4, topoBarra, lista.x() + lista.w() - 2, topoBarra + barra, OURO);
        }

        // detalhes
        g.fill(detalhe.x(), detalhe.y(), detalhe.x() + detalhe.w(), detalhe.y() + detalhe.h(), CAIXA);
        borda(g, detalhe.x(), detalhe.y(), detalhe.w(), detalhe.h(), OURO_ESCURO);
        if (!missoes.isEmpty()) {
            ProfetaDados.Missao m = missoes.get(Mth.clamp(this.missaoSel, 0, missoes.size() - 1));
            int dx = detalhe.x() + 10;
            int dy = detalhe.y() + 9;
            int largura = detalhe.w() - 20;
            for (FormattedCharSequence linha : this.font.split(Component.literal(m.titulo()), largura)) {
                g.drawString(this.font, linha, dx, dy, OURO_CLARO, true);
                dy += 11;
            }
            g.drawString(this.font, rotuloEstado(m.estado()), dx, dy + 1, corEstado(m.estado()), false);
            dy += 16;
            g.fill(dx, dy - 4, dx + largura, dy - 3, 0x55D9B44A);
            dy = escrever(g, m.descricao(), dx, dy, largura, TEXTO) + 4;
            if (!m.objetivo().isEmpty()) {
                g.drawString(this.font, t("gui.csmp.profeta.objetivo"), dx, dy, OURO, false);
                dy = escrever(g, m.objetivo(), dx, dy + 11, largura, TEXTO) + 4;
            }
            if (!m.recompensa().isEmpty()) {
                g.drawString(this.font, t("gui.csmp.profeta.recompensa"), dx, dy, OURO, false);
                escrever(g, m.recompensa(), dx, dy + 11, largura, TEXTO);
            }
        }

        // rodape: voltar + eruditos
        Area voltar = areaVoltar(l);
        boolean sobre = voltar.contem(mouseX, mouseY);
        g.fillGradient(voltar.x(), voltar.y(), voltar.x() + voltar.w(), voltar.y() + voltar.h(),
                sobre ? 0x66D9B44A : 0x55120A20, sobre ? 0x22D9B44A : 0x55120A20);
        borda(g, voltar.x(), voltar.y(), voltar.w(), voltar.h(), sobre ? OURO : 0xFF3A2C58);
        g.drawString(this.font, "< " + t("gui.csmp.profeta.voltar"), voltar.x() + 10, voltar.y() + 4,
                sobre ? 0xFFFFFFFF : 0xFFD2CADB, false);
        desenharEruditos(g, l.cx() + 96, l.yRodape());
    }

    private int escrever(GuiGraphics g, String s, int x, int y, int largura, int cor) {
        for (FormattedCharSequence linha : this.font.split(Component.literal(s), largura)) {
            g.drawString(this.font, linha, x, y, cor, false);
            y += 10;
        }
        return y;
    }

    private void desenharMarca(GuiGraphics g, int x, int y, String estado) {
        int cor = corEstado(estado);
        g.fill(x, y, x + 12, y + 12, 0xFF1A1030);
        borda(g, x, y, 12, 12, cor);
        String simbolo = switch (estado) {
            case "concluida" -> "v";
            case "bloqueada" -> "-";
            default -> "!";
        };
        g.drawString(this.font, simbolo, x + 4, y + 2, cor, false);
    }

    private static int corEstado(String estado) {
        return switch (estado) {
            case "concluida" -> VERDE;
            case "bloqueada" -> APAGADO;
            default -> OURO_CLARO;
        };
    }

    private static String rotuloEstado(String estado) {
        return switch (estado) {
            case "concluida" -> t("gui.csmp.profeta.estado.concluida");
            case "bloqueada" -> t("gui.csmp.profeta.estado.bloqueada");
            default -> t("gui.csmp.profeta.estado.disponivel");
        };
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao != 0) {
            return super.mouseClicked(mx, my, botao);
        }
        Layout l = layout();
        if (areaFechar(l).contem(mx, my)) {
            tocar(0.9F, 0.6F);
            onClose();
            return true;
        }
        if (this.modo == Modo.MISSOES) {
            if (areaVoltar(l).contem(mx, my)) {
                tocar(0.9F, 0.6F);
                irParaMenu();
                return true;
            }
            Area lista = areaLista(l);
            if (lista.contem(mx, my)) {
                int indice = (int) ((my - lista.y() - 1 + this.rolagem) / ALTURA_LINHA_MISSAO);
                if (indice >= 0 && indice < this.dados.missoes().size()) {
                    this.missaoSel = indice;
                    tocar(1.3F, 0.5F);
                }
                return true;
            }
            return super.mouseClicked(mx, my, botao);
        }
        if (!terminou()) {
            completar();
            return true;
        }
        for (int i = 0; i < this.opcoes.size(); i++) {
            if (areaOpcao(l, i).contem(mx, my)) {
                executar(i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, botao);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (this.modo != Modo.MISSOES && terminou()) {
            Layout l = layout();
            for (int i = 0; i < this.opcoes.size(); i++) {
                if (areaOpcao(l, i).contem(mx, my)) {
                    this.selecionada = i;
                }
            }
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double rolagemX, double rolagemY) {
        if (this.modo == Modo.MISSOES) {
            Layout l = layout();
            this.rolagem = Mth.clamp(this.rolagem - rolagemY * 18.0, 0, maxRolagem(l));
            return true;
        }
        return super.mouseScrolled(mx, my, rolagemX, rolagemY);
    }

    @Override
    public boolean keyPressed(int tecla, int scan, int modificadores) {
        if (this.modo == Modo.MISSOES) {
            if (tecla == GLFW.GLFW_KEY_BACKSPACE) {
                irParaMenu();
                return true;
            }
            if (tecla == GLFW.GLFW_KEY_UP) {
                this.missaoSel = Math.max(0, this.missaoSel - 1);
                garantirVisivel();
                tocar(1.3F, 0.4F);
                return true;
            }
            if (tecla == GLFW.GLFW_KEY_DOWN) {
                this.missaoSel = Math.min(this.dados.missoes().size() - 1, this.missaoSel + 1);
                garantirVisivel();
                tocar(1.3F, 0.4F);
                return true;
            }
            return super.keyPressed(tecla, scan, modificadores);
        }
        if (tecla == GLFW.GLFW_KEY_ENTER || tecla == GLFW.GLFW_KEY_KP_ENTER || tecla == GLFW.GLFW_KEY_SPACE) {
            if (!terminou()) {
                completar();
            } else {
                executar(this.selecionada);
            }
            return true;
        }
        if (tecla == GLFW.GLFW_KEY_UP && terminou()) {
            this.selecionada = Math.max(0, this.selecionada - 1);
            tocar(1.3F, 0.4F);
            return true;
        }
        if (tecla == GLFW.GLFW_KEY_DOWN && terminou()) {
            this.selecionada = Math.min(this.opcoes.size() - 1, this.selecionada + 1);
            tocar(1.3F, 0.4F);
            return true;
        }
        if (tecla >= GLFW.GLFW_KEY_1 && tecla <= GLFW.GLFW_KEY_9) {
            if (terminou()) {
                executar(tecla - GLFW.GLFW_KEY_1);
            } else {
                completar();
            }
            return true;
        }
        return super.keyPressed(tecla, scan, modificadores);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
