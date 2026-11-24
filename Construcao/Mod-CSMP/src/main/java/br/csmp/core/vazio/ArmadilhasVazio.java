package br.csmp.core.vazio;

import br.csmp.core.CsmpMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Efeito do fio das armadilhas do Vazio: quem encosta fica cego e quase parado por 5 segundos. */
@EventBusSubscriber(modid = CsmpMod.ID)
public final class ArmadilhasVazio {
    private static final String ULTIMA = "csmp_ultimo_fio";

    private ArmadilhasVazio() {}

    @SubscribeEvent
    public static void aoTick(PlayerTickEvent.Post evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || jogador.isSpectator() || jogador.isCreative()
                || !Vazio.eh(jogador.level())) {
            return;
        }
        if (!jogador.level().getBlockState(jogador.blockPosition()).is(Blocks.TRIPWIRE)) {
            return;
        }
        long tempo = jogador.level().getGameTime();
        if (tempo - jogador.getPersistentData().getLong(ULTIMA) < 80L) {
            return;
        }
        jogador.getPersistentData().putLong(ULTIMA, tempo);
        jogador.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        jogador.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F);
    }
}
