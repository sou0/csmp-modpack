package br.csmp.core.npc;

import br.csmp.core.CsmpMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Visual provisorio do Profeta (modelo humanoide); o modelo definitivo vem do escritor. */
public class ProfetaRenderer extends HumanoidMobRenderer<ProfetaEntity, HumanoidModel<ProfetaEntity>> {
    private static final ResourceLocation TEXTURA =
            ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "textures/entity/profeta.png");

    public ProfetaRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new HumanoidModel<>(contexto.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ProfetaEntity entidade) {
        return TEXTURA;
    }
}
