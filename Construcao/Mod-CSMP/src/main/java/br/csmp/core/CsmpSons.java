package br.csmp.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sons proprios do CSMP. Por enquanto cada um e um REMIX de sons do jogo (mais graves e lentos),
 * definido em assets/csmp/sounds.json. Para usar musica/audio de verdade, basta colocar os arquivos
 * .ogg em assets/csmp/sounds/ e apontar o nome no sounds.json.
 */
public final class CsmpSons {
    public static final DeferredRegister<SoundEvent> SONS = DeferredRegister.create(Registries.SOUND_EVENT, CsmpMod.ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENTE = som("vazio.ambiente");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUMOR = som("vazio.humor");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUSSURRO = som("vazio.sussurro");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRILHA = som("vazio.trilha");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUSTO_CORACAO = som("vazio.susto.coracao");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUSTO_GRITO = som("vazio.susto.grito");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUSTO_PASSOS = som("vazio.susto.passos");
    public static final DeferredHolder<SoundEvent, SoundEvent> NPC_MURMURIO = som("npc.murmurio");
    public static final DeferredHolder<SoundEvent, SoundEvent> SANTUARIO_CHAMADO = som("santuario.chamado");
    public static final DeferredHolder<SoundEvent, SoundEvent> SANTUARIO_TROCA = som("santuario.troca");

    private CsmpSons() {}

    private static DeferredHolder<SoundEvent, SoundEvent> som(String nome) {
        return SONS.register(nome, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, nome)));
    }
}
