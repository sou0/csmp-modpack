package br.csmp.core.sanidade;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A venda: vai na cabeca. Quem usa fica cego (o cliente escurece a tela; na Floresta das Betulas o contorno dos
 * blocos e os mobs aparecem). Anula o drenar de sanidade dos olhos das betulas e aumenta a paranoia.
 * Depois de arrancada na paranoia maxima, nao pode ser usada por 5 minutos.
 */
public class VendaItem extends Item implements Equipable {

    public VendaItem(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<net.minecraft.sounds.SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jogador, InteractionHand mao) {
        if (jogador instanceof ServerPlayer sp && Paranoia.bloqueada(sp, nivel.getGameTime())) {
            sp.displayClientMessage(Component.literal("Suas maos ainda tremem. Voce nao consegue vendar os olhos agora."), true);
            return InteractionResultHolder.fail(jogador.getItemInHand(mao));
        }
        return this.swapWithEquipmentSlot(this, nivel, jogador, mao);
    }
}
