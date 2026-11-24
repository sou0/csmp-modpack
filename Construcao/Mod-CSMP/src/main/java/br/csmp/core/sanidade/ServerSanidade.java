package br.csmp.core.sanidade;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.mundo.Bosques;
import br.csmp.core.vazio.Vazio;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regras da sanidade (insanidade de 0 a 100, invisivel):
 *  Sobe: matar jogador +15, matar NPC bom +20, bater em NPC bom +3 por golpe, morrer +5, olho de betula aberto
 *  (+0,34 por segundo, a venda anula) e andar no chao da floresta (+1 a cada 15 s, a venda NAO anula).
 *  Desce: matar mob hostil -0,5, matar mob de sculk fraco -2, forte -10, e -1 a cada 5 minutos sem fazer maldade.
 *  Ao morrer, a chance de cair no Vazio depende da faixa de insanidade (veja {@link #chanceDeVazio}).
 */
@EventBusSubscriber(modid = br.csmp.core.CsmpMod.ID)
public final class ServerSanidade {
    /** Mobs de sculk "fracos": -2 de insanidade ao matar. */
    public static final TagKey<EntityType<?>> SCULK_FRACO = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("csmp", "sculk_fraco"));
    /** Mobs de sculk "fortes": -10 de insanidade ao matar. */
    public static final TagKey<EntityType<?>> SCULK_FORTE = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("csmp", "sculk_forte"));
    /** NPCs bons: matar ou bater neles aumenta a insanidade. */
    public static final TagKey<EntityType<?>> NPC_BOM = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("csmp", "npc_bom"));

    private static final Map<UUID, Long> ULTIMO_OLHO = new ConcurrentHashMap<>();

    private ServerSanidade() {}

    /** Chance (0 a 1) de cair no Vazio ao morrer, pela insanidade atual. */
    public static double chanceDeVazio(float insanidade) {
        if (insanidade >= 100.0F) {
            return 0.90D;
        }
        if (insanidade >= 75.0F) {
            return 0.65D;
        }
        if (insanidade >= 50.0F) {
            return 0.35D;
        }
        if (insanidade >= 25.0F) {
            return 0.10D;
        }
        return 0.0D;
    }

    /** Faixa de efeitos na tela: 0 = nenhum ... 4 = colapso. */
    public static int faixa(float insanidade) {
        if (insanidade >= 100.0F) {
            return 4;
        }
        if (insanidade >= 75.0F) {
            return 3;
        }
        if (insanidade >= 50.0F) {
            return 2;
        }
        if (insanidade >= 25.0F) {
            return 1;
        }
        return 0;
    }

    /** O jogador esta com a venda? (Chamado pelos olhos das betulas e pela paranoia.) */
    public static boolean temVenda(Player jogador) {
        ItemStack cabeca = jogador.getItemBySlot(EquipmentSlot.HEAD);
        return cabeca.is(CsmpRegistro.VENDA.get());
    }

    /** Um olho de betula aberto: +0,34 de insanidade por segundo, no maximo uma vez por segundo, sem venda. */
    public static void drenarOlho(ServerPlayer jogador, long tempo) {
        if (temVenda(jogador)) {
            return;
        }
        Long antes = ULTIMO_OLHO.get(jogador.getUUID());
        if (antes != null && tempo - antes < 18L) {
            return;
        }
        ULTIMO_OLHO.put(jogador.getUUID(), tempo);
        Sanidade.somar(jogador, 0.34F);
    }

    // ------------------------------------------------------------------ eventos

    @SubscribeEvent
    public static void aoTick(PlayerTickEvent.Post evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || jogador.isSpectator()) {
            return;
        }
        long tempo = jogador.level().getGameTime();
        // andar no chao da floresta: +1 a cada 15 s (a venda nao protege: voce absorve a energia do sculk)
        if (tempo % 300L == 0L && jogador.onGround() && jogador.level() instanceof ServerLevel nivel
                && !Vazio.eh(nivel) && nivel.getBiome(jogador.blockPosition()).is(Bosques.BIOMA)) {
            Sanidade.somar(jogador, 1.0F);
        }
        // -1 a cada 5 minutos sem fazer maldade (a maldade zera o relogio em registrarMaldade)
        if (tempo % 20L == 0L) {
            long ultima = jogador.getPersistentData().getLong("csmp_ultima_maldade");
            if (tempo - ultima >= 6000L && (tempo - ultima) % 6000L < 20L) {
                Sanidade.somar(jogador, -1.0F);
            }
        }
        if (tempo % 100L == 0L) {
            Paranoia.tick(jogador, tempo);
        }
        // o cliente so precisa saber a insanidade para desenhar os efeitos na tela
        if (tempo % 40L == 0L) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(jogador, new SanidadePayload(Sanidade.obter(jogador)));
        }
        // insanidade 50 ou mais: sons falsos (passos, portas) que so o proprio jogador ouve, de vez em quando
        if (tempo % 400L == 0L && Sanidade.obter(jogador) >= 50.0F && jogador.getRandom().nextFloat() < 0.5F) {
            Paranoia.somFalso(jogador, 9.0D);
        }
    }

    private static void registrarMaldade(ServerPlayer jogador) {
        jogador.getPersistentData().putLong("csmp_ultima_maldade", jogador.level().getGameTime());
    }

    @SubscribeEvent
    public static void aoMatar(LivingDeathEvent evento) {
        LivingEntity morto = evento.getEntity();
        DamageSource fonte = evento.getSource();
        if (fonte.getEntity() instanceof ServerPlayer assassino && assassino != morto) {
            if (morto instanceof Player) {
                Sanidade.somar(assassino, 15.0F);
                registrarMaldade(assassino);
            } else if (morto.getType().is(NPC_BOM)) {
                Sanidade.somar(assassino, 20.0F);
                registrarMaldade(assassino);
            } else if (morto.getType().is(SCULK_FORTE)) {
                Sanidade.somar(assassino, -10.0F);
            } else if (morto.getType().is(SCULK_FRACO)) {
                Sanidade.somar(assassino, -2.0F);
            } else if (morto instanceof net.minecraft.world.entity.monster.Enemy) {
                Sanidade.somar(assassino, -0.5F);
            }
        }
        if (morto instanceof ServerPlayer jogador) {
            Sanidade.somar(jogador, 5.0F);
            // a insanidade ja foi somada: decide agora se a morte leva ao Vazio
            if (!Vazio.eh(jogador.level()) && jogador.getRandom().nextDouble() < chanceDeVazio(Sanidade.obter(jogador))) {
                jogador.getPersistentData().putBoolean("csmp_cair_no_vazio", true);
            }
        }
    }

    @SubscribeEvent
    public static void aoBater(LivingIncomingDamageEvent evento) {
        if (evento.getSource().getEntity() instanceof ServerPlayer agressor
                && evento.getEntity().getType().is(NPC_BOM) && !evento.isCanceled()) {
            long tempo = agressor.level().getGameTime();
            if (tempo - agressor.getPersistentData().getLong("csmp_ultimo_golpe_npc") >= 60L) {
                agressor.getPersistentData().putLong("csmp_ultimo_golpe_npc", tempo);
                Sanidade.somar(agressor, 3.0F);
                registrarMaldade(agressor);
            }
        }
    }

    /** O estado de insanidade passa para o corpo novo depois de morrer. */
    @SubscribeEvent
    public static void aoClonar(PlayerEvent.Clone evento) {
        var antigo = evento.getOriginal().getPersistentData();
        var novo = evento.getEntity().getPersistentData();
        for (String chave : new String[] {Sanidade.CHAVE, "csmp_ultima_maldade", "csmp_paranoia", "csmp_cair_no_vazio"}) {
            if (antigo.contains(chave)) {
                novo.put(chave, antigo.get(chave).copy());
            }
        }
    }

    /** Ao renascer, se a morte sorteou o Vazio, leva o jogador para la. */
    @SubscribeEvent
    public static void aoRenascer(PlayerEvent.PlayerRespawnEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador)) {
            return;
        }
        var dados = jogador.getPersistentData();
        if (dados.getBoolean("csmp_cair_no_vazio")) {
            dados.putBoolean("csmp_cair_no_vazio", false);
            ServerLevel vazio = jogador.server.getLevel(Vazio.DIMENSAO);
            if (vazio != null) {
                BlockPos destino = VazioEntrada.pontoDeEntrada(vazio, jogador);
                jogador.teleportTo(vazio, destino.getX() + 0.5D, destino.getY(), destino.getZ() + 0.5D, jogador.getYRot(), 0.0F);
                jogador.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "Voce abre os olhos. Nao e o seu mundo. Voce esta preso.")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC));
            }
        }
    }
}
