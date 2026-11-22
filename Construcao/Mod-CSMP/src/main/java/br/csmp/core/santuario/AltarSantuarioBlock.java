package br.csmp.core.santuario;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Altar do santuario. E o mesmo bloco nas duas dimensoes; o que ele faz depende de onde esta:
 * no Vazio, quem esta preso chama ajuda; no overworld, quem quer ajudar faz a oferenda.
 */
public class AltarSantuarioBlock extends Block {

    public AltarSantuarioBlock(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack item, BlockState estado, Level nivel, BlockPos pos,
                                              Player jogador, InteractionHand mao, BlockHitResult acerto) {
        if (!nivel.isClientSide && nivel instanceof ServerLevel servidor && jogador instanceof ServerPlayer sp) {
            ServerSantuario.interagir(servidor, pos, sp, item, mao);
        }
        return ItemInteractionResult.sidedSuccess(nivel.isClientSide);
    }
}
