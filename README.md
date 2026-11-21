# CSMP Modpack

Modpack NeoForge 1.21.1 do servidor CSMP (jogadores com e sem conta original).

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