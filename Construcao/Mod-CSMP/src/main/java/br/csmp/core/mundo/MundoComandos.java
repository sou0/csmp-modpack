package br.csmp.core.mundo;

import br.csmp.core.sanidade.Paranoia;
import br.csmp.core.sanidade.Sanidade;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de administracao (nivel 2):
 *  /csmp bosque                      mostra o centro da Floresta das Betulas Indoceis mais proxima.
 *  /csmp insanidade ver <jogador>    mostra a insanidade e a paranoia de um jogador.
 *  /csmp insanidade definir <jogador> <valor>   define a insanidade (0 a 100).
 *  /csmp paranoia definir <jogador> <valor>     define a paranoia (100 com a venda = arranca).
 *  /csmp venda animar <jogador>      toca so a animacao de arrancar a venda.
 */
public final class MundoComandos {
    private MundoComandos() {}

    public static void comandos(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("csmp").requires(fonte -> fonte.hasPermission(2))
                .then(Commands.literal("bosque").executes(ctx -> bosque(ctx.getSource())))
                .then(Commands.literal("vazio")
                        .then(Commands.argument("tipo", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        new String[] {"rua", "casa", "loja", "capela", "praca", "ruina", "estatua"}, b))
                                .executes(ctx -> vazio(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "tipo")))))
                .then(Commands.literal("ars")
                        .then(Commands.literal("liberar").then(Commands.argument("jogador", EntityArgument.player())
                                .executes(ctx -> ars(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"), true))))
                        .then(Commands.literal("bloquear").then(Commands.argument("jogador", EntityArgument.player())
                                .executes(ctx -> ars(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"), false)))))
                .then(Commands.literal("libertar").then(Commands.argument("jogador", EntityArgument.player())
                        .executes(ctx -> libertar(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador")))))
                .then(Commands.literal("preso")
                        .then(Commands.literal("marcar").then(Commands.argument("jogador", EntityArgument.player())
                                .executes(ctx -> marcarPreso(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"), true))))
                        .then(Commands.literal("soltar").then(Commands.argument("jogador", EntityArgument.player())
                                .executes(ctx -> marcarPreso(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"), false)))))
                .then(Commands.literal("paranoia")
                        .then(Commands.literal("definir").then(Commands.argument("jogador", EntityArgument.player())
                                .then(Commands.argument("valor", FloatArgumentType.floatArg(0.0F, 100.0F))
                                        .executes(ctx -> paranoia(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"),
                                                FloatArgumentType.getFloat(ctx, "valor")))))))
                .then(Commands.literal("venda")
                        .then(Commands.literal("animar").then(Commands.argument("jogador", EntityArgument.player())
                                .executes(ctx -> animarVenda(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"))))))
                .then(Commands.literal("insanidade")
                        .then(Commands.literal("ver")
                                .then(Commands.argument("jogador", EntityArgument.player())
                                        .executes(ctx -> ver(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador")))))
                        .then(Commands.literal("definir")
                                .then(Commands.argument("jogador", EntityArgument.player())
                                        .then(Commands.argument("valor", FloatArgumentType.floatArg(0.0F, 100.0F))
                                                .executes(ctx -> definir(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"),
                                                        FloatArgumentType.getFloat(ctx, "valor"))))))));
    }

    private static int bosque(CommandSourceStack fonte) {
        net.minecraft.server.level.ServerLevel overworld = fonte.getServer().overworld();
        int[] centro = Bosques.maisPerto(overworld, (int) fonte.getPosition().x, (int) fonte.getPosition().z);
        if (centro == null) {
            fonte.sendFailure(Component.literal("Nenhuma floresta perto daqui (as celulas em volta sao so agua)."));
            return 0;
        }
        fonte.sendSuccess(() -> Component.literal("Bosque mais perto: x=" + centro[0] + " z=" + centro[1]
                + " (raio medio " + Bosques.RAIO + ")"), false);
        return 1;
    }

    /** /csmp vazio <tipo>: coordenadas (no Vazio) da rua ou da construcao desse tipo mais perto de quem digitou. */
    private static int vazio(CommandSourceStack fonte, String tipo) {
        long semente = fonte.getServer().overworld().getSeed();
        int x = (int) fonte.getPosition().x;
        int z = (int) fonte.getPosition().z;
        int[] alvo = tipo.equals("rua")
                ? br.csmp.core.vazio.VazioConsultas.ruaMaisPerto(semente, x, z, 400)
                : br.csmp.core.vazio.VazioConsultas.loteMaisPerto(semente, x, z, tipo.equals("estatua") ? 1300 : 500, tipo);
        if (alvo == null) {
            fonte.sendFailure(Component.literal("Nao achei " + tipo + " por perto."));
            return 0;
        }
        fonte.sendSuccess(() -> Component.literal("Vazio, " + tipo + " mais perto: x=" + alvo[0] + " z=" + alvo[1]), false);
        return 1;
    }

    /** Marca (ou solta) alguem como preso no Vazio: aura vermelha e aparece no menu de reviver. So para testes/admin. */
    private static int marcarPreso(CommandSourceStack fonte, ServerPlayer jogador, boolean preso) {
        jogador.getPersistentData().putBoolean(Sanidade.PRESO, preso);
        if (preso) {
            jogador.getPersistentData().putFloat(Sanidade.INSANIDADE_AO_CAIR, Sanidade.obter(jogador));
        }
        fonte.sendSuccess(() -> Component.literal(jogador.getName().getString() + (preso ? " agora conta como preso no Vazio." : " nao esta mais preso.")), true);
        return 1;
    }

    /** Libera (ou volta a bloquear) a magia do Ars Nouveau para um jogador. */
    private static int ars(CommandSourceStack fonte, ServerPlayer jogador, boolean liberar) {
        jogador.getPersistentData().putBoolean(ArsRestricao.LIBERADO, liberar);
        fonte.sendSuccess(() -> Component.literal("Magia do Ars Nouveau " + (liberar ? "liberada" : "bloqueada")
                + " para " + jogador.getName().getString() + "."), true);
        return 1;
    }

    /**
     * Saida de emergencia dos admins: tira alguem do Vazio sem ritual (por exemplo, se todos ficarem presos) e o
     * leva ao ponto de renascer do overworld.
     */
    private static int libertar(CommandSourceStack fonte, ServerPlayer jogador) {
        net.minecraft.server.level.ServerLevel overworld = fonte.getServer().overworld();
        net.minecraft.core.BlockPos spawn = overworld.getSharedSpawnPos();
        jogador.getPersistentData().putBoolean(Sanidade.PRESO, false);
        Sanidade.definir(jogador, Math.min(Sanidade.obter(jogador), 25.0F));
        jogador.teleportTo(overworld, spawn.getX() + 0.5D, overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                spawn.getX(), spawn.getZ()), spawn.getZ() + 0.5D, jogador.getYRot(), 0.0F);
        jogador.sendSystemMessage(Component.literal("Uma forca de fora te puxa de volta.").withStyle(net.minecraft.ChatFormatting.GRAY,
                net.minecraft.ChatFormatting.ITALIC));
        fonte.sendSuccess(() -> Component.literal(jogador.getName().getString() + " foi tirado do Vazio."), true);
        return 1;
    }

    private static int paranoia(CommandSourceStack fonte, ServerPlayer jogador, float valor) {
        Paranoia.definir(jogador, valor);
        fonte.sendSuccess(() -> Component.literal("Paranoia de " + jogador.getName().getString() + " = " + valor
                + (valor >= 100.0F ? " (com a venda na cabeca, ela e arrancada em ate 5 s)" : "")), true);
        return 1;
    }

    /** So a animacao de arrancar a venda (sem dano nem bloqueio), para conferir como fica. */
    private static int animarVenda(CommandSourceStack fonte, ServerPlayer jogador) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(jogador,
                new br.csmp.core.sanidade.VendaAnimPayload(jogador.getId()));
        fonte.sendSuccess(() -> Component.literal("Animacao da venda tocada em " + jogador.getName().getString()), false);
        return 1;
    }

    private static int ver(CommandSourceStack fonte, ServerPlayer jogador) {
        fonte.sendSuccess(() -> Component.literal(jogador.getName().getString() + ": insanidade "
                + Math.round(Sanidade.obter(jogador) * 10.0F) / 10.0F + ", paranoia "
                + Math.round(Paranoia.obter(jogador) * 10.0F) / 10.0F), false);
        return 1;
    }

    private static int definir(CommandSourceStack fonte, ServerPlayer jogador, float valor) {
        Sanidade.definir(jogador, valor);
        fonte.sendSuccess(() -> Component.literal("Insanidade de " + jogador.getName().getString() + " = " + valor), true);
        return 1;
    }
}
