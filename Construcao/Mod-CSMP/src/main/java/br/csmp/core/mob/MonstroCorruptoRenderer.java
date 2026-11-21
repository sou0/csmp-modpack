package br.csmp.core.mob;

import br.csmp.core.CsmpMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MonstroCorruptoRenderer extends MobRenderer<MonstroCorrupto, MonstroCorruptoModel> {
    private static final ResourceLocation TEXTURA =
            ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "textures/entity/monstro_corrupto.png");

    public MonstroCorruptoRenderer(EntityRendererProvider.Context context) {
        super(context, new MonstroCorruptoModel(context.bakeLayer(MonstroCorruptoModel.LAYER)), 2.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(MonstroCorrupto entity) {
        return TEXTURA;
    }
}