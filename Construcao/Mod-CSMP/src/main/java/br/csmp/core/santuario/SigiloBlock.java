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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Simbolo no chao do Vazio, no centro do circulo de ritual (no lugar da antiga estatua do Vazio). Fica apagado ate
 * existir uma estatua completa no overworld ao alcance; entao acende e sobe uma luz de farol amarela e transparente.
 * Quem esta preso precisa ficar a ate 16 blocos de um farol aceso para aparecer no menu de quem vai revive-lo.
 */
public class SigiloBlock extends Block implements EntityBlock {
    public static final BooleanProperty ATIVO = BooleanProperty.create("ativo");

    public SigiloBlock(Properties propriedades) {
        super(propriedades);
        this.registerDefaultState(this.stateDefinition.any().setValue(ATIVO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> construtor) {
        construtor.add(ATIVO);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new SigiloBE(pos, estado);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack item, BlockState estado, Level nivel, BlockPos pos,
                                              Player jogador, InteractionHand mao, BlockHitResult acerto) {
        if (mao == InteractionHand.MAIN_HAND && !nivel.isClientSide && nivel instanceof ServerLevel servidor
                && jogador instanceof ServerPlayer sp) {
            ServerSantuario.clicarSigilo(servidor, pos, estado, sp);
        }
        return ItemInteractionResult.sidedSuccess(nivel.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level nivel, BlockPos pos, Player jogador,
                                               BlockHitResult acerto) {
        if (!nivel.isClientSide && nivel instanceof ServerLevel servidor && jogador instanceof ServerPlayer sp) {
            ServerSantuario.clicarSigilo(servidor, pos, estado, sp);
        }
        return InteractionResult.sidedSuccess(nivel.isClientSide);
    }
}
