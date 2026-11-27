// CSMP - instalador e atualizador do modpack.
// Compilado com o csc do .NET Framework 4 (vem no Windows) por compilar-instalador.ps1. C# 5: sem $"", sem ?. e sem =>.
//
// Primeira vez: mostra a tela de boas-vindas com a pasta do jogo encontrada (o jogador pode trocar) e o que vai ser
// instalado; nada e feito antes de clicar em Instalar. Depois: a cada abertura confere o manifesto no GitHub, instala o
// NeoForge se faltar, baixa so os arquivos que mudaram, apaga os que sairam do pack e abre o launcher.
// Configs que o jogador mudou nao sao sobrescritas; mods de fora so sao movidos se o jogador deixar.
using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Net;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Web.Script.Serialization;
using System.Windows.Forms;

[assembly: AssemblyTitle("CSMP")]
[assembly: AssemblyDescription("Instalador e atualizador do modpack CSMP")]
[assembly: AssemblyCompany("CSMP")]
[assembly: AssemblyProduct("CSMP")]
[assembly: AssemblyCopyright("CSMP")]
[assembly: AssemblyVersion("5.0.0.0")]
[assembly: AssemblyFileVersion("5.0.0.0")]

namespace Csmp
{
    public static class Programa
    {
        /// <summary>Sobe junto com Construcao/Instalador/versao.txt quando este programa muda.</summary>
        public const int VERSAO = 5;
        public const string REPO = "https://raw.githubusercontent.com/sou0/csmp-modpack/main/";
        public const string PACK = "Pronto-para-Minecraft/";
        public const string EXE_NO_REPO = "Instalador/CSMP.exe";

        [DllImport("user32.dll")]
        private static extern bool SetProcessDPIAware();

        [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
        private static extern bool DeleteFile(string caminho);

        /// <summary>
        /// Tira a marca "baixado da internet" (Zone.Identifier) de um arquivo que este programa instalou. O jogador ja
        /// aceitou rodar o CSMP.exe que baixou; sem isso, a copia em %AppData% repetiria o aviso do Windows a cada abertura.
        /// </summary>
        public static void TirarMarcaDeDownload(string arquivo)
        {
            try { DeleteFile(arquivo + ":Zone.Identifier"); } catch { }
        }

        [STAThread]
        public static void Main(string[] args)
        {
            try { SetProcessDPIAware(); } catch { }
            ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072; // TLS 1.2
            ServicePointManager.DefaultConnectionLimit = 8;
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            bool teste = Array.IndexOf(args, "--teste") >= 0;
            bool reabrindo = Array.IndexOf(args, "--reaberto") >= 0;
            bool unico;
            using (Mutex trava = new Mutex(true, "CSMP-Instalador-sou0", out unico))
            {
                // ao se atualizar ou ao "Tentar de novo", a janela anterior ainda esta fechando: espera ela soltar
                if (!unico && !teste && reabrindo)
                {
                    try { unico = trava.WaitOne(15000); } catch (AbandonedMutexException) { unico = true; }
                }
                if (!unico && !teste)
                {
                    MessageBox.Show("O CSMP já está aberto.", "CSMP", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    return;
                }
                try
                {
                    Application.Run(new Janela(args));
                }
                finally
                {
                    if (unico) { try { trava.ReleaseMutex(); } catch { } }
                }
            }
        }
    }

    public class Entrada
    {
        public string Caminho;
        public long Tamanho;
        public string Hash;
    }

    public class Estado
    {
        public string Hash;
        public long Tamanho;
        public long Data;
    }

    /// <summary>Cabecalho com ceu, nuvens em pixel e a logo do CSMP desenhada pixel a pixel.</summary>
    public class Ceu : Panel
    {
        public static readonly string[] LOGO = {
            "..BBBBBBBBBBBB..",
            ".BbbbbbbbbbbbbB.",
            "BbbbkkkkkkkkbbbB",
            "BbbkCCCCCCCckbbB",
            "BbkCccccccccckbB",
            "BbkCccssssssskbB",
            "BlkCcskkkkkkkllB",
            "BlkCcskllllllllB",
            "BlkCcskllllllllB",
            "BlkCcskkkkkkkllB",
            "BwkCccCCCCCCckwB",
            "BwkcccccccccskwB",
            "BwwkcssssssskwwB",
            "BwwwkkkkkkkkwwwB",
            ".BwwwwwwwwwwwwB.",
            "..BBBBBBBBBBBB..",
        };

        public static Color CorDaLogo(char c)
        {
            switch (c)
            {
                case 'B': return Color.FromArgb(70, 112, 214);
                case 'b': return Color.FromArgb(121, 162, 253);
                case 'l': return Color.FromArgb(142, 204, 255);
                case 'w': return Color.FromArgb(236, 246, 255);
                case 'k': return Color.FromArgb(22, 24, 30);
                case 'c': return Color.FromArgb(174, 236, 255);
                case 'C': return Color.FromArgb(214, 255, 255);
                case 's': return Color.FromArgb(120, 186, 230);
                default: return Color.Transparent;
            }
        }

        public static void DesenharLogo(Graphics g, int x, int y, int px)
        {
            for (int l = 0; l < LOGO.Length; l++)
            {
                for (int c = 0; c < LOGO[l].Length; c++)
                {
                    if (LOGO[l][c] == '.') continue;
                    using (SolidBrush b = new SolidBrush(CorDaLogo(LOGO[l][c]))) g.FillRectangle(b, x + c * px, y + l * px, px, px);
                }
            }
        }

        // nuvens em blocos: {x, y, largura, altura} em blocos de 6 px (a 100% de escala)
        private static readonly int[][] NUVENS_FUNDO = {
            new int[] { 0, 13, 107, 3 }, new int[] { 2, 12, 9, 1 }, new int[] { 4, 11, 5, 1 }, new int[] { 16, 12, 12, 1 },
            new int[] { 19, 11, 6, 1 }, new int[] { 21, 10, 2, 1 }, new int[] { 36, 12, 8, 1 }, new int[] { 38, 11, 4, 1 },
            new int[] { 52, 12, 14, 1 }, new int[] { 55, 11, 8, 1 }, new int[] { 58, 10, 3, 1 }, new int[] { 74, 12, 10, 1 },
            new int[] { 76, 11, 5, 1 }, new int[] { 90, 12, 13, 1 }, new int[] { 93, 11, 7, 1 }, new int[] { 95, 10, 3, 1 },
        };
        private static readonly int[][] NUVENS_SOLTAS = {
            new int[] { 62, 2, 8, 1 }, new int[] { 64, 1, 4, 1 }, new int[] { 84, 4, 10, 1 }, new int[] { 86, 3, 5, 1 },
            new int[] { 97, 1, 6, 1 }, new int[] { 99, 0, 3, 1 }, new int[] { 46, 5, 5, 1 },
        };

        public Ceu()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer | ControlStyles.UserPaint, true);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            float escala = g.DpiX / 96F;
            int u = Math.Max(1, (int)Math.Round(6 * escala));
            using (System.Drawing.Drawing2D.LinearGradientBrush ceu = new System.Drawing.Drawing2D.LinearGradientBrush(
                    ClientRectangle, Color.FromArgb(108, 150, 246), Color.FromArgb(170, 218, 255), 90F))
            {
                g.FillRectangle(ceu, ClientRectangle);
            }
            using (SolidBrush branca = new SolidBrush(Color.FromArgb(150, 255, 255, 255)))
            {
                foreach (int[] n in NUVENS_SOLTAS) g.FillRectangle(branca, n[0] * u, n[1] * u + u / 2, n[2] * u, n[3] * u);
            }
            int baseY = Height - 16 * u;
            using (SolidBrush sombra = new SolidBrush(Color.FromArgb(214, 232, 250)))
            using (SolidBrush branca = new SolidBrush(Color.White))
            {
                foreach (int[] n in NUVENS_FUNDO) g.FillRectangle(sombra, n[0] * u, baseY + n[1] * u + u / 2, n[2] * u, n[3] * u);
                foreach (int[] n in NUVENS_FUNDO) g.FillRectangle(branca, n[0] * u, baseY + n[1] * u + u, n[2] * u, n[3] * u);
            }
            int px = Math.Max(1, (int)Math.Round(3 * escala));
            int lx = (int)(22 * escala);
            int ly = (int)(12 * escala);
            DesenharLogo(g, lx, ly, px);
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.AntiAliasGridFit;
            using (Font titulo = new Font("Segoe UI Black", 18F))
            using (Font sub = new Font("Segoe UI Semibold", 9.5F))
            using (SolidBrush sombraTexto = new SolidBrush(Color.FromArgb(90, 30, 60, 130)))
            using (SolidBrush branco = new SolidBrush(Color.White))
            {
                float tx = lx + 16 * px + 14 * escala;
                g.DrawString("CSMP", titulo, sombraTexto, tx + 2 * escala, ly - 2 * escala);
                g.DrawString("CSMP", titulo, branco, tx, ly - 4 * escala);
                g.DrawString("Instalador e atualizador do modpack", sub, branco, tx + 2 * escala, ly + 28 * escala);
            }
        }
    }

    public class Janela : Form
    {
        // cores
        private static readonly Color Destaque = Color.FromArgb(70, 112, 214);
        private static readonly Color DestaqueClaro = Color.FromArgb(101, 144, 240);
        private static readonly Color Erro = Color.FromArgb(196, 55, 48);
        private static readonly Color Texto = Color.FromArgb(28, 38, 54);
        private static readonly Color TextoFraco = Color.FromArgb(96, 108, 126);
        private static readonly Color Rodape = Color.FromArgb(242, 247, 253);
        private static readonly Color Ok = Color.FromArgb(34, 139, 70);

        private readonly Font fonteTitulo = new Font("Segoe UI Semibold", 14F);
        private readonly Font fonteNormal = new Font("Segoe UI", 9.5F);
        private readonly Font fonteNegrito = new Font("Segoe UI Semibold", 9.5F);
        private readonly Font fontePequena = new Font("Segoe UI", 8.5F);
        private readonly Font fonteIcone = new Font("Segoe UI Symbol", 11F);

        // telas
        private readonly Panel telaBoasVindas = new Panel();
        private readonly Panel telaProgresso = new Panel();
        private readonly Panel telaFim = new Panel();

        // boas-vindas
        private readonly TextBox campoPasta = new TextBox();
        private readonly Label itemNeoForge = new Label();
        private readonly Label itemArquivos = new Label();
        private readonly Label itemAtalho = new Label();
        private readonly CheckBox moverDeFora = new CheckBox();
        private readonly CheckBox versoesSeparadas = new CheckBox();
        private readonly RadioButton radioOficial = new RadioButton();
        private readonly RadioButton radioAlternativo = new RadioButton();
        private readonly RadioButton radioInstancia = new RadioButton();
        /// <summary>"oficial", "alternativo" (TLauncher e parecidos) ou "instancia" (Prism, MultiMC, CurseForge...).</summary>
        private string tipo = "alternativo";
        private static string MINECRAFT = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
        private readonly Label avisoVersao = new Label();
        private bool mexendoNaOpcao;

        // progresso
        private readonly Label tituloProgresso = new Label();
        private readonly Label[] passoIcone = new Label[3];
        private readonly Label[] passoTexto = new Label[3];
        private readonly int[] situacaoPasso = new int[3];
        private readonly Label detalhe = new Label();
        private readonly ProgressBar barra = new ProgressBar();
        private readonly LinkLabel mostrarLog = new LinkLabel();
        private readonly TextBox log = new TextBox();

        // fim
        private readonly Label tituloFim = new Label();
        private readonly Label textoFim = new Label();
        private readonly CheckBox abrirSozinho = new CheckBox();

        // rodape
        private readonly Button botaoSecundario = new Button();
        private readonly Button botaoPrincipal = new Button();
        private readonly Label versaoRodape = new Label();

        private readonly string pastaApp = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "CSMP");
        private Dictionary<string, string> cfg = new Dictionary<string, string>();
        private string pastaJogo;
        private bool primeiraInstalacao;
        private bool deuCerto;
        /// <summary>--teste PASTA: instala nessa pasta sem perguntar, sem atalho e sem launcher, grava o log e fecha.</summary>
        private readonly string pastaTeste;
        private readonly string tipoTeste;

        // dados do manifesto
        private string versaoPack = "";
        private string versaoNeoForge = "";
        private List<Entrada> arquivos = new List<Entrada>();

        private EventHandler acaoPrincipal;
        private EventHandler acaoSecundaria;

        public Janela(string[] args)
        {
            for (int a = 0; a + 1 < args.Length; a++)
            {
                if (args[a] == "--teste") pastaTeste = args[a + 1];
                if (args[a] == "--tipo") tipoTeste = args[a + 1];          // so para testes
                if (args[a] == "--minecraft") MINECRAFT = args[a + 1];     // so para testes
            }
            Text = "CSMP";
            AutoScaleMode = AutoScaleMode.Dpi;
            AutoScaleDimensions = new SizeF(96F, 96F);
            ClientSize = new Size(640, 540);
            FormBorderStyle = FormBorderStyle.FixedSingle;
            MaximizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Color.White;
            ForeColor = Texto;
            Font = fonteNormal;
            try { Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath); } catch { }

            MontarCabecalho();
            MontarRodape();
            MontarBoasVindas();
            MontarProgresso();
            MontarFim();
            Shown += delegate { Comecar(); };
        }

        // ------------------------------------------------------------------ montagem da janela

        private void MontarCabecalho()
        {
            Ceu topo = new Ceu();
            topo.SetBounds(0, 0, 640, 92);
            Controls.Add(topo);
        }

        private void MontarRodape()
        {
            Panel rodape = new Panel();
            rodape.SetBounds(0, 476, 640, 64);
            rodape.BackColor = Rodape;
            Panel borda = new Panel();
            borda.SetBounds(0, 0, 640, 1);
            borda.BackColor = Color.FromArgb(220, 230, 242);
            versaoRodape.Text = "Versão " + Programa.VERSAO;
            versaoRodape.ForeColor = TextoFraco;
            versaoRodape.Font = fontePequena;
            versaoRodape.SetBounds(22, 24, 200, 18);
            Estilizar(botaoSecundario, false);
            botaoSecundario.SetBounds(372, 14, 120, 36);
            Estilizar(botaoPrincipal, true);
            botaoPrincipal.SetBounds(502, 14, 118, 36);
            botaoPrincipal.Click += delegate(object s, EventArgs e) { if (acaoPrincipal != null) acaoPrincipal(s, e); };
            botaoSecundario.Click += delegate(object s, EventArgs e) { if (acaoSecundaria != null) acaoSecundaria(s, e); };
            rodape.Controls.AddRange(new Control[] { borda, versaoRodape, botaoSecundario, botaoPrincipal });
            Controls.Add(rodape);
        }

        private void Estilizar(Button b, bool principal)
        {
            b.FlatStyle = FlatStyle.Flat;
            b.Font = principal ? fonteNegrito : fonteNormal;
            b.Cursor = Cursors.Hand;
            if (principal)
            {
                b.BackColor = Destaque;
                b.ForeColor = Color.White;
                b.FlatAppearance.BorderSize = 0;
                b.FlatAppearance.MouseOverBackColor = DestaqueClaro;
            }
            else
            {
                b.BackColor = Color.White;
                b.ForeColor = Texto;
                b.FlatAppearance.BorderColor = Color.FromArgb(190, 206, 228);
                b.FlatAppearance.MouseOverBackColor = Color.FromArgb(232, 241, 252);
            }
        }

        private Panel NovaTela(Panel p)
        {
            p.SetBounds(0, 92, 640, 384);
            p.BackColor = Color.White;
            p.Visible = false;
            Controls.Add(p);
            return p;
        }

        private Label Rotulo(Panel p, string texto, Font fonte, Color cor, int x, int y, int w, int h)
        {
            Label l = new Label();
            l.Text = texto;
            l.Font = fonte;
            l.ForeColor = cor;
            l.SetBounds(x, y, w, h);
            p.Controls.Add(l);
            return l;
        }

        private void MontarBoasVindas()
        {
            Panel p = NovaTela(telaBoasVindas);
            Rotulo(p, "Bem-vindo ao CSMP", fonteTitulo, Texto, 26, 14, 580, 30);
            Rotulo(p, "Este programa prepara o seu Minecraft para jogar no servidor do CSMP e o mantém atualizado. " +
                    "Nada é alterado antes de você clicar em Instalar.", fonteNormal, TextoFraco, 28, 46, 584, 38);

            Rotulo(p, "Qual launcher você usa?", fonteNegrito, Texto, 28, 86, 400, 20);
            radioOficial.Text = "Launcher oficial (Mojang / Microsoft)";
            radioAlternativo.Text = "TLauncher ou outro alternativo que usa a pasta .minecraft (SKLauncher, Legacy…)";
            radioInstancia.Text = "Prism, MultiMC, CurseForge ou ATLauncher (cada pack numa instância)";
            int y = 108;
            foreach (RadioButton r in new RadioButton[] { radioOficial, radioAlternativo, radioInstancia })
            {
                r.SetBounds(30, y, 582, 22);
                r.CheckedChanged += delegate(object s, EventArgs e) { if (((RadioButton)s).Checked) AplicarTipo(); };
                p.Controls.Add(r);
                y += 22;
            }

            Rotulo(p, "Pasta do jogo", fonteNegrito, Texto, 28, 184, 300, 20);
            campoPasta.ReadOnly = true;
            campoPasta.BackColor = Color.White;
            campoPasta.SetBounds(28, 206, 470, 26);
            p.Controls.Add(campoPasta);
            Button alterar = new Button();
            Estilizar(alterar, false);
            alterar.Text = "Alterar…";
            alterar.SetBounds(506, 204, 106, 30);
            alterar.Click += delegate { AlterarPasta(); };
            p.Controls.Add(alterar);
            avisoVersao.Font = fontePequena;
            avisoVersao.ForeColor = TextoFraco;
            avisoVersao.SetBounds(28, 236, 584, 18);
            p.Controls.Add(avisoVersao);
            versoesSeparadas.Text = "Versões separadas (o TLauncher usa uma pasta para cada versão, dentro de versions)";
            versoesSeparadas.SetBounds(28, 256, 584, 22);
            versoesSeparadas.CheckedChanged += delegate { TrocarModoVersoes(); };
            p.Controls.Add(versoesSeparadas);

            Rotulo(p, "O que será feito", fonteNegrito, Texto, 28, 284, 300, 20);
            itemNeoForge.SetBounds(40, 304, 572, 18);
            itemArquivos.SetBounds(40, 322, 572, 18);
            itemAtalho.SetBounds(40, 340, 572, 18);
            itemAtalho.AutoEllipsis = true;
            p.Controls.Add(itemNeoForge);
            p.Controls.Add(itemArquivos);
            p.Controls.Add(itemAtalho);

            moverDeFora.SetBounds(28, 360, 590, 22);
            moverDeFora.Checked = true;
            p.Controls.Add(moverDeFora);
        }

        private void MontarProgresso()
        {
            Panel p = NovaTela(telaProgresso);
            tituloProgresso.Font = fonteTitulo;
            tituloProgresso.SetBounds(26, 18, 580, 30);
            p.Controls.Add(tituloProgresso);
            string[] nomes = { "Procurando atualizações", "Minecraft 1.21.1 e NeoForge", "Mods, configurações e resourcepacks" };
            for (int i = 0; i < 3; i++)
            {
                passoIcone[i] = Rotulo(p, "○", fonteIcone, TextoFraco, 30, 62 + i * 30, 26, 26);
                passoTexto[i] = Rotulo(p, nomes[i], fonteNormal, TextoFraco, 58, 65 + i * 30, 540, 22);
            }
            detalhe.ForeColor = TextoFraco;
            detalhe.AutoEllipsis = true;
            detalhe.SetBounds(28, 162, 584, 20);
            p.Controls.Add(detalhe);
            barra.Style = ProgressBarStyle.Continuous;
            barra.SetBounds(28, 186, 584, 14);
            p.Controls.Add(barra);
            mostrarLog.Text = "Mostrar detalhes";
            mostrarLog.LinkColor = Destaque;
            mostrarLog.ActiveLinkColor = DestaqueClaro;
            mostrarLog.SetBounds(28, 208, 200, 20);
            mostrarLog.LinkClicked += delegate
            {
                log.Visible = !log.Visible;
                mostrarLog.Text = log.Visible ? "Esconder detalhes" : "Mostrar detalhes";
            };
            p.Controls.Add(mostrarLog);
            log.Multiline = true;
            log.ReadOnly = true;
            log.ScrollBars = ScrollBars.Vertical;
            log.BackColor = Color.FromArgb(248, 248, 248);
            log.Font = new Font("Consolas", 8.5F);
            log.SetBounds(28, 230, 584, 80);
            log.Visible = false;
            p.Controls.Add(log);
        }

        private void MontarFim()
        {
            Panel p = NovaTela(telaFim);
            tituloFim.Font = fonteTitulo;
            tituloFim.SetBounds(26, 18, 580, 30);
            p.Controls.Add(tituloFim);
            textoFim.SetBounds(28, 56, 584, 150);
            p.Controls.Add(textoFim);
            abrirSozinho.Text = "Abrir o launcher automaticamente nas próximas vezes";
            abrirSozinho.SetBounds(28, 270, 584, 24);
            abrirSozinho.CheckedChanged += delegate { cfg["abrir"] = abrirSozinho.Checked ? "sim" : "nao"; SalvarConfig(); };
            p.Controls.Add(abrirSozinho);
        }

        private void Mostrar(Panel tela)
        {
            telaBoasVindas.Visible = tela == telaBoasVindas;
            telaProgresso.Visible = tela == telaProgresso;
            telaFim.Visible = tela == telaFim;
        }

        private void Botoes(string principal, EventHandler acao1, string secundario, EventHandler acao2)
        {
            botaoPrincipal.Text = principal ?? "";
            botaoPrincipal.Visible = principal != null;
            botaoPrincipal.Enabled = true;
            acaoPrincipal = acao1;
            botaoSecundario.Text = secundario ?? "";
            botaoSecundario.Visible = secundario != null;
            acaoSecundaria = acao2;
        }

        private void Passo(int i, int situacao)
        {
            // 0 = esperando, 1 = fazendo, 2 = feito, 3 = erro
            situacaoPasso[i] = situacao;
            Ui(delegate
            {
                passoIcone[i].Text = situacao == 2 ? "✔" : situacao == 3 ? "✖" : situacao == 1 ? "●" : "○";
                passoIcone[i].ForeColor = situacao == 2 ? Ok : situacao == 3 ? Erro : situacao == 1 ? Destaque : TextoFraco;
                passoTexto[i].ForeColor = situacao == 0 ? TextoFraco : Texto;
                passoTexto[i].Font = situacao == 1 ? fonteNegrito : fonteNormal;
            });
        }

        // ------------------------------------------------------------------ fluxo

        private void Comecar()
        {
            Directory.CreateDirectory(pastaApp);
            LerConfig();
            LimparVersaoVelha();
            abrirSozinho.Checked = Cfg("abrir", "sim") == "sim";
            tipo = pastaTeste != null ? (tipoTeste ?? "alternativo") : Cfg("tipo", "alternativo");
            pastaJogo = pastaTeste ?? Cfg("pasta", null);
            // config de versao antiga (sem a escolha do launcher) tambem volta para a tela de boas-vindas
            primeiraInstalacao = pastaTeste == null && (pastaJogo == null || !Directory.Exists(pastaJogo) || Cfg("tipo", null) == null);

            tituloProgresso.Text = primeiraInstalacao ? "Preparando a instalação" : "Atualizando o CSMP";
            Mostrar(telaProgresso);
            Botoes(null, null, "Cancelar", delegate { Close(); });
            Passo(0, 1);
            Detalhe("Conferindo a versão mais recente no GitHub…");
            new Thread(BuscarManifesto) { IsBackground = true }.Start();
        }

        private void BuscarManifesto()
        {
            try
            {
                Dictionary<string, object> m = BaixarManifesto();
                versaoPack = Convert.ToString(m["versao"]);
                versaoNeoForge = Convert.ToString(m["neoforge"]);
                arquivos = new List<Entrada>();
                foreach (object o in (ArrayList)m["arquivos"])
                {
                    Dictionary<string, object> d = (Dictionary<string, object>)o;
                    Entrada e = new Entrada();
                    e.Caminho = (string)d["c"];
                    e.Tamanho = Convert.ToInt64(d["t"]);
                    e.Hash = (string)d["h"];
                    arquivos.Add(e);
                }
                Log("Pack no GitHub: " + versaoPack + " (" + arquivos.Count + " arquivos)");
                if (Convert.ToInt32(m["instalador"]) > Programa.VERSAO && pastaTeste == null && AtualizarEsteExe())
                {
                    return;
                }
            }
            catch (Exception e)
            {
                Log("Sem resposta do GitHub: " + e.Message);
                Passo(0, 3);
                bool instalado = !primeiraInstalacao && File.Exists(Path.Combine(pastaJogo, "csmp-estado.txt"));
                Concluir(instalado, instalado ? "Sem conexão" : "Sem conexão com a internet",
                        instalado ? "Não foi possível procurar atualizações agora. Você pode jogar com a versão que já está instalada."
                                  : "É preciso internet para instalar o CSMP. Confira a conexão e abra o programa de novo.");
                return;
            }
            Passo(0, 2);
            if (primeiraInstalacao)
            {
                Ui(MostrarBoasVindas);
            }
            else
            {
                Instalar();
            }
        }

        private void MostrarBoasVindas()
        {
            Mostrar(telaBoasVindas);
            Botoes("Instalar", delegate { ConfirmarInstalacao(); }, "Cancelar", delegate { Close(); });
            // sugere o launcher que achar no computador; o jogador confirma ou troca
            if (File.Exists(Path.Combine(MINECRAFT, "TLauncher.exe")) || Directory.Exists(Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".tlauncher")))
            {
                radioAlternativo.Checked = true;
            }
            else if (AcharLauncherOficial() != null)
            {
                radioOficial.Checked = true;
            }
            else if (AcharLauncherDeInstancias() != null)
            {
                radioInstancia.Checked = true;
            }
            else
            {
                radioAlternativo.Checked = true;
            }
            AplicarTipo();
        }

        /// <summary>Escolheu o tipo de launcher: ajusta a pasta sugerida e o que aparece na tela.</summary>
        private void AplicarTipo()
        {
            tipo = radioOficial.Checked ? "oficial" : radioInstancia.Checked ? "instancia" : "alternativo";
            versoesSeparadas.Visible = tipo == "alternativo";
            if (tipo == "oficial")
            {
                pastaJogo = Path.Combine(MINECRAFT, "csmp");
            }
            else if (tipo == "alternativo")
            {
                pastaJogo = Directory.Exists(MINECRAFT) ? MINECRAFT : null;
                mexendoNaOpcao = true;
                versoesSeparadas.Checked = false;
                mexendoNaOpcao = false;
            }
            else
            {
                pastaJogo = null; // cada instancia tem a sua: o jogador escolhe
            }
            AtualizarResumo();
        }

        private void AtualizarResumo()
        {
            campoPasta.Text = pastaJogo ?? (tipo == "instancia" ? "(clique em Alterar e escolha a pasta da instância)" : "(escolha a pasta)");
            botaoPrincipal.Enabled = pastaJogo != null;
            long total = 0;
            int mods = 0;
            HashSet<string> doPack = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (Entrada e in arquivos)
            {
                total += e.Tamanho;
                doPack.Add(e.Caminho);
                if (e.Caminho.StartsWith("mods/") && e.Caminho.EndsWith(".jar")) mods++;
            }
            bool temNeo = pastaJogo != null && NeoForgeInstalado(versaoNeoForge);
            string propria = pastaJogo == null ? null : VersaoPropriaCompativel();
            itemNeoForge.Text = tipo == "instancia"
                    ? "•  NeoForge " + versaoNeoForge + ": você escolhe na instância (o instalador mostra como no final)"
                    : "•  Minecraft 1.21.1 com NeoForge " + versaoNeoForge +
                      (propria != null ? "  (já instalado na versão \"" + propria + "\")" : temNeo ? "  (já instalado)" : "");
            itemAtalho.Text = tipo == "oficial"
                    ? "•  Instalação \"CSMP\" no launcher oficial (pasta própria, 6 GB de memória) e atalho CSMP"
                    : "•  Atalho \"CSMP\" na área de trabalho, que atualiza o pack e abre o jogo";
            mexendoNaOpcao = true;
            if (tipo == "alternativo") versoesSeparadas.Checked = pastaJogo != null && EhPastaDeVersao(pastaJogo);
            mexendoNaOpcao = false;
            if (tipo == "oficial")
                avisoVersao.Text = "Uma pasta só do CSMP, para não misturar com os seus outros mundos e mods.";
            else if (tipo == "instancia")
                avisoVersao.Text = "Crie antes uma instância Minecraft 1.21.1 no seu launcher e escolha a pasta dela aqui.";
            else if (pastaJogo != null && EhPastaDeVersao(pastaJogo))
                avisoVersao.Text = "Os mods vão para a pasta desta versão. No launcher, abra a versão \"" + Path.GetFileName(pastaJogo) + "\".";
            else
                avisoVersao.Text = "É a pasta onde ficam saves, options.txt e a pasta mods (normalmente .minecraft).";
            itemArquivos.Text = "•  " + mods + " mods, configurações e resourcepacks (" + Mb(total) + " para baixar)";
            int fora = pastaJogo == null ? 0 : ModsDeFora(doPack).Count;
            moverDeFora.Visible = fora > 0;
            moverDeFora.Text = "Mover para \"mods-fora-do-csmp\" os " + fora + " mod(s) que não são do CSMP (nada é apagado)";
        }

        private void AlterarPasta()
        {
            using (FolderBrowserDialog f = new FolderBrowserDialog())
            {
                f.Description = "Escolha a pasta do jogo (a que tem saves, options.txt e a pasta mods)";
                if (pastaJogo != null) f.SelectedPath = pastaJogo;
                if (f.ShowDialog(this) != DialogResult.OK) return;
                string p = f.SelectedPath;
                if (string.Equals(Path.GetFileName(p), "mods", StringComparison.OrdinalIgnoreCase))
                {
                    p = Path.GetDirectoryName(p); // escolheu a propria pasta mods: o jogo e a de cima
                }
                if (tipo == "instancia")
                {
                    foreach (string sub in new string[] { "minecraft", ".minecraft" })
                    {
                        if (Directory.Exists(Path.Combine(p, sub))) { p = Path.Combine(p, sub); break; }
                    }
                }
                pastaJogo = p;
                if (tipo == "alternativo" && EhPastaDeVersao(p) && VersaoPropriaCompativel() == null && !NomeDaVersaoNossa(p))
                {
                    string tem = NeoForgeDaVersao(p);
                    DialogResult r = MessageBox.Show(this, "A versão \"" + Path.GetFileName(p) + "\" " +
                            (tem == null ? "não usa NeoForge" : "usa o NeoForge " + tem) + ", e o CSMP precisa do NeoForge " +
                            versaoNeoForge + " ou mais novo.\n\nPosso instalar a versão \"neoforge-" + versaoNeoForge +
                            "\" e colocar os mods na pasta dela?", "CSMP", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                    if (r == DialogResult.Yes) pastaJogo = PastaDaNossaVersao(RaizDoLauncher(p));
                }
                AtualizarResumo();
            }
        }

        private void TrocarModoVersoes()
        {
            if (mexendoNaOpcao || pastaJogo == null) return;
            string raiz = RaizDoLauncher(pastaJogo);
            pastaJogo = versoesSeparadas.Checked ? PastaDaNossaVersao(raiz) : raiz;
            AtualizarResumo();
        }

        // ------------------------------------------------------------------ pasta do launcher x pasta do jogo
        // Com "versoes separadas" (TLauncher), cada versao usa .minecraft\versions\<nome> como pasta do jogo (mods, config),
        // mas o NeoForge, as bibliotecas e as versoes continuam na raiz (.minecraft).

        private static bool EhPastaDeVersao(string pasta)
        {
            string pai = Path.GetDirectoryName(pasta.TrimEnd('\\', '/'));
            return pai != null && string.Equals(Path.GetFileName(pai), "versions", StringComparison.OrdinalIgnoreCase);
        }

        private static string RaizDoLauncher(string pasta)
        {
            if (!EhPastaDeVersao(pasta)) return pasta;
            return Path.GetDirectoryName(Path.GetDirectoryName(pasta.TrimEnd('\\', '/')));
        }

        /// <summary>Onde ficam versions/libraries (onde o NeoForge e instalado).</summary>
        private string RaizAtual()
        {
            if (tipo == "oficial") return MINECRAFT;
            return RaizDoLauncher(pastaJogo);
        }

        private string PastaDaNossaVersao(string raiz)
        {
            return Path.Combine(raiz, "versions", "neoforge-" + versaoNeoForge);
        }

        private bool NomeDaVersaoNossa(string pasta)
        {
            return string.Equals(Path.GetFileName(pasta.TrimEnd('\\', '/')), "neoforge-" + versaoNeoForge, StringComparison.OrdinalIgnoreCase);
        }

        /// <summary>Versao do NeoForge que o json da pasta de versao usa (ou null).</summary>
        private static string NeoForgeDaVersao(string pasta)
        {
            try
            {
                string nome = Path.GetFileName(pasta.TrimEnd('\\', '/'));
                string json = Path.Combine(pasta, nome + ".json");
                if (!File.Exists(json))
                {
                    string[] outros = Directory.GetFiles(pasta, "*.json");
                    if (outros.Length == 0) return null;
                    json = outros[0];
                }
                string texto = File.ReadAllText(json);
                int i = texto.IndexOf("fml.neoForgeVersion", StringComparison.Ordinal);
                if (i >= 0)
                {
                    Match m = Regex.Match(texto.Substring(i, Math.Min(300, texto.Length - i)), "(\\d+\\.\\d+\\.\\d+)");
                    if (m.Success) return m.Groups[1].Value;
                }
                Match n = Regex.Match(texto, "neoforge-(\\d+\\.\\d+\\.\\d+)");
                return n.Success ? n.Groups[1].Value : null;
            }
            catch { return null; }
        }

        /// <summary>Se a pasta escolhida e de uma versao com NeoForge compativel, o nome dela; senao null.</summary>
        private string VersaoPropriaCompativel()
        {
            if (pastaJogo == null || !EhPastaDeVersao(pastaJogo) || NomeDaVersaoNossa(pastaJogo)) return null;
            string tem = NeoForgeDaVersao(pastaJogo);
            return tem != null && Compativel(tem, versaoNeoForge) ? Path.GetFileName(pastaJogo.TrimEnd('\\', '/')) : null;
        }

        /// <summary>Mesmo 21.1.x e patch igual ou maior.</summary>
        private static bool Compativel(string tem, string precisa)
        {
            string[] a = tem.Split('.');
            string[] b = precisa.Split('.');
            if (a.Length < 3 || b.Length < 3 || a[0] != b[0] || a[1] != b[1]) return false;
            int x, y;
            return int.TryParse(a[2], out x) && int.TryParse(b[2], out y) && x >= y;
        }

        private void ConfirmarInstalacao()
        {
            if (pastaJogo == null) return;
            Directory.CreateDirectory(pastaJogo);
            if (Cfg("tipo", "") != tipo) cfg.Remove("launcher"); // trocou de launcher: procura o exe de novo
            cfg["tipo"] = tipo;
            cfg["pasta"] = pastaJogo;
            SalvarConfig();
            InstalarNoComputador();
            tituloProgresso.Text = "Instalando o CSMP";
            Mostrar(telaProgresso);
            Botoes(null, null, "Cancelar", delegate { Close(); });
            bool mover = moverDeFora.Visible && moverDeFora.Checked;
            new Thread(delegate() { Instalar(mover); }) { IsBackground = true }.Start();
        }

        private string VersaoParaAbrir()
        {
            string propria = tipo == "alternativo" ? VersaoPropriaCompativel() : null;
            return propria ?? "neoforge-" + versaoNeoForge;
        }

        private void Instalar()
        {
            Instalar(false);
        }

        /// <summary>
        /// Cria (ou atualiza) a instalacao "CSMP" no launcher oficial: versao do NeoForge, pasta propria e 6 GB de memoria.
        /// Mexe so nesse perfil; guarda uma copia do arquivo original na primeira vez.
        /// </summary>
        private void CriarPerfilOficial()
        {
            string[] arquivosDePerfil = { "launcher_profiles.json", "launcher_profiles_microsoft_store.json" };
            bool algum = false;
            foreach (string nomeArq in arquivosDePerfil)
            {
                string arq = Path.Combine(MINECRAFT, nomeArq);
                if (!File.Exists(arq) && (nomeArq != "launcher_profiles.json" || algum)) continue;
                try
                {
                    JavaScriptSerializer js = new JavaScriptSerializer();
                    js.MaxJsonLength = int.MaxValue;
                    Dictionary<string, object> raiz = File.Exists(arq)
                            ? js.Deserialize<Dictionary<string, object>>(File.ReadAllText(arq, Encoding.UTF8))
                            : new Dictionary<string, object>();
                    if (raiz == null) raiz = new Dictionary<string, object>();
                    if (File.Exists(arq) && !File.Exists(arq + ".antes-do-csmp")) File.Copy(arq, arq + ".antes-do-csmp");
                    object o;
                    Dictionary<string, object> perfis = raiz.TryGetValue("profiles", out o) ? o as Dictionary<string, object> : null;
                    if (perfis == null) { perfis = new Dictionary<string, object>(); raiz["profiles"] = perfis; }
                    Dictionary<string, object> perfil = perfis.TryGetValue("csmp", out o) ? o as Dictionary<string, object> : null;
                    string agora = DateTime.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'");
                    if (perfil == null)
                    {
                        perfil = new Dictionary<string, object>();
                        perfil["created"] = agora;
                        perfil["javaArgs"] = "-Xmx6G -Xms2G -XX:+UseG1GC";
                        perfis["csmp"] = perfil;
                    }
                    perfil["name"] = "CSMP";
                    perfil["type"] = "custom";
                    perfil["lastVersionId"] = "neoforge-" + versaoNeoForge;
                    perfil["gameDir"] = pastaJogo;
                    perfil["lastUsed"] = agora;
                    perfil["icon"] = IconeDoPerfil();
                    if (!raiz.ContainsKey("version")) raiz["version"] = 3;
                    File.WriteAllText(arq, js.Serialize(raiz), new UTF8Encoding(false));
                    algum = true;
                    Log("Instalação \"CSMP\" criada no launcher oficial (" + nomeArq + ").");
                }
                catch (Exception e)
                {
                    Log("Aviso: não consegui criar a instalação no launcher oficial (" + e.Message + ").");
                }
            }
        }

        /// <summary>A logo em PNG 128x128 como data URL, que o launcher oficial aceita como icone do perfil.</summary>
        private static string IconeDoPerfil()
        {
            using (Bitmap b = new Bitmap(128, 128))
            {
                using (Graphics g = Graphics.FromImage(b)) Ceu.DesenharLogo(g, 0, 0, 8);
                using (MemoryStream ms = new MemoryStream())
                {
                    b.Save(ms, System.Drawing.Imaging.ImageFormat.Png);
                    return "data:image/png;base64," + Convert.ToBase64String(ms.ToArray());
                }
            }
        }

        private void Instalar(bool moverMods)
        {
            Log("Pasta do jogo: " + pastaJogo);
            try
            {
                Passo(1, 1);
                if (!InstalarNeoForge(versaoNeoForge))
                {
                    Passo(1, 3);
                    return;
                }
                if (tipo == "oficial") CriarPerfilOficial();
                Passo(1, 2);
                Passo(2, 1);
                Sincronizar(moverMods);
                Passo(2, 2);
                if (pastaTeste == null)
                {
                    cfg["versao"] = versaoPack;
                    cfg["neoforge"] = versaoNeoForge;
                    SalvarConfig();
                }
                Concluir(true, "Tudo pronto", null);
            }
            catch (Exception e)
            {
                Log("ERRO: " + e);
                for (int i = 1; i < 3; i++) { if (situacaoPasso[i] == 1) Passo(i, 3); }
                Concluir(File.Exists(Path.Combine(pastaJogo, "csmp-estado.txt")) && !primeiraInstalacao, "Não foi possível concluir",
                        "Algo deu errado: " + e.Message + "\n\nTente de novo. Se continuar, mande um print dos detalhes para o pessoal do CSMP.");
            }
        }

        private string ComoJogar()
        {
            if (tipo == "oficial")
                return "No launcher oficial, escolha a instalação \"CSMP\" ao lado do botão Jogar. Ela já usa o NeoForge " +
                        versaoNeoForge + ", a pasta própria do CSMP e 6 GB de memória.";
            if (tipo == "instancia")
                return "Na instância, coloque o NeoForge " + versaoNeoForge + " (Editar > Versão > Instalar NeoForge, no Prism e no MultiMC) " +
                        "e reserve pelo menos 6 GB de memória nas configurações dela.";
            return "No launcher, escolha a versão \"" + VersaoParaAbrir() + "\" e reserve pelo menos 6 GB de memória nas configurações dele.";
        }

        private void Concluir(bool podeJogar, string titulo, string texto)
        {
            deuCerto = podeJogar;
            Ui(delegate
            {
                if (pastaTeste != null)
                {
                    File.WriteAllText(Path.Combine(pastaTeste, "csmp-teste-log.txt"), titulo + Environment.NewLine + (texto ?? "") +
                            Environment.NewLine + log.Text);
                    Close();
                    return;
                }
                if (podeJogar && texto == null && !primeiraInstalacao && abrirSozinho.Checked)
                {
                    AbrirLauncher(); // atualizacao normal: abre o jogo direto
                    return;
                }
                tituloFim.Text = titulo;
                tituloFim.ForeColor = podeJogar ? Texto : Erro;
                textoFim.Text = texto ?? ("O CSMP está instalado e atualizado (pack de " + versaoPack + ").\n\n" + ComoJogar() + "\n\n" +
                        "Da próxima vez, é só abrir o atalho CSMP na área de trabalho: ele confere se tem atualização e abre o launcher.");
                abrirSozinho.Visible = podeJogar;
                Mostrar(telaFim);
                if (podeJogar)
                {
                    Botoes("Jogar", delegate { AbrirLauncher(); }, "Abrir pasta", delegate { Process.Start("explorer.exe", "\"" + pastaJogo + "\""); });
                }
                else
                {
                    Botoes("Tentar de novo", delegate { Process.Start(Application.ExecutablePath, "--reaberto"); Close(); }, "Detalhes", delegate
                    {
                        Mostrar(telaProgresso);
                        log.Visible = true;
                        mostrarLog.Text = "Esconder detalhes";
                        Botoes("Fechar", delegate { Close(); }, null, null);
                    });
                }
            });
        }

        // ------------------------------------------------------------------ manifesto e auto-atualizacao

        private Dictionary<string, object> BaixarManifesto()
        {
            using (WebClient w = Cliente())
            {
                w.Encoding = Encoding.UTF8;
                string json = w.DownloadString(Programa.REPO + Programa.PACK + "csmp-manifesto.json?t=" + DateTime.UtcNow.Ticks);
                JavaScriptSerializer js = new JavaScriptSerializer();
                js.MaxJsonLength = int.MaxValue;
                return js.Deserialize<Dictionary<string, object>>(json);
            }
        }

        /// <summary>Baixa o CSMP.exe novo, troca este pelo novo e reabre. True se reabriu.</summary>
        private bool AtualizarEsteExe()
        {
            try
            {
                Detalhe("Atualizando o próprio instalador…");
                string atual = Application.ExecutablePath;
                string novo = atual + ".novo";
                using (WebClient w = Cliente())
                {
                    w.DownloadFile(Programa.REPO + Programa.EXE_NO_REPO + "?t=" + DateTime.UtcNow.Ticks, novo);
                }
                string velho = atual + ".velho";
                if (File.Exists(velho)) File.Delete(velho);
                File.Move(atual, velho);   // o Windows deixa renomear o exe que esta rodando
                File.Move(novo, atual);
                Process.Start(atual, "--reaberto");
                Ui(delegate { Close(); });
                return true;
            }
            catch (Exception e)
            {
                Log("Não consegui atualizar o instalador (" + e.Message + "), seguindo com este.");
                return false;
            }
        }

        private void LimparVersaoVelha()
        {
            try { File.Delete(Application.ExecutablePath + ".velho"); } catch { }
        }

        /// <summary>Copia o exe para %AppData%\CSMP e cria o atalho "CSMP" na area de trabalho.</summary>
        private void InstalarNoComputador()
        {
            string destino = Path.Combine(pastaApp, "CSMP.exe");
            string atual = Application.ExecutablePath;
            try
            {
                if (!string.Equals(Path.GetFullPath(atual), Path.GetFullPath(destino), StringComparison.OrdinalIgnoreCase))
                {
                    File.Copy(atual, destino, true);
                }
                Programa.TirarMarcaDeDownload(destino);
                string atalho = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "CSMP.lnk");
                object shell = Activator.CreateInstance(Type.GetTypeFromProgID("WScript.Shell"));
                object lnk = shell.GetType().InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { atalho });
                lnk.GetType().InvokeMember("TargetPath", BindingFlags.SetProperty, null, lnk, new object[] { destino });
                lnk.GetType().InvokeMember("WorkingDirectory", BindingFlags.SetProperty, null, lnk, new object[] { pastaApp });
                lnk.GetType().InvokeMember("Description", BindingFlags.SetProperty, null, lnk, new object[] { "Atualiza e abre o CSMP" });
                lnk.GetType().InvokeMember("Save", BindingFlags.InvokeMethod, null, lnk, null);
                Log("Atalho CSMP criado na área de trabalho.");
            }
            catch (Exception e)
            {
                Log("Aviso: não consegui criar o atalho (" + e.Message + ").");
            }
        }

        // ------------------------------------------------------------------ NeoForge

        private bool NeoForgeInstalado(string versao)
        {
            string nome = "neoforge-" + versao;
            return File.Exists(Path.Combine(RaizAtual(), "versions", nome, nome + ".json"));
        }

        private bool InstalarNeoForge(string versao)
        {
            string nome = "neoforge-" + versao;
            if (tipo == "instancia")
            {
                Log("Instância: o NeoForge é escolhido no próprio launcher.");
                return true;
            }
            if (NeoForgeInstalado(versao))
            {
                Log("NeoForge " + versao + " já instalado.");
                return true;
            }
            string propria = tipo == "alternativo" ? VersaoPropriaCompativel() : null;
            if (propria != null)
            {
                Log("Usando a versão \"" + propria + "\", que já tem um NeoForge compatível.");
                return true;
            }
            string raiz = RaizAtual();
            Detalhe("Procurando o Java…");
            string java = AcharJava();
            if (java == null)
            {
                Concluir(false, "Falta o Java 21",
                        "Para instalar o NeoForge é preciso o Java 21. Ele vem junto com o launcher oficial e com o TLauncher: " +
                        "abra o Minecraft 1.21.1 uma vez por eles e depois abra o CSMP de novo.\n\n" +
                        "Ou instale o Java 21 (Temurin) em adoptium.net e tente de novo.");
                return false;
            }
            Log("Java: " + java);
            string perfis = Path.Combine(raiz, "launcher_profiles.json");
            if (!File.Exists(perfis))
            {
                Directory.CreateDirectory(raiz);
                File.WriteAllText(perfis, "{\n  \"profiles\": {},\n  \"version\": 3\n}\n");
            }
            string instalador = Path.Combine(Path.GetTempPath(), nome + "-installer.jar");
            string url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + versao + "/" + nome + "-installer.jar";
            Detalhe("Baixando o instalador oficial do NeoForge…");
            using (WebClient w = Cliente())
            {
                w.DownloadFile(url, instalador);
                string esperado = w.DownloadString(url + ".sha1").Trim().ToLowerInvariant();
                if (esperado.Length >= 40 && Sha1(instalador) != esperado.Substring(0, 40))
                {
                    throw new IOException("o instalador do NeoForge veio corrompido");
                }
            }
            Detalhe("Instalando o NeoForge (baixa o Minecraft 1.21.1 e as bibliotecas, pode levar alguns minutos)…");
            ProcessStartInfo psi = new ProcessStartInfo(java, "-jar \"" + instalador + "\" --install-client \"" + raiz + "\"");
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.RedirectStandardOutput = true;
            psi.RedirectStandardError = true;
            psi.WorkingDirectory = Path.GetTempPath();
            using (Process p = new Process())
            {
                p.StartInfo = psi;
                DataReceivedEventHandler saida = delegate(object s, DataReceivedEventArgs a)
                {
                    if (a.Data != null && a.Data.Trim().Length > 0) Log("  " + a.Data);
                };
                p.OutputDataReceived += saida;
                p.ErrorDataReceived += saida;
                Ui(delegate { barra.Style = ProgressBarStyle.Marquee; });
                p.Start();
                p.BeginOutputReadLine();
                p.BeginErrorReadLine();
                p.WaitForExit();
                Ui(delegate { barra.Style = ProgressBarStyle.Continuous; });
                if (p.ExitCode != 0 || !NeoForgeInstalado(versao))
                {
                    throw new IOException("o instalador do NeoForge falhou (código " + p.ExitCode + ")");
                }
            }
            try { File.Delete(instalador); } catch { }
            Log("NeoForge " + versao + " instalado.");
            return true;
        }

        private string AcharJava()
        {
            string ad = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string la = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            List<string> candidatos = new List<string>();
            string jh = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(jh)) candidatos.Add(Path.Combine(jh, "bin", "java.exe"));
            string[] lugares = {
                Path.Combine(RaizAtual(), "runtime"),
                Path.Combine(ad, ".minecraft", "runtime"),
                Path.Combine(la, "Packages", "Microsoft.4297127D64EC6_8wekyb3d8bbwe", "LocalCache", "Local", "runtime"),
                Path.Combine(ad, ".tlauncher"),
                @"C:\Program Files\Eclipse Adoptium",
                @"C:\Program Files\Java",
                @"C:\Program Files\Microsoft",
                @"C:\Program Files (x86)\Minecraft Launcher\runtime",
            };
            foreach (string r in lugares) Procurar(r, 0, candidatos);
            candidatos.Add("java");
            string melhor = null;
            int melhorVersao = 0;
            foreach (string c in candidatos)
            {
                if (c != "java" && !File.Exists(c)) continue;
                int v = VersaoDoJava(c);
                if (v >= 21) return c;
                if (v > melhorVersao) { melhor = c; melhorVersao = v; }
            }
            return melhorVersao >= 17 ? melhor : null;
        }

        private static void Procurar(string pasta, int fundo, List<string> achados)
        {
            if (fundo > 6 || !Directory.Exists(pasta)) return;
            try
            {
                string j = Path.Combine(pasta, "bin", "java.exe");
                if (File.Exists(j)) { achados.Add(j); return; }
                foreach (string d in Directory.GetDirectories(pasta)) Procurar(d, fundo + 1, achados);
            }
            catch { }
        }

        private static int VersaoDoJava(string java)
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(java, "-version");
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;
                psi.RedirectStandardError = true;
                psi.RedirectStandardOutput = true;
                using (Process p = Process.Start(psi))
                {
                    string texto = p.StandardError.ReadToEnd() + p.StandardOutput.ReadToEnd();
                    p.WaitForExit(10000);
                    int i = texto.IndexOf('"');
                    if (i < 0) return 0;
                    string v = texto.Substring(i + 1);
                    if (v.StartsWith("1.")) v = v.Substring(2);
                    int n = 0;
                    foreach (char ch in v) { if (!char.IsDigit(ch)) break; n = n * 10 + (ch - '0'); }
                    return n;
                }
            }
            catch { return 0; }
        }

        // ------------------------------------------------------------------ arquivos do pack

        private void Sincronizar(bool moverMods)
        {
            string arqEstado = Path.Combine(pastaJogo, "csmp-estado.txt");
            Dictionary<string, Estado> estado = LerEstado(arqEstado);
            HashSet<string> doPack = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (Entrada e in arquivos) doPack.Add(e.Caminho);

            if (moverMods) MoverModsDeFora(doPack);

            Detalhe("Conferindo os arquivos que você já tem…");
            List<Entrada> baixar = new List<Entrada>();
            long totalBaixar = 0;
            int mantidos = 0;
            Ui(delegate { barra.Maximum = Math.Max(1, arquivos.Count); barra.Value = 0; });
            int i = 0;
            foreach (Entrada e in arquivos)
            {
                i++;
                if (i % 50 == 0) { int v = i; Ui(delegate { barra.Value = Math.Min(barra.Maximum, v); }); }
                string local = Local(e.Caminho);
                Estado st;
                estado.TryGetValue(e.Caminho, out st);
                FileInfo fi = new FileInfo(local);
                if (fi.Exists)
                {
                    string atual = (st != null && st.Tamanho == fi.Length && st.Data == fi.LastWriteTimeUtc.Ticks) ? st.Hash : Sha1(local);
                    if (atual == e.Hash)
                    {
                        estado[e.Caminho] = Marcar(e, local);
                        continue;
                    }
                    if (e.Caminho.StartsWith("config/") && st != null && atual != st.Hash)
                    {
                        mantidos++; // o jogador mudou essa config: fica a dele
                        continue;
                    }
                }
                baixar.Add(e);
                totalBaixar += e.Tamanho;
            }

            // o que saiu do pack (e foi colocado por nos) e apagado; configs mudadas pelo jogador ficam
            int apagados = 0;
            foreach (string c in new List<string>(estado.Keys))
            {
                if (doPack.Contains(c)) continue;
                string local = Local(c);
                try
                {
                    if (File.Exists(local) && (!c.StartsWith("config/") || Sha1(local) == estado[c].Hash))
                    {
                        File.Delete(local);
                        apagados++;
                    }
                }
                catch (Exception ex) { Log("Não consegui apagar " + c + ": " + ex.Message); }
                estado.Remove(c);
            }

            if (mantidos > 0) Log(mantidos + " configuração(ões) que você mudou foram mantidas.");
            if (apagados > 0) Log(apagados + " arquivo(s) que saíram do pack foram apagados.");
            if (baixar.Count == 0)
            {
                Log("Nada para baixar: o pack já está atualizado.");
                SalvarEstado(arqEstado, estado);
                return;
            }

            Log("Baixando " + baixar.Count + " arquivo(s), " + Mb(totalBaixar) + ".");
            Ui(delegate { barra.Maximum = 1000; barra.Value = 0; });
            // varios downloads ao mesmo tempo: o pack tem centenas de configs pequenas
            Queue<Entrada> fila = new Queue<Entrada>(baixar);
            object trava = new object();
            long feito = 0;
            int prontos = 0;
            Exception falha = null;
            List<Thread> trabalhadores = new List<Thread>();
            for (int t = 0; t < 6; t++)
            {
                Thread th = new Thread(delegate()
                {
                    while (true)
                    {
                        Entrada e;
                        lock (trava)
                        {
                            if (falha != null || fila.Count == 0) return;
                            e = fila.Dequeue();
                        }
                        try
                        {
                            BaixarArquivo(e);
                        }
                        catch (Exception ex)
                        {
                            lock (trava) { if (falha == null) falha = ex; }
                            return;
                        }
                        lock (trava)
                        {
                            estado[e.Caminho] = Marcar(e, Local(e.Caminho));
                            feito += e.Tamanho;
                            prontos++;
                            if (prontos % 25 == 0) SalvarEstado(arqEstado, estado); // se cair no meio, nao recomeca do zero
                            long f = feito;
                            long tot = Math.Max(1, totalBaixar);
                            Detalhe("Baixando " + prontos + " de " + baixar.Count + "  ·  " + Mb(feito) + " de " + Mb(totalBaixar));
                            Ui(delegate { barra.Value = (int)Math.Min(1000, f * 1000 / tot); });
                        }
                    }
                });
                th.IsBackground = true;
                th.Start();
                trabalhadores.Add(th);
            }
            foreach (Thread th in trabalhadores) th.Join();
            lock (trava) { SalvarEstado(arqEstado, estado); }
            if (falha != null) throw falha;
            Log("Download terminado.");
        }

        private void BaixarArquivo(Entrada e)
        {
            string local = Local(e.Caminho);
            Directory.CreateDirectory(Path.GetDirectoryName(local));
            string temp = local + ".baixando";
            string url = Programa.REPO + Programa.PACK + Url(e.Caminho);
            for (int tentativa = 1; ; tentativa++)
            {
                try
                {
                    using (WebClient w = Cliente())
                    {
                        // a partir da 2a tentativa pula o cache do GitHub, que pode estar com a versao anterior
                        w.DownloadFile(tentativa == 1 ? url : url + "?t=" + DateTime.UtcNow.Ticks, temp);
                    }
                    if (Sha1(temp) != e.Hash) throw new IOException("o arquivo veio diferente do esperado");
                    if (File.Exists(local)) File.Delete(local);
                    File.Move(temp, local);
                    return;
                }
                catch (Exception ex)
                {
                    try { File.Delete(temp); } catch { }
                    if (tentativa >= 4) throw new IOException("falhou ao baixar " + e.Caminho + ": " + ex.Message);
                    Log("Tentando de novo " + e.Caminho + " (" + ex.Message + ")");
                    Thread.Sleep(1500 * tentativa);
                }
            }
        }

        private List<string> ModsDeFora(HashSet<string> doPack)
        {
            List<string> fora = new List<string>();
            string mods = Path.Combine(pastaJogo, "mods");
            if (!Directory.Exists(mods)) return fora;
            foreach (string f in Directory.GetFiles(mods, "*.jar"))
            {
                if (!doPack.Contains("mods/" + Path.GetFileName(f))) fora.Add(f);
            }
            return fora;
        }

        private void MoverModsDeFora(HashSet<string> doPack)
        {
            List<string> fora = ModsDeFora(doPack);
            if (fora.Count == 0) return;
            string destino = Path.Combine(pastaJogo, "mods-fora-do-csmp");
            Directory.CreateDirectory(destino);
            foreach (string f in fora)
            {
                string alvo = Path.Combine(destino, Path.GetFileName(f));
                if (File.Exists(alvo)) File.Delete(alvo);
                File.Move(f, alvo);
            }
            Log(fora.Count + " mod(s) de fora movidos para " + destino);
        }

        private string Local(string caminho)
        {
            return Path.Combine(pastaJogo, caminho.Replace('/', Path.DirectorySeparatorChar));
        }

        private static string Url(string caminho)
        {
            string[] partes = caminho.Split('/');
            for (int i = 0; i < partes.Length; i++) partes[i] = Uri.EscapeDataString(partes[i]);
            return string.Join("/", partes);
        }

        private static Estado Marcar(Entrada e, string local)
        {
            FileInfo fi = new FileInfo(local);
            Estado s = new Estado();
            s.Hash = e.Hash;
            s.Tamanho = fi.Length;
            s.Data = fi.LastWriteTimeUtc.Ticks;
            return s;
        }

        private static Dictionary<string, Estado> LerEstado(string arq)
        {
            Dictionary<string, Estado> d = new Dictionary<string, Estado>(StringComparer.OrdinalIgnoreCase);
            if (!File.Exists(arq)) return d;
            foreach (string linha in File.ReadAllLines(arq, Encoding.UTF8))
            {
                string[] p = linha.Split('|');
                if (p.Length < 4) continue;
                Estado s = new Estado();
                s.Hash = p[1];
                long.TryParse(p[2], out s.Tamanho);
                long.TryParse(p[3], out s.Data);
                d[p[0]] = s;
            }
            return d;
        }

        private static void SalvarEstado(string arq, Dictionary<string, Estado> estado)
        {
            StringBuilder sb = new StringBuilder();
            sb.Append("# arquivos colocados pelo instalador do CSMP (caminho|sha1|tamanho|data). Nao edite.\n");
            foreach (KeyValuePair<string, Estado> kv in estado)
            {
                sb.Append(kv.Key).Append('|').Append(kv.Value.Hash).Append('|').Append(kv.Value.Tamanho).Append('|').Append(kv.Value.Data).Append('\n');
            }
            File.WriteAllText(arq + ".tmp", sb.ToString(), new UTF8Encoding(false));
            if (File.Exists(arq)) File.Delete(arq);
            File.Move(arq + ".tmp", arq);
        }

        // ------------------------------------------------------------------ launcher

        private void AbrirLauncher()
        {
            if (!deuCerto) return;
            string exe = Cfg("launcher", null);
            if (exe == null || !File.Exists(exe)) exe = AcharLauncher();
            if (exe == null && tipo == "oficial")
            {
                try
                {
                    // launcher oficial da Microsoft Store: nao tem um .exe comum para abrir
                    Process.Start("explorer.exe", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft");
                    Close();
                    return;
                }
                catch { }
            }
            if (exe == null)
            {
                MessageBox.Show(this, "Não encontrei o seu launcher. Na próxima janela, escolha o programa dele (.exe).",
                        "CSMP", MessageBoxButtons.OK, MessageBoxIcon.Information);
                using (OpenFileDialog f = new OpenFileDialog())
                {
                    f.Title = "Escolha o seu launcher do Minecraft";
                    f.Filter = "Programas (*.exe)|*.exe";
                    if (f.ShowDialog(this) != DialogResult.OK) return;
                    exe = f.FileName;
                }
            }
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(exe);
                psi.WorkingDirectory = Path.GetDirectoryName(exe);
                Process.Start(psi);
                cfg["launcher"] = exe;
                SalvarConfig();
                Close();
            }
            catch (Exception e)
            {
                cfg.Remove("launcher");
                SalvarConfig();
                MessageBox.Show(this, "Não consegui abrir o launcher: " + e.Message, "CSMP", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                if (!telaFim.Visible) Concluir(true, "Tudo pronto", null);
            }
        }

        private string AcharLauncher()
        {
            if (tipo == "oficial") return AcharLauncherOficial();
            if (tipo == "instancia") return AcharLauncherDeInstancias();
            string ad = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string[] lugares = {
                Path.Combine(RaizAtual(), "TLauncher.exe"),
                Path.Combine(ad, ".minecraft", "TLauncher.exe"),
            };
            return Primeiro(lugares);
        }

        private static string AcharLauncherOficial()
        {
            string pf86 = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
            string pf = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
            return Primeiro(new string[] {
                Path.Combine(pf86, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Path.Combine(pf, "Minecraft Launcher", "MinecraftLauncher.exe"),
                @"C:\XboxGames\Minecraft Launcher\Content\Minecraft.exe",
            });
        }

        private static string AcharLauncherDeInstancias()
        {
            string la = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            return Primeiro(new string[] {
                Path.Combine(la, "Programs", "PrismLauncher", "prismlauncher.exe"),
                Path.Combine(la, "Programs", "CurseForge Windows", "CurseForge.exe"),
                Path.Combine(la, "Programs", "ATLauncher", "ATLauncher.exe"),
            });
        }

        private static string Primeiro(string[] lugares)
        {
            foreach (string l in lugares)
            {
                if (File.Exists(l)) return l;
            }
            return null;
        }

        // ------------------------------------------------------------------ utilidades

        private WebClient Cliente()
        {
            WebClient w = new WebClient();
            w.Headers[HttpRequestHeader.UserAgent] = "CSMP-Instalador/" + Programa.VERSAO;
            return w;
        }

        private static string Sha1(string arq)
        {
            using (FileStream f = File.OpenRead(arq))
            using (SHA1 s = SHA1.Create())
            {
                byte[] h = s.ComputeHash(f);
                StringBuilder sb = new StringBuilder(40);
                foreach (byte b in h) sb.Append(b.ToString("x2"));
                return sb.ToString();
            }
        }

        private static string Mb(long bytes)
        {
            return (bytes / 1048576.0).ToString("0.0") + " MB";
        }

        private string Cfg(string chave, string padrao)
        {
            string v;
            return cfg.TryGetValue(chave, out v) && v.Length > 0 ? v : padrao;
        }

        private void LerConfig()
        {
            cfg = new Dictionary<string, string>();
            string arq = Path.Combine(pastaApp, "config.txt");
            if (!File.Exists(arq)) return;
            foreach (string linha in File.ReadAllLines(arq, Encoding.UTF8))
            {
                int i = linha.IndexOf('=');
                if (i > 0) cfg[linha.Substring(0, i)] = linha.Substring(i + 1);
            }
        }

        private void SalvarConfig()
        {
            if (pastaTeste != null) return;
            StringBuilder sb = new StringBuilder();
            foreach (KeyValuePair<string, string> kv in cfg) sb.Append(kv.Key).Append('=').Append(kv.Value).Append('\n');
            File.WriteAllText(Path.Combine(pastaApp, "config.txt"), sb.ToString(), new UTF8Encoding(false));
        }

        private void Detalhe(string texto)
        {
            Ui(delegate { detalhe.Text = texto; });
        }

        private void Log(string texto)
        {
            Ui(delegate { log.AppendText(texto + Environment.NewLine); });
        }

        private void Ui(MethodInvoker acao)
        {
            if (IsDisposed) return;
            if (InvokeRequired)
            {
                try { Invoke(acao); } catch (ObjectDisposedException) { } catch (InvalidOperationException) { }
            }
            else
            {
                acao();
            }
        }
    }
}
