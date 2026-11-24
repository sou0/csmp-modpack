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
