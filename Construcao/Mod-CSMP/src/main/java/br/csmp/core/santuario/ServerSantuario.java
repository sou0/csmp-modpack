package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.CsmpSons;
import br.csmp.core.sanidade.Sanidade;
import br.csmp.core.vazio.Vazio;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Regras da estatua:
 *  1. Quem esta preso no Vazio ativa uma estatua de la (clique na base): o CHAMADO. So vale a primeira
 *     estatua ativada por pessoa; as outras avisam que ja ha uma conexao.
 *  2. O chamado alcanca {@link #ALCANCE_DO_SINAL} blocos. As estatuas do overworld nesse alcance (carregadas)
 *     soltam particulas e um som pulsante; ninguem recebe coordenadas.
 *  3. No overworld, a estatua e montada colocando itens na base (um por vez). Nada confere na hora: so ao
 *     ATIVAR. Errou: a estatua quebra e quem ativou e punido. Acertou e ha um chamado: comeca o ritual.
 *  4. Ativar com o AMULETO na mao: o preso volta e o amuleto some. Ativar agachado de maos vazias (sem
 *     amuleto): quem ativou troca de lugar com o preso.
 */
public final class ServerSantuario {
    /** Alcance do sinal de socorro, em blocos. */
    public static final double ALCANCE_DO_SINAL = 500.0D;
    private static final long VALIDADE_DO_CHAMADO = 20L * 60L * 15L;
    private static final double DISTANCIA_DO_CHAMADOR = 24.0D;

    private record Chamado(UUID preso, BlockPos baseVazio, long ate) {}

    private static final Map<UUID, Chamado> CHAMADOS = new ConcurrentHashMap<>();
    private static volatile MinecraftServer servidorAtual;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> BASES = new HashMap<>();
    private static Ritual ritual;

    private ServerSantuario() {}

    // ------------------------------------------------------------------ registro das bases carregadas

    static synchronized void registrarBase(ResourceKey<Level> dimensao, BlockPos pos) {
        BASES.computeIfAbsent(dimensao, k -> new HashSet<>()).add(pos.immutable());
    }

    static synchronized void removerBase(ResourceKey<Level> dimensao, BlockPos pos) {
        Set<BlockPos> conjunto = BASES.get(dimensao);
        if (conjunto != null) {
            conjunto.remove(pos);
        }
    }

    private static synchronized List<BlockPos> basesDoOverworld() {
        Set<BlockPos> conjunto = BASES.get(Level.OVERWORLD);
        return conjunto == null ? List.of() : List.copyOf(conjunto);
    }

    private static double distanciaHorizontal(BlockPos a, BlockPos b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    /** Chamados ativos (e validos) dentro do alcance de uma estatua do overworld. */
    static List<BlockPos> chamadosPerto(BlockPos baseOverworld) {
        return CHAMADOS.values().stream()
                .map(Chamado::baseVazio)
                .filter(v -> distanciaHorizontal(v, baseOverworld) <= ALCANCE_DO_SINAL)
                .toList();
    }

    // ------------------------------------------------------------------ tick

    static MinecraftServer servidor() {
        return servidorAtual;
    }

    /**
     * Pares de estatuas ligadas por um chamado ativo: {base no Vazio, base no overworld}, so as do overworld
     * carregadas e dentro do alcance do sinal. Usado pela ponte de voz.
     */
    static List<BlockPos[]> paresAtivos() {
        List<BlockPos[]> pares = new ArrayList<>();
        for (Chamado chamado : CHAMADOS.values()) {
            for (BlockPos base : basesDoOverworld()) {
                if (distanciaHorizontal(base, chamado.baseVazio()) <= ALCANCE_DO_SINAL) {
                    pares.add(new BlockPos[] {chamado.baseVazio(), base});
                }
            }
        }
        return pares;
    }

    public static void onServerTick(ServerTickEvent.Post evento) {
        servidorAtual = evento.getServer();
        if (ritual != null && ritual.tick()) {
            ritual = null;
        }
        MinecraftServer servidor = evento.getServer();
        int tick = servidor.getTickCount();
        if (tick % 40 == 0 && !CHAMADOS.isEmpty()) {
            ressoar(servidor);
        }
    }

    /** Valida os chamados e faz as estatuas do overworld no alcance ressoarem. */
    private static void ressoar(MinecraftServer servidor) {
        ServerLevel overworld = servidor.getLevel(Level.OVERWORLD);
        ServerLevel vazio = servidor.getLevel(Vazio.DIMENSAO);
        if (overworld == null || vazio == null) {
            return;
        }
        long agora = overworld.getGameTime();
        for (Iterator<Chamado> it = CHAMADOS.values().iterator(); it.hasNext(); ) {
            Chamado chamado = it.next();
            ServerPlayer preso = servidor.getPlayerList().getPlayer(chamado.preso());
            boolean perto = preso != null && Vazio.eh(preso.level())
                    && Math.hypot(preso.getX() - (chamado.baseVazio().getX() + 0.5D),
                    preso.getZ() - (chamado.baseVazio().getZ() + 0.5D)) <= DISTANCIA_DO_CHAMADOR;
            if (!perto || agora > chamado.ate()) {
                it.remove();
                if (preso != null) {
                    preso.displayClientMessage(Component.literal("O chamado se perdeu.")
                            .withStyle(ChatFormatting.GRAY), true);
                }
                continue;
            }
            for (BlockPos base : basesDoOverworld()) {
                if (distanciaHorizontal(base, chamado.baseVazio()) <= ALCANCE_DO_SINAL && overworld.isLoaded(base)) {
                    ressoarEstatua(overworld, base);
                }
            }
        }
    }

    /** Alcance (blocos) em que a estatua chamando e vista e ouvida. */
    private static final double ALCANCE_DA_RESSONANCIA = 60.0D;

    /**
     * A estatua que recebe um chamado pulsa: som grave que se ouve a ~60 blocos (volume 1 = 16 blocos) e uma
     * coluna de almas que sobe, enviada "forcada" para ser vista de longe, mesmo com particulas reduzidas.
     */
    private static void ressoarEstatua(ServerLevel overworld, BlockPos base) {
        overworld.playSound(null, base, CsmpSons.SANTUARIO_CHAMADO.get(), SoundSource.BLOCKS,
                (float) (ALCANCE_DA_RESSONANCIA / 16.0D), 0.8F);
        double x = base.getX() + 0.5D;
        double z = base.getZ() + 0.5D;
        for (ServerPlayer jogador : overworld.players()) {
            if (jogador.distanceToSqr(x, base.getY(), z) > ALCANCE_DA_RESSONANCIA * ALCANCE_DA_RESSONANCIA * 4.0D) {
                continue;
            }
            overworld.sendParticles(jogador, ParticleTypes.SCULK_SOUL, true, x, base.getY() + 1.2D, z, 10, 0.4D, 1.2D, 0.4D, 0.02D);
            for (int altura = 3; altura <= 18; altura += 3) {
                overworld.sendParticles(jogador, ParticleTypes.SOUL_FIRE_FLAME, true, x, base.getY() + altura, z, 2, 0.08D, 0.6D, 0.08D, 0.01D);
            }
        }
    }

    // ------------------------------------------------------------------ comando de administracao

    /**
     * /csmp santuario trocar x z: inicia a troca entre as duas dimensoes na mesma coordenada, sem exigir
     * nada (so para testar). /csmp santuario trocar ox oz vx vz: coordenadas diferentes.
     */
    public static void comandos(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("csmp").requires(fonte -> fonte.hasPermission(2))
                .then(Commands.literal("rituais")
                        .then(Commands.literal("revelar").executes(ctx -> revelar(ctx.getSource(), true)))
                        .then(Commands.literal("esconder").executes(ctx -> revelar(ctx.getSource(), false)))
                        .then(Commands.literal("estado").executes(ctx -> {
                            boolean r = RituaisRevelados.revelados(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(r ? "Rituais: revelados." : "Rituais: escondidos."), false);
                            return r ? 1 : 0;
                        }))));
        evento.getDispatcher().register(Commands.literal("csmp").requires(fonte -> fonte.hasPermission(2))
                .then(Commands.literal("santuario")
                        .then(Commands.literal("trocar")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> trocarPorComando(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z"),
                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z")))
                                                .then(Commands.argument("vx", IntegerArgumentType.integer())
                                                        .then(Commands.argument("vz", IntegerArgumentType.integer())
                                                                .executes(ctx -> trocarPorComando(ctx.getSource(),
                                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                                        IntegerArgumentType.getInteger(ctx, "z"),
                                                                        IntegerArgumentType.getInteger(ctx, "vx"),
                                                                        IntegerArgumentType.getInteger(ctx, "vz"))))))))));
    }

    private static int revelar(CommandSourceStack fonte, boolean valor) {
        RituaisRevelados.definir(fonte.getServer(), valor);
        fonte.sendSuccess(() -> Component.literal(valor
                ? "Rituais revelados: as estatuas do overworld agora aceitam itens e podem ser ativadas."
                : "Rituais escondidos: as estatuas do overworld ficam em silencio."), true);
        return 1;
    }

    private static int trocarPorComando(CommandSourceStack fonte, int ox, int oz, int vx, int vz) {
        if (ritual != null) {
            fonte.sendFailure(Component.literal("Ja existe um ritual em andamento."));
            return 0;
        }
        Ritual novo = Ritual.criar(fonte.getServer(), ox, oz, vx, vz, null, null);
        if (novo == null) {
            fonte.sendFailure(Component.literal("Nao achei a base da estatua nas duas dimensoes nessas coordenadas (com os chunks carregados)."));
            return 0;
        }
        ritual = novo;
        fonte.sendSuccess(() -> Component.literal("Ritual iniciado: base do overworld em y=" + novo.yOverworld()
                + ", base do Vazio em y=" + novo.yVazio()), true);
        return 1;
    }

    // ------------------------------------------------------------------ cliques na base

    /** Clique direito na base (ou na estatua). */
    public static void clicarBase(ServerLevel nivel, BlockPos pos, ServerPlayer jogador, ItemStack item) {
        if (Vazio.eh(nivel)) {
            chamar(nivel, pos, jogador);
        } else if (nivel.dimension() == Level.OVERWORLD) {
            montarOuAtivar(nivel, pos, jogador, item);
        } else {
            aviso(jogador, "A estatua nao responde aqui.");
        }
    }

    private static void aviso(ServerPlayer jogador, String texto) {
        jogador.displayClientMessage(Component.literal(texto).withStyle(ChatFormatting.GRAY), true);
    }

    private static void chamar(ServerLevel vazio, BlockPos pos, ServerPlayer jogador) {
        Chamado atual = CHAMADOS.get(jogador.getUUID());
        if (atual != null) {
            aviso(jogador, atual.baseVazio().equals(pos)
                    ? "Voce ja esta chamando por esta estatua. Fique perto e espere."
                    : "Voce ja esta ligado a outra estatua. O sinal so segue pela primeira.");
            return;
        }
        CHAMADOS.put(jogador.getUUID(), new Chamado(jogador.getUUID(), pos.immutable(), vazio.getGameTime() + VALIDADE_DO_CHAMADO));
        vazio.playSound(null, pos, CsmpSons.SANTUARIO_CHAMADO.get(), SoundSource.BLOCKS, 2.5F, 1.0F);
        vazio.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 30, 0.4D, 1.5D, 0.4D, 0.05D);
        Component chamado = Component.literal("Um chamado distante ecoa pelo chao... alguem esta preso do outro lado.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        MinecraftServer servidor = vazio.getServer();
        for (ServerPlayer outro : servidor.getPlayerList().getPlayers()) {
            if (!Vazio.eh(outro.level()) && Math.hypot(outro.getX() - pos.getX(), outro.getZ() - pos.getZ()) <= ALCANCE_DO_SINAL) {
                outro.sendSystemMessage(chamado);
            }
        }
        jogador.displayClientMessage(Component.literal("Seu chamado foi lancado. Fique perto da estatua e espere ajuda.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private static void montarOuAtivar(ServerLevel overworld, BlockPos pos, ServerPlayer jogador, ItemStack item) {
        if (!(overworld.getBlockEntity(pos) instanceof BaseEstatuaBE base)) {
            return;
        }
        if (!RituaisRevelados.revelados(overworld.getServer())) {
            aviso(jogador, "A estatua esta em silencio. Ninguem sabe ainda o que ela pede.");
            return;
        }
        List<String> passos = EstatuaPassos.passos();
        boolean temAmuleto = item.is(CsmpRegistro.AMULETO.get());
        if (base.itens().size() < passos.size()) {
            montar(overworld, pos, jogador, item, base, temAmuleto);
            return;
        }
        boolean agachado = jogador.isShiftKeyDown() && item.isEmpty();
        if (!temAmuleto && !agachado) {
            aviso(jogador, "A estatua esta completa. Ative com o amuleto na mao, ou agachado de maos vazias.");
            return;
        }
        if (!base.itens().equals(passos)) {
            quebrar(overworld, pos, jogador, base);
            return;
        }
        Chamado chamado = chamadoMaisPerto(pos);
        if (chamado == null) {
            aviso(jogador, "A estatua esta certa, mas nada responde. Ninguem chama por perto.");
            return;
        }
        if (ritual != null) {
            aviso(jogador, "Um ritual ja esta em andamento.");
            return;
        }
        Ritual novo = Ritual.criar(overworld.getServer(), pos.getX(), pos.getZ(), chamado.baseVazio().getX(),
                chamado.baseVazio().getZ(), chamado.preso(), temAmuleto ? null : jogador.getUUID());
        if (novo == null) {
            aviso(jogador, "Algo impede a troca (as duas estatuas precisam estar carregadas).");
            return;
        }
        if (temAmuleto && !jogador.getAbilities().instabuild) {
            item.shrink(1);
        }
        CHAMADOS.remove(chamado.preso());
        base.limpar();
        atualizarEstatua(overworld, pos, 0);
        ritual = novo;
    }

    private static Chamado chamadoMaisPerto(BlockPos baseOverworld) {
        Chamado melhor = null;
        double menor = Double.MAX_VALUE;
        for (Chamado c : CHAMADOS.values()) {
            double d = distanciaHorizontal(c.baseVazio(), baseOverworld);
            if (d <= ALCANCE_DO_SINAL && d < menor) {
                melhor = c;
                menor = d;
            }
        }
        return melhor;
    }

    private static void montar(ServerLevel nivel, BlockPos pos, ServerPlayer jogador, ItemStack item,
                               BaseEstatuaBE base, boolean temAmuleto) {
        if (item.isEmpty()) {
            aviso(jogador, "A base espera um item. Segure algo e clique.");
            return;
        }
        if (temAmuleto) {
            aviso(jogador, "O amuleto so serve para ativar a estatua depois de montada.");
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        if (!jogador.getAbilities().instabuild) {
            item.shrink(1);
        }
        base.adicionar(id);
        int colocados = base.itens().size();
        atualizarEstatua(nivel, pos, EstatuaPassos.mascaraPara(colocados));
        nivel.playSound(null, pos, SoundEvents.DEEPSLATE_BRICKS_PLACE, SoundSource.BLOCKS, 1.2F, 0.7F);
        nivel.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5D, pos.getY() + 1.4D, pos.getZ() + 0.5D, 12, 0.3D, 0.6D, 0.3D, 0.02D);
        boolean completa = colocados == EstatuaPassos.passos().size();
        boolean novaParte = EstatuaPassos.mascaraPara(colocados) != EstatuaPassos.mascaraPara(colocados - 1);
        if (novaParte) {
            tremer(nivel, pos, completa ? 24 : 14, completa ? 1.6F : 0.9F);
            nivel.playSound(null, pos, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.4F, 0.5F);
        }
        if (completa) {
            nivel.playSound(null, pos, CsmpSons.SANTUARIO_CHAMADO.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
        }
    }

    /** Treme a camera de quem esta a ate 16 blocos da base. */
    static void tremer(ServerLevel nivel, BlockPos pos, int ticks, float forca) {
        for (ServerPlayer jogador : nivel.players()) {
            if (jogador.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D) <= 16.0D * 16.0D) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(jogador, new TremorPayload(ticks, forca));
            }
        }
    }

    /** Estatua errada: ela quebra (volta a ficar em pedacos) e quem ativou e punido. */
    private static void quebrar(ServerLevel nivel, BlockPos pos, ServerPlayer jogador, BaseEstatuaBE base) {
        tremer(nivel, pos, 30, 2.2F);
        base.limpar();
        atualizarEstatua(nivel, pos, 0);
        nivel.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.4F, 0.6F);
        nivel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 1.4D, pos.getZ() + 0.5D, 3, 0.4D, 0.6D, 0.4D, 0.0D);
        nivel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 1.4D, pos.getZ() + 0.5D, 30, 0.5D, 0.8D, 0.5D, 0.05D);
        jogador.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
        jogador.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        Sanidade.somar(jogador, 10.0F);
        jogador.sendSystemMessage(Component.literal("A estatua rachou e desabou: os itens ou a ordem estavam errados.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    /** Acha (ou cria) a estatua em cima da base e define as partes visiveis. */
    private static void atualizarEstatua(ServerLevel nivel, BlockPos base, int mascara) {
        AABB caixa = new AABB(base.above()).inflate(0.4D, 1.0D, 0.4D);
        List<EstatuaEntity> achadas = nivel.getEntitiesOfClass(EstatuaEntity.class, caixa);
        EstatuaEntity estatua;
        if (achadas.isEmpty()) {
            estatua = CsmpRegistro.ESTATUA.get().create(nivel);
            if (estatua == null) {
                return;
            }
            estatua.moveTo(base.getX() + 0.5D, base.getY() + 1.0D, base.getZ() + 0.5D, 180.0F, 0.0F);
            estatua.setYBodyRot(180.0F);
            estatua.setYHeadRot(180.0F);
            nivel.addFreshEntity(estatua);
        } else {
            estatua = achadas.get(0);
        }
        estatua.definirPartes(mascara);
    }
}
