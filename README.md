# CSMP Modpack

Modpack NeoForge 1.21.1 do servidor CSMP (jogadores com e sem conta original).

## Instalar (jogadores)

Baixe **[CSMP-Setup.exe](https://github.com/sou0/csmp-modpack/raw/main/Instalador/CSMP-Setup.exe)** e instale (Avançar, Avançar, Concluir).
Depois abra pelo atalho **CSMP** (área de trabalho ou Menu Iniciar).
Ele pergunta qual launcher você usa e faz o jeito certo para cada um:
- **Oficial:** cria a instalação "CSMP" no launcher (pasta própria, NeoForge certo, 6 GB de memória).
- **TLauncher e outros que usam a .minecraft:** instala o NeoForge e os mods na .minecraft
  (ou na pasta da versão, com a opção "Versões separadas").
- **Prism, MultiMC, CurseForge, ATLauncher:** coloca os mods na pasta da instância e explica como escolher o NeoForge nela.

Para tirar, use "Adicionar ou remover programas" do Windows (ele pergunta se tira também os mods). Abra sempre por esse atalho: ele baixa as atualizações
do pack antes de abrir o launcher. Configs que você mudar não são sobrescritas.

O Windows pode avisar que o programa é de um "editor desconhecido" (ele não tem assinatura digital paga):
clique em "Mais informações" > "Executar assim mesmo".

Para publicar uma atualização: rode `Construcao/GERAR-PASTA-PRONTA.bat` (gera o manifesto) e suba no git.
Se mudar o instalador (`Construcao/Instalador/CSMP.cs`), suba o número em `versao.txt` e em `Programa.VERSAO`
e rode `compilar-instalador.ps1` (usa o Inno Setup em `_ferramentas`); o programa avisa os jogadores que saiu versão nova.

## Estrutura

```
Pronto-para-Minecraft/      <- USE ESTA: mods, config e resourcepacks para copiar para o .minecraft
Construcao/                 <- local de construcao (onde tudo e editado)
  Pack-Direto/              fonte do pack (mods, config, resourcepacks, docs do servidor)
  Mod-CSMP/                 codigo-fonte do mod proprio (Medidor de Caos, clima, mobs)
  Ambiente-de-Teste/        servidor e cliente de teste + scripts (atalhos .bat)
  Arquivo-Morto/            pastas antigas e backups (so local, nao vai para o GitHub)
  GERAR-PASTA-PRONTA.bat    regenera Pronto-para-Minecraft a partir de Pack-Direto
```

## Fluxo de trabalho

1. Edite mods/config em `Construcao/Pack-Direto` (ou o codigo em `Construcao/Mod-CSMP` e rode `compilar.ps1`).
2. Teste com os atalhos de `Construcao/Ambiente-de-Teste`.
3. `GERAR-PASTA-PRONTA.bat` (ou `1-ATUALIZAR-TUDO.bat` / `compilar.ps1`, que ja fazem isso) atualiza `Pronto-para-Minecraft`.