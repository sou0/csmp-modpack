package br.csmp.core.npc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

/** Estado do Profeta guardado no placar do mundo (sobrevive a reiniciar o servidor). */
public final class ServerProfeta {
    /** Total de Eruditos que precisam ser libertados para avancar a historia. */
    public static final int TOTAL_ERUDITOS = 8;

    private ServerProfeta() {}

    /**
     * Quantos Eruditos ja foram libertados. Fica no placar: para mudar durante os testes use
     * /scoreboard players set #eruditos csmp_cfg 3
     */
    public static int eruditosLibertados(MinecraftServer servidor) {
        Scoreboard placar = servidor.getScoreboard();
        Objective objetivo = placar.getObjective("csmp_cfg");
        if (objetivo == null) {
            return 0;
        }
        ReadOnlyScoreInfo info = placar.getPlayerScoreInfo(ScoreHolder.forNameOnly("#eruditos"), objetivo);
        int valor = info == null ? 0 : info.value();
        return Math.max(0, Math.min(TOTAL_ERUDITOS, valor));
    }
}
