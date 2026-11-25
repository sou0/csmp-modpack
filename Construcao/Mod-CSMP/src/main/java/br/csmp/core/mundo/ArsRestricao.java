package br.csmp.core.mundo;

import br.csmp.core.CsmpMod;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Magia do Ars Nouveau (e dos addons do pack) e so para jogadores escolhidos. Ninguem usa os itens nem os blocos
 * desses mods ate um admin liberar com /csmp ars liberar &lt;jogador&gt;. O bloqueio e no servidor, entao vale para
 * todos os jeitos de usar (clique no ar, num bloco, numa entidade ou atacar com o item).
 */
@EventBusSubscriber(modid = CsmpMod.ID)
public final class ArsRestricao {
    public static final String LIBERADO = "csmp_ars_liberado";
    private static final Set<String> MODS = Set.of("ars_nouveau", "ars_additions", "ars_ocultas", "ars_elemental", "ars_technica");
    private static final String ULTIMO_AVISO = "csmp_ars_aviso";

    private ArsRestricao() {}

    public static boolean liberado(Player jogador) {
        return jogador.getPersistentData().getBoolean(LIBERADO);
    }

    private static boolean deArs(ItemStack item) {
        return !item.isEmpty() && MODS.contains(BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace());
    }

    private static boolean deArs(BlockState estado) {
        return MODS.contains(BuiltInRegistries.BLOCK.getKey(estado.getBlock()).getNamespace());
    }

    /** True se o jogador (do servidor) esta bloqueado; avisa no maximo a cada 3 segundos. */
    private static boolean bloquear(Player jogador) {
        if (!(jogador instanceof ServerPlayer sp) || liberado(sp) || sp.isCreative()) {
            return false;
        }
        long agora = sp.level().getGameTime();
        if (agora - sp.getPersistentData().getLong(ULTIMO_AVISO) > 60L) {
            sp.getPersistentData().putLong(ULTIMO_AVISO, agora);
            sp.displayClientMessage(Component.literal("Essa magia nao responde a voce.")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void usarItem(PlayerInteractEvent.RightClickItem evento) {
        if (deArs(evento.getItemStack()) && bloquear(evento.getEntity())) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void usarBloco(PlayerInteractEvent.RightClickBlock evento) {
        boolean ars = deArs(evento.getItemStack()) || deArs(evento.getLevel().getBlockState(evento.getPos()));
        if (ars && bloquear(evento.getEntity())) {
            evento.setUseBlock(TriState.FALSE);
            evento.setUseItem(TriState.FALSE);
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void usarEmEntidade(PlayerInteractEvent.EntityInteract evento) {
        if (deArs(evento.getItemStack()) && bloquear(evento.getEntity())) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void atacar(AttackEntityEvent evento) {
        if (deArs(evento.getEntity().getMainHandItem()) && bloquear(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    /** A liberacao continua depois de morrer. */
    @SubscribeEvent
    public static void aoClonar(PlayerEvent.Clone evento) {
        if (evento.getOriginal().getPersistentData().getBoolean(LIBERADO)) {
            evento.getEntity().getPersistentData().putBoolean(LIBERADO, true);
        }
    }
}
