package br.csmp.core.santuario;

import br.csmp.core.CsmpSons;
import br.csmp.core.vazio.Vazio;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Regras do santuario:
 *  1. Quem esta preso no Vazio fica perto do santuario de la e CHAMA ajuda (clique direito no altar).
 *  2. O chamado ecoa para todos no overworld e vale por 15 minutos.
 *  3. Alguem no overworld, perto do altar de la, faz a OFERENDA (itens de valor, ate completar o requisito).
 *  4. Completada a oferenda, comeca o RITUAL: os dois lados trocam um pedaco do terreno e o preso volta.
 */
public final class ServerSantuario {
    /** Valor total de oferenda exigido. */
    public static final int REQUISITO = 16;
    private static final long VALIDADE_DO_CHAMADO = 20L * 60L * 15L;
    private static final double DISTANCIA_DO_CHAMADOR = 14.0D;

    private record Chamado(UUID jogador, long ate) {}

    private static final Map<Long, Chamado> CHAMADOS = new HashMap<>();
    private static final Map<Long, Integer> OFERENDAS = new HashMap<>();
    private static Ritual ritual;

    private ServerSantuario() {}

    private static long chave(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    public static void onServerTick(ServerTickEvent.Post evento) {
        if (ritual != null && ritual.tick()) {
            ritual = null;
        }
        MinecraftServer servidor = evento.getServer();
        if (servidor.getTickCount() % 200 == 0 && !CHAMADOS.isEmpty()) {
            long agora = servidor.overworld().getGameTime();
            for (Iterator<Map.Entry<Long, Chamado>> it = CHAMADOS.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, Chamado> e = it.next();
                if (agora > e.getValue().ate()) {
                    OFERENDAS.remove(e.getKey());
                    it.remove();
                }
            }
        }
    }

    /** Comando de teste/administracao: /csmp santuario trocar <x> <z> inicia a troca sem exigir oferenda. */
    public static void comandos(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("csmp").requires(fonte -> fonte.hasPermission(2))
                .then(Commands.literal("santuario")
                        .then(Commands.literal("trocar")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> trocarPorComando(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z"))))))));
    }

    private static int trocarPorComando(CommandSourceStack fonte, int x, int z) {
        if (ritual != null) {
            fonte.sendFailure(Component.literal("Ja existe um ritual em andamento."));
            return 0;
        }
        Ritual novo = Ritual.criar(fonte.getServer(), x, z, null);
        if (novo == null) {
            fonte.sendFailure(Component.literal("Nao achei o altar nas duas dimensoes nessa coordenada (com os chunks carregados)."));
            return 0;
        }
        ritual = novo;
        fonte.sendSuccess(() -> Component.literal("Ritual iniciado: altar do overworld em y=" + novo.yOverworld()
                + ", altar do Vazio em y=" + novo.yVazio()), true);
        return 1;
    }

    /** Clique direito no altar. */
    public static void interagir(ServerLevel nivel, BlockPos pos, ServerPlayer jogador, ItemStack item, InteractionHand mao) {
        if (Vazio.eh(nivel)) {
            chamar(nivel, pos, jogador);
        } else if (nivel.dimension() == Level.OVERWORLD) {
            oferecer(nivel, pos, jogador, item);
        } else {
            jogador.displayClientMessage(Component.literal("O altar nao responde aqui.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static void chamar(ServerLevel vazio, BlockPos pos, ServerPlayer jogador) {
        long k = chave(pos.getX(), pos.getZ());
        CHAMADOS.put(k, new Chamado(jogador.getUUID(), vazio.getGameTime() + VALIDADE_DO_CHAMADO));
        OFERENDAS.remove(k);
        vazio.playSound(null, pos, CsmpSons.SANTUARIO_CHAMADO.get(), SoundSource.BLOCKS, 2.5F, 1.0F);
        vazio.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 30, 0.4D, 1.5D, 0.4D, 0.05D);
        Component aviso = Component.literal("Um chamado ecoa pelo mundo... vem de um santuario em X " + pos.getX()
                + ", Z " + pos.getZ() + ". Alguem esta preso do outro lado.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        for (ServerPlayer outro : vazio.getServer().getPlayerList().getPlayers()) {
            if (!Vazio.eh(outro.level())) {
                outro.sendSystemMessage(aviso);
            }
        }
        jogador.displayClientMessage(Component.literal("Seu chamado foi lancado. Fique perto do santuario e espere ajuda.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private static void oferecer(ServerLevel overworld, BlockPos pos, ServerPlayer jogador, ItemStack item) {
        long k = chave(pos.getX(), pos.getZ());
        Chamado chamado = CHAMADOS.get(k);
        if (chamado == null || overworld.getGameTime() > chamado.ate()) {
            jogador.displayClientMessage(Component.literal("O santuario esta em silencio.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        MinecraftServer servidor = overworld.getServer();
        ServerPlayer preso = servidor.getPlayerList().getPlayer(chamado.jogador());
        ServerLevel vazio = servidor.getLevel(Vazio.DIMENSAO);
        boolean pertoDoAltar = preso != null && vazio != null && Vazio.eh(preso.level())
                && Math.hypot(preso.getX() - (pos.getX() + 0.5D), preso.getZ() - (pos.getZ() + 0.5D)) <= DISTANCIA_DO_CHAMADOR;
        if (!pertoDoAltar) {
            CHAMADOS.remove(k);
            OFERENDAS.remove(k);
            jogador.displayClientMessage(Component.literal("Quem chamou se afastou do santuario. O chamado se perdeu.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int valor = valorDe(item);
        if (valor <= 0) {
            jogador.displayClientMessage(Component.literal("O santuario nao se interessa por isso. Ofereca algo de valor.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int total = valor * item.getCount();
        if (!jogador.getAbilities().instabuild) {
            item.shrink(item.getCount());
        }
        int acumulado = OFERENDAS.merge(k, total, Integer::sum);
        if (acumulado < REQUISITO) {
            jogador.displayClientMessage(Component.literal("Oferenda: " + acumulado + "/" + REQUISITO)
                    .withStyle(ChatFormatting.GOLD), true);
            return;
        }
        if (ritual != null) {
            jogador.displayClientMessage(Component.literal("Um ritual ja esta em andamento.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Ritual novo = Ritual.criar(servidor, pos.getX(), pos.getZ(), chamado.jogador());
        if (novo == null) {
            jogador.displayClientMessage(Component.literal("Algo impede a troca (os dois santuarios precisam estar carregados).")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        CHAMADOS.remove(k);
        OFERENDAS.remove(k);
        ritual = novo;
    }

    /** Quanto cada item vale para o santuario. */
    private static int valorDe(ItemStack item) {
        String id = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        return switch (id) {
            case "minecraft:emerald", "minecraft:gold_ingot" -> 1;
            case "minecraft:ender_pearl" -> 2;
            case "minecraft:diamond", "minecraft:netherite_scrap" -> 3;
            case "minecraft:echo_shard" -> 4;
            case "minecraft:enchanted_golden_apple" -> 8;
            case "minecraft:netherite_ingot", "minecraft:emerald_block", "minecraft:gold_block" -> 9;
            case "minecraft:totem_of_undying" -> 12;
            case "minecraft:nether_star" -> 20;
            case "minecraft:diamond_block" -> 27;
            default -> 0;
        };
    }
}
