package br.csmp.core;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** Mod proprio do CSMP: medidor de caos, clima, nuvens e mobs proprios. */
@Mod(CsmpMod.ID)
public class CsmpMod {
    public static final String ID = "csmp";

    public CsmpMod(IEventBus modBus) {
        CsmpRegistro.BLOCOS.register(modBus);
        CsmpRegistro.ENTIDADES.register(modBus);
        CsmpRegistro.ITENS.register(modBus);
        modBus.addListener(CsmpNetwork::register);
        modBus.addListener(CsmpMod::atributos);
        modBus.addListener(CsmpMod::abas);
        NeoForge.EVENT_BUS.addListener(ServerCaos::onServerTick);
        NeoForge.EVENT_BUS.addListener(ServerCaos::onLogin);
    }

    private static void atributos(EntityAttributeCreationEvent event) {
        event.put(CsmpRegistro.MONSTRO_CORRUPTO.get(), br.csmp.core.mob.MonstroCorrupto.criarAtributos().build());
    }

    private static void abas(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CsmpRegistro.OVO_MONSTRO_CORRUPTO.get());
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(CsmpRegistro.ITEM_CHAO_VAZIO.get());
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CsmpRegistro.ITEM_TRONCO_NEGRO.get());
            event.accept(CsmpRegistro.ITEM_FOLHAS_NEGRAS.get());
        }
    }
}