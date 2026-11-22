package br.csmp.core.vazio;

import br.csmp.core.CsmpMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Identificacao da dimensao Vazio (csmp:vazio). */
public final class Vazio {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "vazio");
    public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, ID);

    private Vazio() {}

    public static boolean eh(Level nivel) {
        return nivel != null && nivel.dimension().equals(DIMENSAO);
    }
}
