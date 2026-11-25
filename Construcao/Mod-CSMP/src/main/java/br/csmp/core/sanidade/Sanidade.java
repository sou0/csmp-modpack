package br.csmp.core.sanidade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sanidade individual (0 a 100, invisivel). Guardada nos dados persistentes do jogador. Aqui ficam so as
 * operacoes basicas; as regras de quando sobe e desce estao em {@link ServerSanidade}.
 */
public final class Sanidade {
    public static final String CHAVE = "csmp_insanidade";
    /** Morreu e caiu no Vazio (tem aura vermelha e so sai pelo ritual). */
    public static final String PRESO = "csmp_preso";
    /** Insanidade que a pessoa tinha quando caiu (aparece no menu da estatua). */
    public static final String INSANIDADE_AO_CAIR = "csmp_insanidade_ao_cair";

    private Sanidade() {}

    /** Insanidade atual do jogador, de 0 a 100 (0 = lucido). */
    public static float obter(ServerPlayer jogador) {
        CompoundTag dados = jogador.getPersistentData();
        return dados.contains(CHAVE) ? dados.getFloat(CHAVE) : 0.0F;
    }

    public static void definir(ServerPlayer jogador, float valor) {
        jogador.getPersistentData().putFloat(CHAVE, Math.max(0.0F, Math.min(100.0F, valor)));
    }

    public static void somar(ServerPlayer jogador, float delta) {
        definir(jogador, obter(jogador) + delta);
    }
}
