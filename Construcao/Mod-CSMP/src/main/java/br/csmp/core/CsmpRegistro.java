package br.csmp.core;

import br.csmp.core.mob.MonstroCorrupto;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tudo que o mod csmp registra (entidades e itens). */
public final class CsmpRegistro {
    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CsmpMod.ID);
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(CsmpMod.ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MonstroCorrupto>> MONSTRO_CORRUPTO =
            ENTIDADES.register("monstro_corrupto", () -> EntityType.Builder
                    .of(MonstroCorrupto::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.4F)
                    .eyeHeight(2.6F)
                    .clientTrackingRange(10)
                    .build("monstro_corrupto"));

    public static final DeferredItem<DeferredSpawnEggItem> OVO_MONSTRO_CORRUPTO =
            ITENS.register("monstro_corrupto_spawn_egg",
                    () -> new DeferredSpawnEggItem(MONSTRO_CORRUPTO, 0x2A0A3A, 0xB01030, new Item.Properties()));

    private CsmpRegistro() {}
}