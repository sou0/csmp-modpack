package br.csmp.core.sanidade;

import br.csmp.core.CsmpMod;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Animacao de arrancar a venda com as unhas (feita no Blockbench pelo lider, exportada pelo GeckoLib), tocada pelo
 * Player Animator em quem chega na paranoia maxima, para todos que estao vendo. Ha duas versoes: a normal (cabeca,
 * tronco e bracos) e uma compativel (cabeca e braco esquerdo) usada quando o resourcepack de animacoes do jogador
 * (Fresh Animations / FA+Player) esta ligado, porque ele mexe nos mesmos ossos.
 */
@EventBusSubscriber(modid = CsmpMod.ID, value = Dist.CLIENT)
public final class ClientVendaAnimacao {
    static final ResourceLocation CAMADA = ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "venda");
    private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "venda_arrancar");
    private static final ResourceLocation COMPATIVEL = ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "venda_arrancar_compat");

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("csmp");

    private ClientVendaAnimacao() {}

    private static boolean temAnimador() {
        return ModList.get().isLoaded("playeranimator");
    }

    @SubscribeEvent
    public static void preparar(FMLClientSetupEvent evento) {
        if (temAnimador()) {
            evento.enqueueWork(Real::registrarCamada);
        }
    }

    /** Pacote do servidor: o jogador com este id arrancou a venda. */
    public static void tocar(int idEntidade) {
        var nivel = Minecraft.getInstance().level;
        if (nivel == null || !temAnimador()) {
            return;
        }
        Entity entidade = nivel.getEntity(idEntidade);
        if (entidade instanceof AbstractClientPlayer jogador) {
            Real.tocar(jogador, usarCompativel() ? COMPATIVEL : NORMAL);
        }
    }

    /** O resourcepack de animacoes do jogador esta ligado? Entao usa a versao que so mexe na cabeca e num braco. */
    private static boolean usarCompativel() {
        for (String id : Minecraft.getInstance().getResourcePackRepository().getSelectedIds()) {
            String nome = id.toLowerCase(Locale.ROOT);
            if (nome.contains("freshanimations") || nome.contains("fa+player") || nome.contains("fresh animations")) {
                return true;
            }
        }
        return false;
    }

    /** Tudo que toca nas classes do Player Animator fica aqui, so carregado se o mod existir. */
    private static final class Real {
        static void registrarCamada() {
            dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(CAMADA, 1000,
                    jogador -> new dev.kosmx.playerAnim.api.layered.ModifierLayer<>());
        }

        @SuppressWarnings("unchecked")
        static void tocar(AbstractClientPlayer jogador, ResourceLocation animacao) {
            var dados = dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.getPlayerAssociatedData(jogador).get(CAMADA);
            var tocavel = dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry.getAnimation(animacao);
            if (tocavel == null) {
                LOG.warn("Animacao {} nao foi carregada pelo Player Animator", animacao);
                return;
            }
            if (dados instanceof dev.kosmx.playerAnim.api.layered.ModifierLayer<?> camada) {
                ((dev.kosmx.playerAnim.api.layered.ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation>) camada)
                        .setAnimation(tocavel.playAnimation());
            } else {
                LOG.warn("Camada {} da animacao nao existe no jogador", CAMADA);
            }
        }
    }
}
