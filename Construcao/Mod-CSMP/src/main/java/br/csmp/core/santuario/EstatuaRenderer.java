package br.csmp.core.santuario;

import br.csmp.core.CsmpMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Modelo de jogador com textura de pedra; esconde as partes que ainda nao foram montadas. */
public class EstatuaRenderer extends HumanoidMobRenderer<EstatuaEntity, HumanoidModel<EstatuaEntity>> {
    private static final ResourceLocation TEXTURA =
            ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "textures/entity/estatua.png");

    public EstatuaRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new HumanoidModel<>(contexto.bakeLayer(ModelLayers.PLAYER)), 0.0F);
    }

    @Override
    public void render(EstatuaEntity entidade, float guinada, float parcial, PoseStack pilha,
                       MultiBufferSource buffers, int luz) {
        int p = entidade.partes();
        HumanoidModel<EstatuaEntity> modelo = this.getModel();
        modelo.head.visible = (p & EstatuaEntity.CABECA) != 0;
        modelo.hat.visible = (p & EstatuaEntity.CABECA) != 0;
        modelo.body.visible = (p & EstatuaEntity.TORSO) != 0;
        modelo.rightArm.visible = (p & EstatuaEntity.BRACOS) != 0;
        modelo.leftArm.visible = (p & EstatuaEntity.BRACOS) != 0;
        modelo.rightLeg.visible = (p & EstatuaEntity.PERNAS) != 0;
        modelo.leftLeg.visible = (p & EstatuaEntity.PERNAS) != 0;
        if (p != 0) {
            super.render(entidade, guinada, parcial, pilha, buffers, luz);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EstatuaEntity entidade) {
        return TEXTURA;
    }
}
