package br.csmp.core.santuario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Estatuas do overworld que estao completas (todas as partes montadas). Fica salvo no mundo porque o simbolo do Vazio
 * precisa saber, mesmo com a estatua longe e descarregada, se acende o farol.
 */
public final class EstatuasProntas extends SavedData {
    private static final String NOME = "csmp_estatuas_prontas";
    private final Set<BlockPos> posicoes = new HashSet<>();

    private static final SavedData.Factory<EstatuasProntas> FABRICA =
            new SavedData.Factory<>(EstatuasProntas::new, EstatuasProntas::carregar, null);

    private static EstatuasProntas carregar(CompoundTag tag, HolderLookup.Provider registros) {
        EstatuasProntas dados = new EstatuasProntas();
        for (long l : tag.getLongArray("Posicoes")) {
            dados.posicoes.add(BlockPos.of(l));
        }
        return dados;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
        tag.put("Posicoes", new LongArrayTag(this.posicoes.stream().mapToLong(BlockPos::asLong).toArray()));
        return tag;
    }

    private static EstatuasProntas de(MinecraftServer servidor) {
        return servidor.overworld().getDataStorage().computeIfAbsent(FABRICA, NOME);
    }

    public static synchronized List<BlockPos> todas(MinecraftServer servidor) {
        return List.copyOf(de(servidor).posicoes);
    }

    public static synchronized void marcar(MinecraftServer servidor, BlockPos pos, boolean pronta) {
        EstatuasProntas dados = de(servidor);
        boolean mudou = pronta ? dados.posicoes.add(pos.immutable()) : dados.posicoes.remove(pos);
        if (mudou) {
            dados.setDirty();
        }
    }
}
