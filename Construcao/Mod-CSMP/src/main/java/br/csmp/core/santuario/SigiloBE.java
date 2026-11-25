package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** So existe para o simbolo ser lembrado enquanto esta carregado e para o cliente desenhar o farol. */
public class SigiloBE extends BlockEntity {

    public SigiloBE(BlockPos pos, BlockState estado) {
        super(CsmpRegistro.SIGILO_BE.get(), pos, estado);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            ServerSantuario.registrarSigilo(this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            ServerSantuario.removerSigilo(this.worldPosition);
        }
    }
}
