package br.csmp.core;

import br.csmp.core.mob.MonstroCorrupto;
import br.csmp.core.mundo.PontoSculkFeature;
import br.csmp.core.npc.ProfetaEntity;
import br.csmp.core.santuario.BaseEstatuaBE;
import br.csmp.core.santuario.BaseEstatuaBlock;
import br.csmp.core.santuario.EstatuaEntity;
import br.csmp.core.santuario.EstatuaFeature;
import br.csmp.core.npc.VazioNpcEntity;
import br.csmp.core.vazio.EstruturaVazioFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
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
    public static final DeferredRegister<Feature<?>> FEICOES =
            DeferredRegister.create(Registries.FEATURE, CsmpMod.ID);

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCOS_ENTIDADE =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CsmpMod.ID);

    /** Cemiterio com a estatua (mesma construcao nas duas dimensoes, blocos diferentes). */
    /** A floresta das betulas indoceis (bioma pintado chunk a chunk). */
    public static final DeferredHolder<Feature<?>, br.csmp.core.mundo.BosqueFeature> BOSQUE_FEATURE =
            FEICOES.register("bosque", () -> new br.csmp.core.mundo.BosqueFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, EstatuaFeature> ESTATUA_FEATURE =
            FEICOES.register("estatua", () -> new EstatuaFeature(EstatuaFeature.Config.CODEC));

    /** Estruturas soltas do Vazio (poste, portas, casinha, escada, sala, monolito, balanco, fonte). */
    public static final DeferredHolder<Feature<?>, EstruturaVazioFeature> ESTRUTURA_VAZIO =
            FEICOES.register("estrutura_vazio", () -> new EstruturaVazioFeature(EstruturaVazioFeature.Config.CODEC));

    /** Ponto de sculk que seca arvores e grama em volta. */
    public static final DeferredHolder<Feature<?>, PontoSculkFeature> PONTO_SCULK =
            FEICOES.register("ponto_sculk", () -> new PontoSculkFeature(PontoSculkFeature.Config.CODEC));

    // ---- Dimensao Vazio ----
    /**
     * Chao de folhas vermelhas por cima da terra preta. Emite uma luz fraca: o Vazio e iluminado "de baixo",
     * e o bloco e escuro, entao o chao em si nao parece brilhar.
     */
    public static final DeferredBlock<Block> CHAO_VAZIO = BLOCOS.register("chao_vazio",
            () -> new br.csmp.core.vazio.ChaoVazioBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.5F, 6.0F)
                    .sound(br.csmp.core.vazio.ChaoVazioBlock.tipoDeSom())
                    .lightLevel(estado -> estado.getValue(br.csmp.core.vazio.ChaoVazioBlock.POWERED) ? 9 : 6)));

    /**
     * Chao falso das armadilhas do Vazio: igual ao chao (mesmo modelo e som), mas sem colisao. Quem pisa cai.
     * Nao tem item nem drop.
     */
    public static final DeferredBlock<Block> CHAO_FALSO = BLOCOS.register("chao_falso",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.5F, 6.0F)
                    .noCollission()
                    .sound(br.csmp.core.vazio.ChaoVazioBlock.tipoDeSom())
                    .lightLevel(estado -> 6)
                    .noLootTable()));

    /** Terra preta: as camadas debaixo do chao do Vazio. */
    public static final DeferredBlock<Block> TERRA_NEGRA = BLOCOS.registerSimpleBlock("terra_negra",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0F, 6.0F)
                    .sound(SoundType.GRAVEL));

    public static final DeferredBlock<RotatedPillarBlock> TRONCO_NEGRO = BLOCOS.register("tronco_negro",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG)
                    .mapColor(MapColor.COLOR_BLACK)));

    public static final DeferredBlock<LeavesBlock> FOLHAS_NEGRAS = BLOCOS.register("folhas_negras",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES)
                    .mapColor(MapColor.COLOR_BLACK)));

    /** Base da estatua (mesmo bloco nas duas dimensoes). Inquebravel em sobrevivencia. */
    public static final DeferredBlock<BaseEstatuaBlock> BASE_ESTATUA = BLOCOS.register("base_estatua",
            () -> new BaseEstatuaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(estado -> 7)
                    .noLootTable()));

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<BaseEstatuaBE>> BASE_ESTATUA_BE =
            BLOCOS_ENTIDADE.register("base_estatua", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(BaseEstatuaBE::new, BASE_ESTATUA.get()).build(null));

    /** Tronco de betula com olho (abre quando um jogador chega perto). */
    public static final DeferredBlock<br.csmp.core.mundo.BetulaOlhoBlock> BETULA_OLHO = BLOCOS.register("betula_olho",
            () -> new br.csmp.core.mundo.BetulaOlhoBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG)));
    public static final DeferredItem<BlockItem> ITEM_BETULA_OLHO = ITENS.registerSimpleBlockItem(BETULA_OLHO);

    /** Minerio sculk: carrega o artefato (so o item por enquanto; nasce nas cavernas com sculk). */
    public static final DeferredBlock<Block> MINERIO_SCULK = BLOCOS.registerSimpleBlock("minerio_sculk",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.SCULK));

    public static final DeferredItem<BlockItem> ITEM_BASE_ESTATUA = ITENS.registerSimpleBlockItem(BASE_ESTATUA);
    public static final DeferredItem<BlockItem> ITEM_MINERIO_SCULK = ITENS.registerSimpleBlockItem(MINERIO_SCULK);
    public static final DeferredItem<Item> PECA_RACHADA = ITENS.registerSimpleItem("peca_rachada");
    public static final DeferredItem<br.csmp.core.sanidade.VendaItem> VENDA = ITENS.register("venda",
            () -> new br.csmp.core.sanidade.VendaItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> AMULETO = ITENS.registerItem("amuleto",
            propriedades -> new Item(propriedades.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<BlockItem> ITEM_CHAO_VAZIO = ITENS.registerSimpleBlockItem(CHAO_VAZIO);
    public static final DeferredItem<BlockItem> ITEM_TERRA_NEGRA = ITENS.registerSimpleBlockItem(TERRA_NEGRA);
    public static final DeferredItem<BlockItem> ITEM_TRONCO_NEGRO = ITENS.registerSimpleBlockItem(TRONCO_NEGRO);
    public static final DeferredItem<BlockItem> ITEM_FOLHAS_NEGRAS = ITENS.registerSimpleBlockItem(FOLHAS_NEGRAS);

    public static final DeferredHolder<EntityType<?>, EntityType<MonstroCorrupto>> MONSTRO_CORRUPTO =
            ENTIDADES.register("monstro_corrupto", () -> EntityType.Builder
                    .of(MonstroCorrupto::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.4F)
                    .eyeHeight(2.6F)
                    .clientTrackingRange(10)
                    .build("monstro_corrupto"));

    // ---- NPCs do Vazio (estilo Yume Nikki) ----
    public static final DeferredHolder<EntityType<?>, EntityType<VazioNpcEntity>> SOMBRA =
            ENTIDADES.register("sombra", () -> EntityType.Builder
                    .of(VazioNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).eyeHeight(1.7F).clientTrackingRange(10).build("sombra"));
    public static final DeferredHolder<EntityType<?>, EntityType<VazioNpcEntity>> CRIANCA =
            ENTIDADES.register("crianca", () -> EntityType.Builder
                    .of(VazioNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.4F, 1.3F).eyeHeight(1.1F).clientTrackingRange(10).build("crianca"));
    public static final DeferredHolder<EntityType<?>, EntityType<VazioNpcEntity>> SENTINELA =
            ENTIDADES.register("sentinela", () -> EntityType.Builder
                    .of(VazioNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 2.9F).eyeHeight(2.6F).clientTrackingRange(12).build("sentinela"));

    public static final DeferredItem<DeferredSpawnEggItem> OVO_SOMBRA =
            ITENS.register("sombra_spawn_egg",
                    () -> new DeferredSpawnEggItem(SOMBRA, 0x0A0A0E, 0xEDEDED, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_CRIANCA =
            ITENS.register("crianca_spawn_egg",
                    () -> new DeferredSpawnEggItem(CRIANCA, 0xE8D6C8, 0x961E28, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_SENTINELA =
            ITENS.register("sentinela_spawn_egg",
                    () -> new DeferredSpawnEggItem(SENTINELA, 0x5A5A62, 0xC81818, new Item.Properties()));

    /** A estatua de pedra do ritual. */
    public static final DeferredHolder<EntityType<?>, EntityType<EstatuaEntity>> ESTATUA =
            ENTIDADES.register("estatua", () -> EntityType.Builder
                    .of(EstatuaEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(16)
                    .build("estatua"));

    // ---- NPC Profeta ----
    public static final DeferredHolder<EntityType<?>, EntityType<ProfetaEntity>> PROFETA =
            ENTIDADES.register("profeta", () -> EntityType.Builder
                    .of(ProfetaEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("profeta"));

    public static final DeferredItem<DeferredSpawnEggItem> OVO_PROFETA =
            ITENS.register("profeta_spawn_egg",
                    () -> new DeferredSpawnEggItem(PROFETA, 0xE8D9A8, 0x5B3A8C, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> OVO_MONSTRO_CORRUPTO =
            ITENS.register("monstro_corrupto_spawn_egg",
                    () -> new DeferredSpawnEggItem(MONSTRO_CORRUPTO, 0x2A0A3A, 0xB01030, new Item.Properties()));

    private CsmpRegistro() {}
}