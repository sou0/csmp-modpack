package br.csmp.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuracao do cliente do csmp (arquivo config/csmp-client.toml). */
public final class CsmpConfig {
    public static final ModConfigSpec ESPECIFICACAO;
    public static final ModConfigSpec.BooleanValue NUVENS_NO_VAZIO;

    static {
        ModConfigSpec.Builder construtor = new ModConfigSpec.Builder();
        NUVENS_NO_VAZIO = construtor
                .comment("Mostra as nuvens (e as nuvens 3D do Create: Skys) na dimensao Vazio. Padrao: desligado.")
                .define("nuvensNoVazio", false);
        ESPECIFICACAO = construtor.build();
    }

    private CsmpConfig() {}
}
