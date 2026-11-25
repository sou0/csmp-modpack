package br.csmp.core;

import br.csmp.core.npc.VazioNpcEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** Mod proprio do CSMP: medidor de caos, clima, nuvens e mobs proprios. */
@Mod(CsmpMod.ID)
public class CsmpMod {
    public static final String ID = "csmp";

    public CsmpMod(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, CsmpConfig.ESPECIFICACAO, "csmp-client.toml");
        CsmpSons.SONS.register(modBus);
        CsmpRegistro.BLOCOS.register(modBus);
        CsmpRegistro.FEICOES.register(modBus);
        CsmpRegistro.ENTIDADES.register(modBus);
        CsmpRegistro.BLOCOS_ENTIDADE.register(modBus);
        CsmpRegistro.ITENS.register(modBus);
        modBus.addListener(CsmpNetwork::register);
        modBus.addListener(CsmpMod::atributos);
        modBus.addListener(CsmpMod::abas);
        modBus.addListener(CsmpMod::posicoesDeSpawn);
        NeoForge.EVENT_BUS.addListener(ServerCaos::onServerTick);
        NeoForge.EVENT_BUS.addListener(ServerCaos::onLogin);
        NeoForge.EVENT_BUS.addListener(br.csmp.core.vazio.ServerLuzes::onServerTick);
        NeoForge.EVENT_BUS.addListener(br.csmp.core.vazio.ServerLuzes::onChat);
        NeoForge.EVENT_BUS.addListener(br.csmp.core.santuario.ServerSantuario::onServerTick);
        NeoForge.EVENT_BUS.addListener(br.csmp.core.santuario.ServerSantuario::comandos);
        NeoForge.EVENT_BUS.addListener(br.csmp.core.mundo.MundoComandos::comandos);
    }

    private static void atributos(EntityAttributeCreationEvent event) {
        event.put(CsmpRegistro.MONSTRO_CORRUPTO.get(), br.csmp.core.mob.MonstroCorrupto.criarAtributos().build());
        event.put(CsmpRegistro.ESTATUA.get(), br.csmp.core.santuario.EstatuaEntity.criarAtributos().build());
        event.put(CsmpRegistro.PROFETA.get(), br.csmp.core.npc.ProfetaEntity.criarAtributos().build());
        event.put(CsmpRegistro.SOMBRA.get(), br.csmp.core.npc.VazioNpcEntity.criarAtributos(0.0D).build());
        event.put(CsmpRegistro.CRIANCA.get(), br.csmp.core.npc.VazioNpcEntity.criarAtributos(0.22D).build());
        event.put(CsmpRegistro.SENTINELA.get(), br.csmp.core.npc.VazioNpcEntity.criarAtributos(0.0D).build());
    }

    /** Os NPCs do Vazio so nascem sobre o chao do Vazio. */
    private static void posicoesDeSpawn(RegisterSpawnPlacementsEvent event) {
        registrarSpawn(event, CsmpRegistro.SOMBRA.get());
        registrarSpawn(event, CsmpRegistro.CRIANCA.get());
        registrarSpawn(event, CsmpRegistro.SENTINELA.get());
    }

    private static void registrarSpawn(RegisterSpawnPlacementsEvent event, EntityType<VazioNpcEntity> tipo) {
        event.register(tipo, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, nivel, motivo, pos, aleatorio) -> nivel.getBlockState(pos.below()).is(CsmpRegistro.CHAO_VAZIO.get()),
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    private static void abas(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CsmpRegistro.OVO_MONSTRO_CORRUPTO.get());
            event.accept(CsmpRegistro.OVO_PROFETA.get());
            event.accept(CsmpRegistro.OVO_SOMBRA.get());
            event.accept(CsmpRegistro.OVO_CRIANCA.get());
            event.accept(CsmpRegistro.OVO_SENTINELA.get());
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(CsmpRegistro.ITEM_CHAO_VAZIO.get());
            event.accept(CsmpRegistro.ITEM_TERRA_NEGRA.get());
            event.accept(CsmpRegistro.ITEM_BASE_ESTATUA.get());
            event.accept(CsmpRegistro.ITEM_MINERIO_SCULK.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(CsmpRegistro.PECA_RACHADA.get());
            event.accept(CsmpRegistro.AMULETO.get());
            event.accept(CsmpRegistro.VENDA.get());
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CsmpRegistro.ITEM_TRONCO_NEGRO.get());
            event.accept(CsmpRegistro.ITEM_FOLHAS_NEGRAS.get());
            event.accept(CsmpRegistro.ITEM_BETULA_OLHO.get());
            event.accept(CsmpRegistro.ITEM_FOLHAS_CARMESIM.get());
        }
    }
}