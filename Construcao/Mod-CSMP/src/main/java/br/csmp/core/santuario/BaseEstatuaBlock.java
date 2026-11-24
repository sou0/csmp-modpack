package br.csmp.core.santuario;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Base da estatua: o bloco onde se monta (overworld) ou se ativa (Vazio) a estatua. E o mesmo bloco nas
 * duas dimensoes; quem decide o que acontece e o {@link ServerSantuario}.
 */
public class BaseEstatuaBlock extends Block implements EntityBlock {

    public BaseEstatuaBlock(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new BaseEstatuaBE(pos, estado);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack item, BlockState estado, Level nivel, BlockPos pos,
                                              Player jogador, InteractionHand mao, BlockHitResult acerto) {
        if (mao == InteractionHand.MAIN_HAND && !nivel.isClientSide && nivel instanceof ServerLevel servidor
                && jogador instanceof ServerPlayer sp) {
            ServerSantuario.clicarBase(servidor, pos, sp, item);
        }
        return ItemInteractionResult.sidedSuccess(nivel.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jogador,
                                               BlockHitResult acerto) {
        if (!nivel.isClientSide && nivel instanceof ServerLevel servidor && jogador instanceof ServerPlayer sp) {
            ServerSantuario.clicarBase(servidor, pos, sp, ItemStack.EMPTY);
        }
        return InteractionResult.sidedSuccess(nivel.isClientSide);
    }
}
