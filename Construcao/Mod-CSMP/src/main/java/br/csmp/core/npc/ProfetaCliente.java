package br.csmp.core.npc;

import net.minecraft.client.Minecraft;

/** Ponto de entrada no cliente: abre a tela do Profeta. So e chamada no cliente. */
public final class ProfetaCliente {
    private ProfetaCliente() {}

    public static void abrir(int eruditos, int entidade) {
        Minecraft.getInstance().setScreen(new ProfetaTela(eruditos, entidade));
    }
}
