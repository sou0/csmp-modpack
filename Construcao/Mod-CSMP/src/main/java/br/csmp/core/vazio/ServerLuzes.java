package br.csmp.core.vazio;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * As luzes do Vazio: quem esta la dentro ve uma luz para cada jogador do overworld, que anda junto
 * com ele. Chegando perto de uma luz, da para ouvir o que esse jogador fala no chat (um eco).
 */
public final class ServerLuzes {
    /** Distancia (em blocos, no plano) para ouvir o eco de um jogador do overworld. */
    public static final double ALCANCE_DO_ECO = 24.0D;

    private ServerLuzes() {}

    public static void onServerTick(ServerTickEvent.Post evento) {
        MinecraftServer servidor = evento.getServer();
        if (servidor.getTickCount() % 10 != 0) {
            return;
        }
        ServerLevel vazio = servidor.getLevel(Vazio.DIMENSAO);
        if (vazio == null || vazio.players().isEmpty()) {
            return;
        }
        ServerLevel overworld = servidor.getLevel(Level.OVERWORLD);
        List<LuzesPayload.Luz> luzes = new ArrayList<>();
        if (overworld != null) {
            for (ServerPlayer jogador : overworld.players()) {
                if (jogador.isSpectator()) {
                    continue;
                }
                luzes.add(new LuzesPayload.Luz(jogador.getUUID().getMostSignificantBits(),
                        jogador.getUUID().getLeastSignificantBits(), jogador.getX(), jogador.getZ()));
            }
        }
        LuzesPayload pacote = new LuzesPayload(luzes);
        for (ServerPlayer preso : vazio.players()) {
            PacketDistributor.sendToPlayer(preso, pacote);
        }
    }

    /** Eco: o que um jogador do overworld fala chega, abafado, a quem esta perto da luz dele no Vazio. */
    public static void onChat(ServerChatEvent evento) {
        ServerPlayer falante = evento.getPlayer();
        if (falante.level().dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel vazio = falante.server.getLevel(Vazio.DIMENSAO);
        if (vazio == null) {
            return;
        }
        Component eco = Component.literal("[eco] " + falante.getGameProfile().getName() + ": " + evento.getRawText())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer preso : vazio.players()) {
            double dx = preso.getX() - falante.getX();
            double dz = preso.getZ() - falante.getZ();
            if (dx * dx + dz * dz <= ALCANCE_DO_ECO * ALCANCE_DO_ECO) {
                preso.sendSystemMessage(eco);
            }
        }
    }
}
