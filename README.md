# CSMP Modpack

Modpack NeoForge 1.21.1 do servidor CSMP (jogadores com e sem conta original).

## Instalar (jogadores)

Baixe **[CSMP.exe](https://github.com/sou0/csmp-modpack/raw/main/Instalador/CSMP.exe)** e abra.
Ele mostra a pasta do Minecraft que encontrou (dá para trocar), instala o NeoForge certo e o pack,
e cria o atalho **CSMP** na área de trabalho. Quem usa versões separadas no TLauncher marca a opção
"Versões separadas" (ou escolhe a pasta da versão em `.minecraft\versions`): os mods vão para a pasta da versão. Abra sempre por esse atalho: ele baixa as atualizações
do pack antes de abrir o launcher. Configs que você mudar não são sobrescritas.

O Windows pode avisar que o programa é de um "editor desconhecido" (ele não tem assinatura digital paga):
clique em "Mais informações" > "Executar assim mesmo".

Para publicar uma atualização: rode `Construcao/GERAR-PASTA-PRONTA.bat` (gera o manifesto) e suba no git.
Se mudar o instalador (`Construcao/Instalador/CSMP.cs`), suba o número em `versao.txt` e em `Programa.VERSAO`
e rode `compilar-instalador.ps1`; os jogadores recebem a versão nova sozinhos.

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