package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.CsmpSons;
import br.csmp.core.sanidade.Sanidade;
import br.csmp.core.vazio.Vazio;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Regras do ritual de reviver:
 *  1. No overworld, a estatua e montada colocando itens na base, um por vez (so depois que um admin revela os
 *     rituais). Nada e conferido na hora.
 *  2. Estatua completa: os simbolos do Vazio a ate {@link #ALCANCE_DO_SINAL} blocos (mesmo x, z) acendem um farol.
 *  3. Quem esta preso (morreu e caiu no Vazio) precisa ficar a ate {@link #RAIO_DO_FAROL} blocos de um farol aceso.
 *  4. Ativar a estatua (amuleto na mao, ou agachado de maos vazias) confere a ordem: errada, a estatua quebra e quem
 *     ativou e punido; certa, abre o menu com os presos perto de um farol ligado a ela. Escolhido alguem, o ritual troca
 *     o terreno em volta da estatua com o em volta do farol. Com amuleto o preso volta; sem amuleto quem ativou toma o
 *     lugar dele.
 *  Presos tem uma aura vermelha.
 */
public final class ServerSantuario {
    /** Alcance entre a estatua do overworld e os simbolos do Vazio que ela acende. */
    public static final double ALCANCE_DO_SINAL = 500.0D;
    /** Distancia maxima do farol para um preso aparecer no menu. */
    public static final double RAIO_DO_FAROL = 16.0D;
    private static final double ALCANCE_DA_RESSONANCIA = 60.0D;
    private static final DustParticleOptions AURA = new DustParticleOptions(new Vector3f(0.75F, 0.03F, 0.06F), 1.3F);

    private static volatile MinecraftServer servidorAtual;
    private static final Set<BlockPos> BASES = new HashSet<>();
    private static final Set<BlockPos> SIGILOS = new HashSet<>();
    private static Ritual ritual;

    private ServerSantuario() {}

    // ------------------------------------------------------------------ registro do que esta carregado

    static synchronized void registrarBase(ResourceKey<Level> dimensao, BlockPos pos) {
        if (dimensao == Level.OVERWORLD) {
            BASES.add(pos.immutable());
        }
    }

    static synchronized void removerBase(ResourceKey<Level> dimensao, BlockPos pos) {
        BASES.remove(pos);
    }

    static synchronized void registrarSigilo(BlockPos pos) {
        SIGILOS.add(pos.immutable());
    }

    static synchronized void removerSigilo(BlockPos pos) {
        SIGILOS.remove(pos);
    }

    private static synchronized List<BlockPos> bases() {
        return List.copyOf(BASES);
    }

    private static synchronized List<BlockPos> sigilos() {
        return List.copyOf(SIGILOS);
    }

    private static double distanciaHorizontal(BlockPos a, BlockPos b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    static MinecraftServer servidor() {
        return servidorAtual;
    }

    /** O simbolo (no Vazio) esta aceso: existe uma estatua completa no overworld ao alcance. */
    private static boolean aceso(ServerLevel vazio, BlockPos sigilo) {
        BlockState estado = vazio.getBlockState(sigilo);
        return estado.is(CsmpRegistro.SIGILO.get()) && estado.getValue(SigiloBlock.ATIVO);
    }

    /** Pares {simbolo aceso no Vazio, estatua completa do overworld} ligados. Usado pela ponte de voz. */
    static List<BlockPos[]> paresAtivos() {
        MinecraftServer servidor = servidorAtual;
        if (servidor == null) {
            return List.of();
        }
        List<BlockPos> prontas = EstatuasProntas.todas(servidor);
        List<BlockPos[]> pares = new ArrayList<>();
        for (BlockPos sigilo : sigilos()) {
            for (BlockPos base : prontas) {
                if (distanciaHorizontal(base, sigilo) <= ALCANCE_DO_SINAL) {
                    pares.add(new BlockPos[] {sigilo, base});
                }
            }
        }
        return pares;
    }

    public static boolean preso(ServerPlayer jogador) {
        return Vazio.eh(jogador.level()) && jogador.getPersistentData().getBoolean(Sanidade.PRESO);
    }

    // ------------------------------------------------------------------ tick

    public static void onServerTick(ServerTickEvent.Post evento) {
        MinecraftServer servidor = evento.getServer();
        servidorAtual = servidor;
        if (ritual != null && ritual.tick()) {
            ritual = null;
        }
        ServerLevel vazio = servidor.getLevel(Vazio.DIMENSAO);
        if (vazio == null) {
            return;
        }
        int tick = servidor.getTickCount();
        if (tick % 20 == 0) {
            atualizarFarois(servidor, vazio);
        }
        if (tick % 8 == 0) {
            for (ServerPlayer jogador : vazio.players()) {
                if (preso(jogador)) {
                    vazio.sendParticles(AURA, jogador.getX(), jogador.getY() + 1.0D, jogador.getZ(), 5, 0.35D, 0.6D, 0.35D, 0.0D);
                }
            }
        }
        if (tick % 40 == 0) {
            ressoar(servidor, vazio);
        }
    }

    /** Acende ou apaga os simbolos carregados conforme haja (ou nao) estatua completa ao alcance. */
    private static void atualizarFarois(MinecraftServer servidor, ServerLevel vazio) {
        List<BlockPos> prontas = EstatuasProntas.todas(servidor);
        for (BlockPos sigilo : sigilos()) {
            if (!vazio.isLoaded(sigilo)) {
                continue;
            }
            BlockState estado = vazio.getBlockState(sigilo);
            if (!estado.is(CsmpRegistro.SIGILO.get())) {
                continue;
            }
            boolean ligar = prontas.stream().anyMatch(base -> distanciaHorizontal(base, sigilo) <= ALCANCE_DO_SINAL);
            if (estado.getValue(SigiloBlock.ATIVO) != ligar) {
                vazio.setBlock(sigilo, estado.setValue(SigiloBlock.ATIVO, ligar), 3);
                if (ligar) {
                    vazio.playSound(null, sigilo, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.6F);
                }
            }
        }
    }

    /** Estatua completa com algum preso perto de um farol ligado a ela pulsa (som e coluna de almas). */
    private static void ressoar(MinecraftServer servidor, ServerLevel vazio) {
        ServerLevel overworld = servidor.overworld();
        for (BlockPos base : bases()) {
            if (!overworld.isLoaded(base) || !EstatuasProntas.todas(servidor).contains(base)) {
                continue;
            }
            if (!presosPara(servidor, base).isEmpty()) {
                ressoarEstatua(overworld, base);
            }
        }
    }

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

    /** Presos que podem ser revividos por esta estatua: a ate 16 blocos de um farol aceso ligado a ela. */
    private static List<ServerPlayer> presosPara(MinecraftServer servidor, BlockPos base) {
        ServerLevel vazio = servidor.getLevel(Vazio.DIMENSAO);
        List<ServerPlayer> lista = new ArrayList<>();
        if (vazio == null) {
            return lista;
        }
        for (ServerPlayer jogador : vazio.players()) {
            if (preso(jogador) && faroLPerto(vazio, jogador, base) != null) {
                lista.add(jogador);
            }
        }
        return lista;
    }

    /** Farol aceso, ligado a esta estatua, a ate 16 blocos do jogador (o mais perto), ou null. */
    private static BlockPos faroLPerto(ServerLevel vazio, ServerPlayer jogador, BlockPos base) {
        BlockPos melhor = null;
        double menor = Double.MAX_VALUE;
        for (BlockPos sigilo : sigilos()) {
            if (distanciaHorizontal(sigilo, base) > ALCANCE_DO_SINAL || !vazio.isLoaded(sigilo) || !aceso(vazio, sigilo)) {
                continue;
            }
            double d = Math.sqrt(jogador.distanceToSqr(sigilo.getX() + 0.5D, sigilo.getY() + 1.0D, sigilo.getZ() + 0.5D));
            if (d <= RAIO_DO_FAROL && d < menor) {
                menor = d;
                melhor = sigilo;
            }
        }
        return melhor;
    }

    // ------------------------------------------------------------------ comandos de administracao

    /**
     * /csmp rituais revelar|esconder|estado e /csmp santuario trocar x z [vx vz] (troca de terreno so para testar).
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
            fonte.sendFailure(Component.literal("Nao achei a base da estatua (overworld) e o simbolo (Vazio) nessas coordenadas (com os chunks carregados)."));
            return 0;
        }
        ritual = novo;
        fonte.sendSuccess(() -> Component.literal("Ritual iniciado: base do overworld em y=" + novo.yOverworld()
                + ", simbolo do Vazio em y=" + novo.yVazio()), true);
        return 1;
    }

    // ------------------------------------------------------------------ cliques

    private static void aviso(ServerPlayer jogador, String texto) {
        jogador.displayClientMessage(Component.literal(texto).withStyle(ChatFormatting.GRAY), true);
    }

    /** Clique no simbolo do Vazio: so diz o estado (quem revive e quem esta no overworld). */
    static void clicarSigilo(ServerLevel nivel, BlockPos pos, BlockState estado, ServerPlayer jogador) {
        if (estado.getValue(SigiloBlock.ATIVO)) {
            aviso(jogador, preso(jogador)
                    ? "O farol esta aceso. Fique perto dele: do outro lado, alguem pode te escolher."
                    : "O farol esta aceso. Alguem do outro lado montou a estatua.");
        } else {
            aviso(jogador, "O simbolo esta apagado. Do outro lado, ninguem montou a estatua ainda.");
        }
    }

    /** Clique direito na base da estatua (ou na propria estatua). */
    public static void clicarBase(ServerLevel nivel, BlockPos pos, ServerPlayer jogador, ItemStack item) {
        if (nivel.dimension() != Level.OVERWORLD) {
            aviso(jogador, "A estatua nao responde aqui.");
            return;
        }
        if (!(nivel.getBlockEntity(pos) instanceof BaseEstatuaBE base)) {
            return;
        }
        if (!RituaisRevelados.revelados(nivel.getServer())) {
            aviso(jogador, "A estatua esta em silencio. Ninguem sabe ainda o que ela pede.");
            return;
        }
        List<String> passos = EstatuaPassos.passos();
        boolean temAmuleto = item.is(CsmpRegistro.AMULETO.get());
        if (base.itens().size() < passos.size()) {
            montar(nivel, pos, jogador, item, base, temAmuleto);
            return;
        }
        boolean agachado = jogador.isShiftKeyDown() && item.isEmpty();
        if (!temAmuleto && !agachado) {
            aviso(jogador, "A estatua esta completa. Ative com o amuleto na mao, ou agachado de maos vazias.");
            return;
        }
        if (!base.itens().equals(passos)) {
            quebrar(nivel, pos, jogador, base);
            return;
        }
        List<RitualPayloads.Preso> lista = new ArrayList<>();
        for (ServerPlayer preso : presosPara(nivel.getServer(), pos)) {
            lista.add(new RitualPayloads.Preso(preso.getUUID(), preso.getGameProfile().getName(),
                    preso.getPersistentData().getFloat(Sanidade.INSANIDADE_AO_CAIR)));
        }
        if (lista.isEmpty()) {
            aviso(jogador, "A estatua esta certa, mas ninguem espera perto de um farol do outro lado.");
            return;
        }
        PacketDistributor.sendToPlayer(jogador, new RitualPayloads.AbrirMenu(pos, temAmuleto, lista));
    }

    /** Escolha feita no menu: confere tudo de novo (o jogador pode ter mudado de lugar ou de item) e inicia o ritual. */
    public static void escolher(ServerPlayer jogador, BlockPos pos, UUID idPreso, boolean comAmuleto) {
        ServerLevel overworld = jogador.server.overworld();
        if (jogador.level() != overworld || jogador.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D) > 64.0D) {
            return;
        }
        if (!(overworld.getBlockEntity(pos) instanceof BaseEstatuaBE base) || !base.itens().equals(EstatuaPassos.passos())) {
            return;
        }
        ItemStack mao = jogador.getMainHandItem();
        if (comAmuleto && !mao.is(CsmpRegistro.AMULETO.get())) {
            aviso(jogador, "O amuleto precisa estar na sua mao.");
            return;
        }
        ServerPlayer preso = jogador.server.getPlayerList().getPlayer(idPreso);
        ServerLevel vazio = jogador.server.getLevel(Vazio.DIMENSAO);
        if (preso == null || vazio == null || !preso(preso)) {
            aviso(jogador, "Essa pessoa nao esta mais la.");
            return;
        }
        BlockPos farol = faroLPerto(vazio, preso, pos);
        if (farol == null) {
            aviso(jogador, "Essa pessoa se afastou do farol.");
            return;
        }
        if (ritual != null) {
            aviso(jogador, "Um ritual ja esta em andamento.");
            return;
        }
        Ritual novo = Ritual.criar(jogador.server, pos.getX(), pos.getZ(), farol.getX(), farol.getZ(),
                preso.getUUID(), comAmuleto ? null : jogador.getUUID());
        if (novo == null) {
            aviso(jogador, "Algo impede a troca (a estatua e o farol precisam estar carregados).");
            return;
        }
        if (comAmuleto && !jogador.getAbilities().instabuild) {
            mao.shrink(1);
        }
        base.limpar();
        EstatuasProntas.marcar(jogador.server, pos, false);
        atualizarEstatua(overworld, pos, 0);
        ritual = novo;
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
            EstatuasProntas.marcar(nivel.getServer(), pos, true);
            nivel.playSound(null, pos, CsmpSons.SANTUARIO_CHAMADO.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
        }
    }

    /** Treme a camera de quem esta a ate 16 blocos da base. */
    static void tremer(ServerLevel nivel, BlockPos pos, int ticks, float forca) {
        for (ServerPlayer jogador : nivel.players()) {
            if (jogador.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D) <= 16.0D * 16.0D) {
                PacketDistributor.sendToPlayer(jogador, new TremorPayload(ticks, forca));
            }
        }
    }

    /** Estatua errada: ela quebra (volta a ficar em pedacos), o farol apaga e quem ativou e punido. */
    private static void quebrar(ServerLevel nivel, BlockPos pos, ServerPlayer jogador, BaseEstatuaBE base) {
        tremer(nivel, pos, 30, 2.2F);
        base.limpar();
        EstatuasProntas.marcar(nivel.getServer(), pos, false);
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
