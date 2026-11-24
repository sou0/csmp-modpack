package br.csmp.core.santuario;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Se os rituais ja foram revelados aos jogadores (pela planilha: so admins revelam, por evento ou pela lore).
 * Enquanto nao forem, as estatuas do overworld ficam em silencio: nao aceitam itens nem ativam. Fica salvo no mundo.
 */
public final class RituaisRevelados extends SavedData {
    private static final String NOME = "csmp_rituais";
    private boolean revelados;

    private static final SavedData.Factory<RituaisRevelados> FABRICA =
            new SavedData.Factory<>(RituaisRevelados::new, RituaisRevelados::carregar, null);

    private static RituaisRevelados carregar(CompoundTag tag, HolderLookup.Provider registros) {
        RituaisRevelados dados = new RituaisRevelados();
        dados.revelados = tag.getBoolean("Revelados");
        return dados;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
        tag.putBoolean("Revelados", this.revelados);
        return tag;
    }

    private static RituaisRevelados de(MinecraftServer servidor) {
        return servidor.overworld().getDataStorage().computeIfAbsent(FABRICA, NOME);
    }

    public static boolean revelados(MinecraftServer servidor) {
        return de(servidor).revelados;
    }

    public static void definir(MinecraftServer servidor, boolean valor) {
        RituaisRevelados dados = de(servidor);
        dados.revelados = valor;
        dados.setDirty();
    }
}
