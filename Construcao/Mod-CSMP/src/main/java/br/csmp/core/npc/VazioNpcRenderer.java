package br.csmp.core.npc;

import br.csmp.core.CsmpMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Visual provisorio dos NPCs do Vazio: humanoide com textura e tamanho proprios de cada tipo. */
public class VazioNpcRenderer extends HumanoidMobRenderer<VazioNpcEntity, HumanoidModel<VazioNpcEntity>> {

    public VazioNpcRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new HumanoidModel<>(contexto.bakeLayer(ModelLayers.PLAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(VazioNpcEntity entidade) {
        return ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "textures/entity/" + entidade.tipo() + ".png");
    }

    @Override
    protected void scale(VazioNpcEntity entidade, PoseStack pilha, float parcial) {
        float escala = switch (entidade.tipo()) {
            case "crianca" -> 0.65F;
            case "sentinela" -> 1.5F;
            default -> 1.0F;
        };
        pilha.scale(escala, escala, escala);
    }
}
