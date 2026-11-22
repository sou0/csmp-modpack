package br.csmp.core;

import br.csmp.core.mob.MonstroCorrupto;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tudo que o mod csmp registra (blocos, entidades e itens). */
public final class CsmpRegistro {
    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CsmpMod.ID);
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(CsmpMod.ID);
    public static final DeferredRegister.Blocks BLOCOS = DeferredRegister.createBlocks(CsmpMod.ID);

    // ---- Dimensao Vazio ----
    /** Chao branco que brilha um pouco; parece o auroracite, mas e solido sem precisar de item. */
    public static final DeferredBlock<Block> CHAO_VAZIO = BLOCOS.registerSimpleBlock("chao_vazio",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(estado -> 7)
                    .requiresCorrectToolForDrops());

    public static final DeferredBlock<RotatedPillarBlock> TRONCO_NEGRO = BLOCOS.register("tronco_negro",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG)
                    .mapColor(MapColor.COLOR_BLACK)));

    public static final DeferredBlock<LeavesBlock> FOLHAS_NEGRAS = BLOCOS.register("folhas_negras",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES)
                    .mapColor(MapColor.COLOR_BLACK)));

    public static final DeferredItem<BlockItem> ITEM_CHAO_VAZIO = ITENS.registerSimpleBlockItem(CHAO_VAZIO);
    public static final DeferredItem<BlockItem> ITEM_TRONCO_NEGRO = ITENS.registerSimpleBlockItem(TRONCO_NEGRO);
    public static final DeferredItem<BlockItem> ITEM_FOLHAS_NEGRAS = ITENS.registerSimpleBlockItem(FOLHAS_NEGRAS);

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