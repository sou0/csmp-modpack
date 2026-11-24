package br.csmp.core.santuario;

import br.csmp.core.CsmpRegistro;
import br.csmp.core.vazio.Vazio;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Memoria da base: quais itens ja foram colocados, na ordem. Nao diz se esta certo: isso so e conferido
 * quando a estatua e ativada.
 */
public class BaseEstatuaBE extends BlockEntity {
    private final List<String> itens = new ArrayList<>();

    public BaseEstatuaBE(BlockPos pos, BlockState estado) {
        super(CsmpRegistro.BASE_ESTATUA_BE.get(), pos, estado);
    }

    public List<String> itens() {
        return this.itens;
    }

    public void adicionar(String id) {
        this.itens.add(id);
        this.setChanged();
    }

    public void limpar() {
        this.itens.clear();
        this.setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && !Vazio.eh(this.level)) {
            ServerSantuario.registrarBase(this.level.dimension(), this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            ServerSantuario.removerBase(this.level.dimension(), this.worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registros) {
        super.saveAdditional(tag, registros);
        ListTag lista = new ListTag();
        for (String id : this.itens) {
            lista.add(StringTag.valueOf(id));
        }
        tag.put("Itens", lista);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registros) {
        super.loadAdditional(tag, registros);
        this.itens.clear();
        for (Tag t : tag.getList("Itens", Tag.TAG_STRING)) {
            this.itens.add(t.getAsString());
        }
    }
}
