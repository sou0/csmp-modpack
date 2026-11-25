package br.csmp.core.santuario;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Farol do simbolo aceso: um feixe fino, amarelo-claro e bem transparente (quase so o brilho de fora), como pediu o
 * lider, para ser visto de longe na nevoa do Vazio.
 */
public class SigiloRenderer implements BlockEntityRenderer<SigiloBE> {
    /** Amarelo claro (ARGB). */
    private static final int COR = 0xFFFFEFA8;

    public SigiloRenderer(BlockEntityRendererProvider.Context contexto) {
    }

    @Override
    public void render(SigiloBE be, float parcial, PoseStack pilha, MultiBufferSource buffers, int luz, int sobreposicao) {
        if (be.getLevel() == null || !be.getBlockState().getValue(SigiloBlock.ATIVO)) {
            return;
        }
        BeaconRenderer.renderBeaconBeam(pilha, buffers, BeaconRenderer.BEAM_LOCATION, parcial, 0.6F,
                be.getLevel().getGameTime(), 1, 320, COR, 0.06F, 0.32F);
    }

    @Override
    public boolean shouldRenderOffScreen(SigiloBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(SigiloBE be, Vec3 camera) {
        return Vec3.atCenterOf(be.getBlockPos()).multiply(1.0D, 0.0D, 1.0D)
                .closerThan(camera.multiply(1.0D, 0.0D, 1.0D), getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(SigiloBE be) {
        return AABB.INFINITE;
    }
}
