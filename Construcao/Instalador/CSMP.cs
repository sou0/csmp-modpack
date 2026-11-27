// CSMP - instalador e atualizador do modpack.
// Compilado com o csc do .NET Framework 4 (vem no Windows), por compilar-instalador.ps1. C# 5: sem $"", sem ?. e sem =>.
//
// O que faz, toda vez que abre:
//  1. baixa o manifesto do pack no GitHub (lista de arquivos com tamanho e SHA-1);
//  2. se houver versao nova deste programa, se atualiza e reabre;
//  3. na primeira vez, acha a pasta do Minecraft e pergunta se e ela;
//  4. instala o NeoForge certo se ainda nao estiver instalado (instalador oficial, modo --install-client);
//  5. baixa so os arquivos que mudaram (mods, config, resourcepacks) e apaga os que sairam do pack;
//  6. abre o launcher (TLauncher ou o oficial).
// Configs que o jogador mudou nao sao sobrescritas. Mods que nao sao do pack podem ser movidos para outra pasta.
using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Net;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Web.Script.Serialization;
using System.Windows.Forms;

[assembly: AssemblyTitle("CSMP")]
[assembly: AssemblyProduct("CSMP - instalador e atualizador")]
[assembly: AssemblyVersion("1.0.0.0")]

namespace Csmp
{
    public static class Programa
    {
        /// <summary>Sobe junto com Construcao/Instalador/versao.txt quando este programa muda.</summary>
        public const int VERSAO = 1;
        public const string REPO = "https://raw.githubusercontent.com/sou0/csmp-modpack/main/";
        public const string PACK = "Pronto-para-Minecraft/";
        public const string EXE_NO_REPO = "Instalador/CSMP.exe";

        [STAThread]
        public static void Main(string[] args)
        {
            ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072; // TLS 1.2
            ServicePointManager.DefaultConnectionLimit = 8;
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new Janela(args));
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

    public class Janela : Form
    {
        private readonly Label titulo = new Label();
        private readonly Label status = new Label();
        private readonly ProgressBar barra = new ProgressBar();
        private readonly TextBox log = new TextBox();
        private readonly Button jogar = new Button();
        private readonly Button trocar = new Button();
        private readonly CheckBox abrirSozinho = new CheckBox();

        private readonly string pastaApp = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "CSMP");
        private Dictionary<string, string> cfg = new Dictionary<string, string>();
        private string pastaJogo;
        private bool pronto;
        /// <summary>--teste PASTA: instala nessa pasta sem perguntar, sem atalho e sem launcher, grava o log e fecha.</summary>
        private readonly string pastaTeste;

        public Janela(string[] args)
        {
            for (int a = 0; a + 1 < args.Length; a++)
            {
                if (args[a] == "--teste") pastaTeste = args[a + 1];
            }
            Text = "CSMP";
            ClientSize = new Size(560, 360);
            FormBorderStyle = FormBorderStyle.FixedSingle;
            MaximizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Color.FromArgb(18, 14, 16);
            ForeColor = Color.FromArgb(230, 222, 222);
            Font = new Font("Segoe UI", 9F);
            try { Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath); } catch { }

            titulo.Text = "CSMP";
            titulo.Font = new Font("Segoe UI Semibold", 20F);
            titulo.ForeColor = Color.FromArgb(200, 40, 40);
            titulo.SetBounds(16, 10, 300, 40);

            status.Text = "Preparando...";
            status.SetBounds(18, 54, 524, 20);

            barra.SetBounds(18, 78, 524, 16);

            log.Multiline = true;
            log.ReadOnly = true;
            log.ScrollBars = ScrollBars.Vertical;
            log.BackColor = Color.FromArgb(28, 22, 24);
            log.ForeColor = Color.FromArgb(200, 190, 190);
            log.BorderStyle = BorderStyle.FixedSingle;
            log.Font = new Font("Consolas", 8.5F);
            log.SetBounds(18, 102, 524, 196);

            abrirSozinho.Text = "Abrir o launcher sozinho quando terminar";
            abrirSozinho.SetBounds(18, 312, 280, 24);

            trocar.Text = "Trocar pasta";
            trocar.SetBounds(308, 308, 110, 32);
            trocar.FlatStyle = FlatStyle.Flat;
            trocar.Click += delegate { TrocarPasta(); };

            jogar.Text = "Jogar";
            jogar.SetBounds(428, 308, 114, 32);
            jogar.FlatStyle = FlatStyle.Flat;
            jogar.BackColor = Color.FromArgb(120, 20, 24);
            jogar.Enabled = false;
            jogar.Click += delegate { AbrirLauncher(true); };

            Controls.AddRange(new Control[] { titulo, status, barra, log, abrirSozinho, trocar, jogar });
            Shown += delegate { Comecar(); };
        }

        // ------------------------------------------------------------------ fluxo principal

        private void Comecar()
        {
            Directory.CreateDirectory(pastaApp);
            LerConfig();
            abrirSozinho.Checked = Cfg("abrir", "sim") == "sim";
            abrirSozinho.CheckedChanged += delegate { cfg["abrir"] = abrirSozinho.Checked ? "sim" : "nao"; SalvarConfig(); };
            LimparVersaoVelha();
            if (pastaTeste != null)
            {
                pastaJogo = pastaTeste;
                abrirSozinho.Checked = false;
                Log("MODO TESTE: " + pastaJogo);
                new Thread(Trabalhar) { IsBackground = true }.Start();
                return;
            }
            InstalarNoComputador();

            pastaJogo = Cfg("pasta", null);
            if (pastaJogo == null || !Directory.Exists(pastaJogo))
            {
                pastaJogo = EscolherPasta();
                if (pastaJogo == null) { Close(); return; }
                cfg["pasta"] = pastaJogo;
                SalvarConfig();
            }
            Log("Pasta do jogo: " + pastaJogo);
            new Thread(Trabalhar) { IsBackground = true }.Start();
        }

        private void Trabalhar()
        {
            try
            {
                Status("Procurando atualizacao...");
                Dictionary<string, object> manifesto;
                try
                {
                    manifesto = BaixarManifesto();
                }
                catch (Exception e)
                {
                    Log("Nao consegui falar com o GitHub: " + e.Message);
                    bool jaInstalado = File.Exists(Path.Combine(pastaJogo, "csmp-estado.txt"));
                    Status(jaInstalado ? "Sem internet: usando a versao que ja esta instalada." : "Sem internet e o pack ainda nao foi instalado.");
                    Terminar(jaInstalado);
                    return;
                }
                Log("Versao do pack no GitHub: " + manifesto["versao"]);

                int versaoExe = Convert.ToInt32(manifesto["instalador"]);
                if (versaoExe > Programa.VERSAO && pastaTeste == null && AtualizarEsteExe())
                {
                    return;
                }

                string neoforge = (string)manifesto["neoforge"];
                if (!InstalarNeoForge(neoforge))
                {
                    Terminar(false);
                    return;
                }

                List<Entrada> arquivos = new List<Entrada>();
                foreach (object o in (ArrayList)manifesto["arquivos"])
                {
                    Dictionary<string, object> d = (Dictionary<string, object>)o;
                    Entrada e = new Entrada();
                    e.Caminho = (string)d["c"];
                    e.Tamanho = Convert.ToInt64(d["t"]);
                    e.Hash = (string)d["h"];
                    arquivos.Add(e);
                }
                Sincronizar(arquivos);
                if (pastaTeste == null)
                {
                    cfg["versao"] = (string)manifesto["versao"];
                    cfg["neoforge"] = neoforge;
                    SalvarConfig();
                }
                Status("Tudo pronto! Pack de " + manifesto["versao"] + ". No launcher, use a versao neoforge-" + neoforge + ".");
                Terminar(true);
            }
            catch (Exception e)
            {
                Log("ERRO: " + e);
                Status("Deu erro. Veja o texto acima (e mande um print para o pessoal do CSMP).");
                Terminar(File.Exists(Path.Combine(pastaJogo, "csmp-estado.txt")));
            }
        }

        private void Terminar(bool podeJogar)
        {
            Ui(delegate
            {
                barra.Value = barra.Maximum;
                jogar.Enabled = podeJogar;
                pronto = podeJogar;
                if (pastaTeste != null)
                {
                    File.WriteAllText(Path.Combine(pastaTeste, "csmp-teste-log.txt"), status.Text + Environment.NewLine + log.Text);
                    Close();
                    return;
                }
                if (podeJogar && abrirSozinho.Checked)
                {
                    AbrirLauncher(false);
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
                Status("Atualizando o instalador...");
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
                Process.Start(atual);
                Ui(delegate { Close(); });
                return true;
            }
            catch (Exception e)
            {
                Log("Nao consegui atualizar o instalador (" + e.Message + "), seguindo com este.");
                return false;
            }
        }

        private void LimparVersaoVelha()
        {
            try { File.Delete(Application.ExecutablePath + ".velho"); } catch { }
        }

        /// <summary>Na primeira vez copia o exe para %AppData%\CSMP e cria o atalho "CSMP" na area de trabalho.</summary>
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
                string atalho = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "CSMP.lnk");
                if (!File.Exists(atalho))
                {
                    object shell = Activator.CreateInstance(Type.GetTypeFromProgID("WScript.Shell"));
                    object lnk = shell.GetType().InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { atalho });
                    lnk.GetType().InvokeMember("TargetPath", BindingFlags.SetProperty, null, lnk, new object[] { destino });
                    lnk.GetType().InvokeMember("WorkingDirectory", BindingFlags.SetProperty, null, lnk, new object[] { pastaApp });
                    lnk.GetType().InvokeMember("Description", BindingFlags.SetProperty, null, lnk, new object[] { "Atualiza e abre o CSMP" });
                    lnk.GetType().InvokeMember("Save", BindingFlags.InvokeMethod, null, lnk, null);
                    Log("Atalho \"CSMP\" criado na area de trabalho. Use ele para jogar: ele sempre atualiza antes.");
                }
            }
            catch (Exception e)
            {
                Log("Aviso: nao consegui criar o atalho (" + e.Message + ").");
            }
        }

        // ------------------------------------------------------------------ pasta do jogo

        private string EscolherPasta()
        {
            string padrao = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
            if (Directory.Exists(padrao))
            {
                string extra = Directory.Exists(Path.Combine(padrao, "mods")) ? "\n\nEla ja tem uma pasta mods." : "";
                DialogResult r = MessageBox.Show(this,
                        "Encontrei a pasta do Minecraft:\n\n" + padrao + extra + "\n\nE nessa pasta que voce quer colocar o CSMP?\n" +
                        "(Sim = usar essa; Nao = escolher outra)",
                        "CSMP - pasta do jogo", MessageBoxButtons.YesNoCancel, MessageBoxIcon.Question);
                if (r == DialogResult.Yes) return padrao;
                if (r == DialogResult.Cancel) return null;
            }
            else
            {
                MessageBox.Show(this, "Nao achei a pasta .minecraft. Escolha a pasta do jogo (a que tem ou vai ter a pasta mods).",
                        "CSMP - pasta do jogo", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            using (FolderBrowserDialog f = new FolderBrowserDialog())
            {
                f.Description = "Escolha a pasta do jogo (a que tem saves, options.txt e a pasta mods)";
                if (Directory.Exists(padrao)) f.SelectedPath = padrao;
                if (f.ShowDialog(this) != DialogResult.OK) return null;
                string p = f.SelectedPath;
                if (string.Equals(Path.GetFileName(p), "mods", StringComparison.OrdinalIgnoreCase))
                {
                    p = Path.GetDirectoryName(p); // escolheu a pasta mods: o jogo e a de cima
                }
                return p;
            }
        }

        private void TrocarPasta()
        {
            string nova = EscolherPasta();
            if (nova == null) return;
            cfg["pasta"] = nova;
            SalvarConfig();
            MessageBox.Show(this, "Pasta trocada. O CSMP vai reabrir para instalar nela.", "CSMP");
            Process.Start(Application.ExecutablePath);
            Close();
        }

        // ------------------------------------------------------------------ NeoForge

        private bool InstalarNeoForge(string versao)
        {
            string nome = "neoforge-" + versao;
            if (File.Exists(Path.Combine(pastaJogo, "versions", nome, nome + ".json")))
            {
                Log("NeoForge " + versao + " ja instalado.");
                return true;
            }
            Status("Instalando o NeoForge " + versao + "...");
            string java = AcharJava();
            if (java == null)
            {
                Log("Nao achei o Java 21. Ele vem junto com o launcher oficial e com o TLauncher (abra o Minecraft 1.21.1 uma vez),");
                Log("ou instale o Java 21 em https://adoptium.net/temurin/releases/?version=21 e abra o CSMP de novo.");
                Status("Falta o Java 21 para instalar o NeoForge.");
                return false;
            }
            Log("Java: " + java);
            string perfis = Path.Combine(pastaJogo, "launcher_profiles.json");
            if (!File.Exists(perfis))
            {
                Directory.CreateDirectory(pastaJogo);
                File.WriteAllText(perfis, "{\n  \"profiles\": {},\n  \"version\": 3\n}\n");
            }
            string instalador = Path.Combine(Path.GetTempPath(), nome + "-installer.jar");
            string url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + versao + "/" + nome + "-installer.jar";
            Log("Baixando o instalador oficial do NeoForge...");
            using (WebClient w = Cliente())
            {
                w.DownloadFile(url, instalador);
                string esperado = w.DownloadString(url + ".sha1").Trim().ToLowerInvariant();
                if (esperado.Length >= 40 && Sha1(instalador) != esperado.Substring(0, 40))
                {
                    Log("O instalador do NeoForge veio corrompido. Tente de novo.");
                    return false;
                }
            }
            Log("Rodando o instalador do NeoForge (baixa o Minecraft 1.21.1 e as bibliotecas, pode demorar)...");
            ProcessStartInfo psi = new ProcessStartInfo(java, "-jar \"" + instalador + "\" --install-client \"" + pastaJogo + "\"");
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.RedirectStandardOutput = true;
            psi.RedirectStandardError = true;
            psi.WorkingDirectory = Path.GetTempPath();
            using (Process p = new Process())
            {
                p.StartInfo = psi;
                int linhas = 0;
                DataReceivedEventHandler saida = delegate(object s, DataReceivedEventArgs a)
                {
                    if (a.Data == null) return;
                    linhas++;
                    if (linhas % 25 == 0) Status("Instalando o NeoForge... (" + linhas + ")");
                    if (a.Data.IndexOf("rror", StringComparison.Ordinal) >= 0 || a.Data.StartsWith("Successfully")) Log("  " + a.Data);
                };
                p.OutputDataReceived += saida;
                p.ErrorDataReceived += saida;
                p.Start();
                p.BeginOutputReadLine();
                p.BeginErrorReadLine();
                p.WaitForExit();
                if (p.ExitCode != 0 || !File.Exists(Path.Combine(pastaJogo, "versions", nome, nome + ".json")))
                {
                    Log("O instalador do NeoForge falhou (codigo " + p.ExitCode + "). O log dele fica em " + instalador + ".log");
                    return false;
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
            string[] runtimes = {
                Path.Combine(pastaJogo, "runtime"),
                Path.Combine(ad, ".minecraft", "runtime"),
                Path.Combine(la, "Packages", "Microsoft.4297127D64EC6_8wekyb3d8bbwe", "LocalCache", "Local", "runtime"),
                Path.Combine(ad, ".tlauncher"),
                @"C:\Program Files\Eclipse Adoptium",
                @"C:\Program Files\Java",
                @"C:\Program Files\Microsoft",
                @"C:\Program Files (x86)\Minecraft Launcher\runtime",
            };
            foreach (string r in runtimes) Procurar(r, 0, candidatos);
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

        private void Sincronizar(List<Entrada> arquivos)
        {
            string arqEstado = Path.Combine(pastaJogo, "csmp-estado.txt");
            Dictionary<string, Estado> estado = LerEstado(arqEstado);
            bool primeiraVez = estado.Count == 0;
            HashSet<string> doPack = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (Entrada e in arquivos) doPack.Add(e.Caminho);

            if (primeiraVez) MoverModsDeFora(doPack);

            Status("Conferindo os arquivos...");
            List<Entrada> baixar = new List<Entrada>();
            long totalBaixar = 0;
            int mantidos = 0;
            Ui(delegate { barra.Maximum = arquivos.Count; barra.Value = 0; });
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
                catch (Exception ex) { Log("Nao consegui apagar " + c + ": " + ex.Message); }
                estado.Remove(c);
            }

            if (mantidos > 0) Log(mantidos + " config(s) que voce mudou foram mantidas.");
            if (apagados > 0) Log(apagados + " arquivo(s) que sairam do pack foram apagados.");
            if (baixar.Count == 0)
            {
                Log("Nada para baixar: o pack ja esta atualizado.");
                SalvarEstado(arqEstado, estado);
                return;
            }

            Log("Baixando " + baixar.Count + " arquivo(s), " + Mb(totalBaixar) + "...");
            Ui(delegate { barra.Maximum = 1000; barra.Value = 0; });
            long feito = 0;
            int n = 0;
            foreach (Entrada e in baixar)
            {
                n++;
                Status("Baixando " + n + "/" + baixar.Count + " (" + Mb(feito) + " de " + Mb(totalBaixar) + "): " + Path.GetFileName(e.Caminho));
                BaixarArquivo(e);
                estado[e.Caminho] = Marcar(e, Local(e.Caminho));
                feito += e.Tamanho;
                long f = feito;
                long t = Math.Max(1, totalBaixar);
                Ui(delegate { barra.Value = (int)Math.Min(1000, f * 1000 / t); });
                if (n % 20 == 0) SalvarEstado(arqEstado, estado); // se cair no meio, nao recomeca do zero
            }
            SalvarEstado(arqEstado, estado);
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
                    if (Sha1(temp) != e.Hash) throw new IOException("arquivo veio diferente do esperado");
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

        /// <summary>Primeira instalacao: mods que nao sao do pack podem quebrar o jogo; pergunta se pode tira-los.</summary>
        private void MoverModsDeFora(HashSet<string> doPack)
        {
            string mods = Path.Combine(pastaJogo, "mods");
            if (!Directory.Exists(mods)) return;
            List<string> fora = new List<string>();
            foreach (string f in Directory.GetFiles(mods, "*.jar"))
            {
                if (!doPack.Contains("mods/" + Path.GetFileName(f))) fora.Add(f);
            }
            if (fora.Count == 0) return;
            DialogResult r = DialogResult.No;
            Ui(delegate
            {
                r = MessageBox.Show(this, "A pasta mods tem " + fora.Count + " mod(s) que nao sao do CSMP.\n\n" +
                        "Posso move-los para a pasta \"mods-fora-do-csmp\" (nada e apagado)? Misturar mods costuma quebrar o jogo.",
                        "CSMP", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
            });
            if (r != DialogResult.Yes)
            {
                Log("Mods de fora deixados na pasta mods (" + fora.Count + ").");
                return;
            }
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

        private void AbrirLauncher(bool clicou)
        {
            if (!pronto && !clicou) return;
            string exe = Cfg("launcher", null);
            if (exe == null || !File.Exists(exe))
            {
                exe = AcharLauncher();
            }
            if (exe == null)
            {
                using (OpenFileDialog f = new OpenFileDialog())
                {
                    f.Title = "Onde esta o seu launcher? (TLauncher.exe ou Minecraft Launcher)";
                    f.Filter = "Programas (*.exe)|*.exe";
                    if (f.ShowDialog(this) != DialogResult.OK) return;
                    exe = f.FileName;
                }
            }
            cfg["launcher"] = exe;
            SalvarConfig();
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(exe);
                psi.WorkingDirectory = Path.GetDirectoryName(exe);
                Process.Start(psi);
                if (Cfg("avisou", "nao") != "sim")
                {
                    cfg["avisou"] = "sim";
                    SalvarConfig();
                    MessageBox.Show(this, "No launcher, escolha a versao \"neoforge-" + Cfg("neoforge", "") + "\" (no oficial, o perfil NeoForge)\n" +
                            "e coloque pelo menos 6 GB de RAM nas configuracoes dele.\n\nDa proxima vez e so abrir o atalho CSMP.",
                            "CSMP", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                Close();
            }
            catch (Exception e)
            {
                cfg.Remove("launcher");
                SalvarConfig();
                MessageBox.Show(this, "Nao consegui abrir o launcher: " + e.Message, "CSMP");
            }
        }

        private string AcharLauncher()
        {
            string ad = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string pf86 = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
            string[] lugares = {
                Path.Combine(pastaJogo, "TLauncher.exe"),
                Path.Combine(ad, ".minecraft", "TLauncher.exe"),
                Path.Combine(pf86, "Minecraft Launcher", "MinecraftLauncher.exe"),
                @"C:\XboxGames\Minecraft Launcher\Content\Minecraft.exe",
            };
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
            StringBuilder sb = new StringBuilder();
            foreach (KeyValuePair<string, string> kv in cfg) sb.Append(kv.Key).Append('=').Append(kv.Value).Append('\n');
            File.WriteAllText(Path.Combine(pastaApp, "config.txt"), sb.ToString(), new UTF8Encoding(false));
        }

        private void Status(string texto)
        {
            Ui(delegate { status.Text = texto; });
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
