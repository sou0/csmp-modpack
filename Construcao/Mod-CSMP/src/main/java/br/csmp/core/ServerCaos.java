package br.csmp.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Le o #caos do datapack csmp_caos e avisa os clientes quando muda. */
public final class ServerCaos {
    private static int ultimo = Integer.MIN_VALUE;

    private ServerCaos() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        int valor = ler(server);
        if (valor != ultimo) {
            ultimo = valor;
            PacketDistributor.sendToAllPlayers(new CaosPayload(valor));
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new CaosPayload(ler(player.server)));
        }
    }

    static int ler(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        Objective obj = sb.getObjective("csmp_caos");
        if (obj == null) return 0;
        ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(ScoreHolder.forNameOnly("#caos"), obj);
        return info == null ? 0 : info.value();
    }
}