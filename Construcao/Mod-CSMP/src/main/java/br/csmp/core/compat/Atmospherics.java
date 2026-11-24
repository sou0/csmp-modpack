package br.csmp.core.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Ponte com o Create: Atmospherics (mod id aeronauticswinds). So toca nas classes dele se o mod estiver carregado;
 * sem ele, nada aqui roda.
 */
public final class Atmospherics {
    private static Boolean presente;

    private Atmospherics() {}

    public static boolean presente() {
        if (presente == null) {
            presente = ModList.get() != null && ModList.get().isLoaded("aeronauticswinds");
        }
        return presente;
    }

    /** Um bloco mudou: descarta o cache da costa do chunk (e dos vizinhos) para a agua ser redesenhada certa. */
    public static void blocoMudou(Level nivel, BlockPos pos) {
        int y = pos.getY();
        if (y < 30 || y > 90) {
            return; // a costa so importa perto do nivel do mar
        }
        try {
            Real.invalidar(nivel, pos.getX() >> 4, pos.getZ() >> 4);
        } catch (LinkageError mudou) {
            presente = false; // outra versao do mod, sem esse metodo: para de tentar
        }
    }

    /** Separado para a classe do Atmospherics so ser carregada quando o mod existe. */
    private static final class Real {
        static void invalidar(Level nivel, int chunkX, int chunkZ) {
            com.enzonic.aeronauticswinds.ocean.waves.CoastField.forLevel(nivel).invalidate(chunkX, chunkZ);
        }
    }
}
