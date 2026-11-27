package br.csmp.core.sanidade;

import br.csmp.core.CsmpSons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Paranoia (invisivel, 0 a 100): sobe 1 por segundo com a venda e desce 1 a cada 2 s sem ela.
 *  50 ou mais: sons falsos (so o jogador ouve). 75 ou mais: ele mesmo escuta melhor (sons falsos mais frequentes
 *  e mais perto). 100: arranca a venda com as unhas, leva 2 coracoes de dano e nao pode usa-la por 5 minutos.
 * Chamado a cada 100 ticks (5 s) por jogador, entao cada chamada anda 5 segundos de paranoia.
 */
public final class Paranoia {
    public static final String CHAVE = "csmp_paranoia";
    public static final String BLOQUEIO = "csmp_venda_bloqueada_ate";
    private static final long BLOQUEIO_TICKS = 20L * 60L * 5L;
    /** Momento do puxao na animacao (cerca de 2 s depois de comecar). */
    private static final long TICKS_ATE_O_PUXAO = 40L;
    private static final String ARRANCAR_EM = "csmp_arrancar_venda_em";

    private Paranoia() {}

    public static float obter(ServerPlayer jogador) {
        return jogador.getPersistentData().getFloat(CHAVE);
    }

    public static void definir(ServerPlayer jogador, float valor) {
        jogador.getPersistentData().putFloat(CHAVE, Math.max(0.0F, Math.min(100.0F, valor)));
    }

    public static boolean bloqueada(ServerPlayer jogador, long tempo) {
        return tempo < jogador.getPersistentData().getLong(BLOQUEIO);
    }

    static void tick(ServerPlayer jogador, long tempo) {
        boolean usando = ServerSanidade.temVenda(jogador);
        float p = obter(jogador);
        p = usando ? p + 5.0F : p - 2.5F;
        definir(jogador, p);
        p = obter(jogador);
        if (usando && p >= 100.0F && !jogador.getPersistentData().contains(ARRANCAR_EM)) {
            // comeca a animacao (todos que estao vendo) e marca o puxao para quando as maos chegam na venda
            jogador.getPersistentData().putLong(ARRANCAR_EM, tempo + TICKS_ATE_O_PUXAO);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(jogador,
                    new VendaAnimPayload(jogador.getId()));
            return;
        }
        if (p >= 50.0F) {
            float chance = p >= 75.0F ? 0.55F : 0.25F;
            if (jogador.getRandom().nextFloat() < chance) {
                somFalso(jogador, p >= 75.0F ? 5.0D : 9.0D);
            }
        }
    }

    /** Chamado todo tick: na hora do puxao, a venda sai da cabeca, machuca e fica bloqueada por 5 minutos. */
    static void puxao(ServerPlayer jogador, long tempo) {
        var dados = jogador.getPersistentData();
        if (!dados.contains(ARRANCAR_EM) || tempo < dados.getLong(ARRANCAR_EM)) {
            return;
        }
        dados.remove(ARRANCAR_EM);
        if (ServerSanidade.temVenda(jogador)) {
            jogador.setItemSlot(EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
            jogador.hurt(jogador.damageSources().generic(), 4.0F);
            jogador.level().playSound(null, jogador.blockPosition(), net.minecraft.sounds.SoundEvents.WOOL_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        dados.putLong(BLOQUEIO, tempo + BLOQUEIO_TICKS);
        definir(jogador, 60.0F);
        jogador.sendSystemMessage(Component.literal("Voce arranca a venda com as unhas.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    /** Toca um som que mais ninguem ouve, vindo de perto do jogador. */
    static void somFalso(ServerPlayer jogador, double distancia) {
        SoundEvent[] opcoes = {CsmpSons.SUSTO_PASSOS.get(), CsmpSons.SUSTO_CORACAO.get(),
                net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN, net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT,
                net.minecraft.sounds.SoundEvents.SKELETON_AMBIENT, net.minecraft.sounds.SoundEvents.STONE_STEP};
        SoundEvent som = opcoes[jogador.getRandom().nextInt(opcoes.length)];
        double angulo = jogador.getRandom().nextDouble() * Math.PI * 2.0D;
        // enviado so para este jogador (Player nulo = todos, entao usa playNotifySound com o jogador como alvo)
        jogador.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(som), SoundSource.AMBIENT,
                jogador.getX() + Math.cos(angulo) * distancia, jogador.getY(), jogador.getZ() + Math.sin(angulo) * distancia,
                0.8F, 0.85F + jogador.getRandom().nextFloat() * 0.3F, jogador.getRandom().nextLong()));
    }
}
