; Instalador do CSMP (Inno Setup 6). Compilado por compilar-instalador.ps1, que passa a versao com /DVersao=N.
; Instala o "CSMP Atualizador" na pasta do usuario (sem pedir administrador), cria os atalhos e registra o
; desinstalador em "Adicionar ou remover programas". O atualizador e quem cuida do pack (mods, configs, NeoForge).

#ifndef Versao
  #define Versao "0"
#endif

[Setup]
AppId={{6E3C1B52-8F4A-4C2E-9B7D-3A51C0D9E8F1}
AppName=CSMP
AppVersion={#Versao}
AppVerName=CSMP (versão {#Versao})
AppPublisher=CSMP
AppPublisherURL=https://github.com/sou0/csmp-modpack
AppSupportURL=https://github.com/sou0/csmp-modpack
VersionInfoVersion={#Versao}.0.0.0
VersionInfoDescription=Instalador do modpack CSMP
VersionInfoProductName=CSMP
DefaultDirName={localappdata}\Programs\CSMP
DisableWelcomePage=no
DisableProgramGroupPage=yes
DisableDirPage=yes
PrivilegesRequired=lowest
OutputDir=..\..\Instalador
OutputBaseFilename=CSMP-Setup
SetupIconFile=csmp.ico
UninstallDisplayIcon={app}\CSMPAtualizador.exe
UninstallDisplayName=CSMP
WizardStyle=modern
WizardImageFile=saida\faixa-100.bmp,saida\faixa-200.bmp
WizardSmallImageFile=saida\logo-55.bmp,saida\logo-110.bmp
Compression=lzma2
SolidCompression=yes
CloseApplications=yes

[Languages]
Name: "pt"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Messages]
pt.WelcomeLabel2=Este assistente vai instalar o CSMP no seu computador.%n%nDepois de instalado, abra sempre pelo atalho CSMP: ele confere se o modpack tem atualização, baixa o que mudou e abre o seu launcher.

[Tasks]
Name: "atalho"; Description: "Criar atalho na área de trabalho"; GroupDescription: "Atalhos:"

[Files]
Source: "saida\CSMPAtualizador.exe"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\CSMP"; Filename: "{app}\CSMPAtualizador.exe"; Comment: "Atualiza e abre o CSMP"
Name: "{autodesktop}\CSMP"; Filename: "{app}\CSMPAtualizador.exe"; Comment: "Atualiza e abre o CSMP"; Tasks: atalho

[Run]
Filename: "{app}\CSMPAtualizador.exe"; Description: "Abrir o CSMP agora"; Flags: nowait postinstall skipifsilent

[Code]
// Ao desinstalar (sem ser em modo silencioso), o atualizador pergunta se tira tambem os arquivos do pack.
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  Codigo: Integer;
begin
  if (CurUninstallStep = usUninstall) and (not UninstallSilent()) then
    Exec(ExpandConstant('{app}\CSMPAtualizador.exe'), '--desinstalar', '', SW_SHOWNORMAL, ewWaitUntilTerminated, Codigo);
end;
