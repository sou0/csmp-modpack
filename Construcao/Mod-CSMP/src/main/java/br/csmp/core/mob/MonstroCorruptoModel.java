package br.csmp.core.mob;

import br.csmp.core.CsmpMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo do Monstro Corrupto (exportado do Blockbench e adaptado para 1.21.1:
 * nomes sem espacos, assinaturas novas e animacao basica).
 */
public class MonstroCorruptoModel extends EntityModel<MonstroCorrupto> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "monstro_corrupto"), "main");

    private final ModelPart monster;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart arm;
    private final ModelPart armL;
    private final ModelPart foreArm;
    private final ModelPart foreArmL;
    private final ModelPart leg;
    private final ModelPart legL;

    public MonstroCorruptoModel(ModelPart root) {
        this.monster = root.getChild("monster");
        ModelPart chest = this.monster.getChild("chest");
        this.arm = chest.getChild("armd");
        this.armL = chest.getChild("armd_l");
        this.foreArm = this.arm.getChild("upperarm").getChild("arm");
        this.foreArmL = this.armL.getChild("upperarm_l").getChild("arm_l");
        this.legL = chest.getChild("hip").getChild("leg__l");
        this.leg = chest.getChild("hip").getChild("leg_");
        this.neck = this.monster.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition monster = partdefinition.addOrReplaceChild("monster", CubeListBuilder.create(), PartPose.offset(-1.0F, -6.0F, 0.0F));

        PartDefinition chest = monster.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 16).addBox(-22.0F, -23.0F, -35.0F, 45.0F, 31.0F, 41.0F, new CubeDeformation(0.0F))
        .texOffs(0, 16).addBox(-12.0F, -13.0F, 6.0F, 25.0F, 16.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition spine = chest.addOrReplaceChild("spine", CubeListBuilder.create().texOffs(0, 5).addBox(-1.5F, -1.75F, 2.25F, 3.0F, 4.75F, 5.25F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -30.0F, -5.5F));

        PartDefinition neck_lomb_r1 = spine.addOrReplaceChild("neck_lomb_r1", CubeListBuilder.create().texOffs(0, 5).addBox(-2.5F, -0.75F, -2.5F, 5.0F, 4.75F, 5.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition armd = chest.addOrReplaceChild("armd", CubeListBuilder.create(), PartPose.offset(23.8796F, -7.4671F, -29.9F));

        PartDefinition upperarm = armd.addOrReplaceChild("upperarm", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5F, -6.0F, -6.0F, 23.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.2583F, -0.4826F, -0.1F));

        PartDefinition arm = upperarm.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, -7.575F, -7.25F, 26.0F, 15.15F, 14.5F, new CubeDeformation(0.0F)), PartPose.offset(14.9822F, -0.075F, 0.0F));

        PartDefinition hand = arm.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(0, 13).addBox(1.5F, -3.575F, -7.25F, 13.0F, 7.15F, 14.5F, new CubeDeformation(0.0F)), PartPose.offset(23.5F, -3.0F, 0.0F));

        PartDefinition thumb = hand.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(0, 10).addBox(-4.0821F, -27.2537F, -17.7892F, 4.45F, 4.75F, 10.25F, new CubeDeformation(0.0F)), PartPose.offset(5.523F, 23.5603F, 0.125F));

        PartDefinition fngers = hand.addOrReplaceChild("fngers", CubeListBuilder.create().texOffs(0, 7).addBox(0.0F, -1.575F, -1.75F, 7.0F, 3.15F, 3.5F, new CubeDeformation(0.0F))
        .texOffs(0, 6).addBox(7.0F, -1.575F, -1.75F, 6.0F, 3.15F, 3.5F, new CubeDeformation(0.0F)), PartPose.offset(14.5F, -2.0F, -5.5F));

        PartDefinition fngers2 = hand.addOrReplaceChild("fngers2", CubeListBuilder.create().texOffs(0, 7).addBox(0.0F, -1.575F, -1.75F, 7.0F, 3.15F, 3.5F, new CubeDeformation(0.0F))
        .texOffs(0, 6).addBox(7.0F, -1.575F, -1.75F, 6.0F, 3.15F, 3.5F, new CubeDeformation(0.0F)), PartPose.offset(14.5F, -2.0F, 0.0F));

        PartDefinition fngers3 = hand.addOrReplaceChild("fngers3", CubeListBuilder.create().texOffs(0, 7).addBox(0.0F, -1.575F, -1.75F, 7.0F, 3.15F, 3.5F, new CubeDeformation(0.0F))
        .texOffs(0, 6).addBox(7.0F, -1.575F, -1.75F, 6.0F, 3.15F, 3.5F, new CubeDeformation(0.0F)), PartPose.offset(14.5F, -2.0F, 5.5F));

        PartDefinition chest_bones_d = chest.addOrReplaceChild("chest_bones_d", CubeListBuilder.create(), PartPose.offset(31.0F, 5.0F, 4.4F));

        PartDefinition lower_bone_r1 = chest_bones_d.addOrReplaceChild("lower_bone_r1", CubeListBuilder.create().texOffs(0, 1).addBox(3.0F, -5.0F, -6.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(3.0F, -5.0F, -8.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(3.0F, -5.0F, -10.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -2.0F, 6.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition upper_bone_r1 = chest_bones_d.addOrReplaceChild("upper_bone_r1", CubeListBuilder.create().texOffs(0, 1).addBox(4.2993F, -7.4682F, -6.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(4.2993F, -7.4682F, -10.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(4.2993F, -7.4682F, -8.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 0.0F, 6.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition hip = chest.addOrReplaceChild("hip", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 33.5F));

        PartDefinition leg_ = hip.addOrReplaceChild("leg_", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -4.8505F, -7.2384F, 7.0F, 15.0F, 14.25F, new CubeDeformation(0.0F)), PartPose.offset(14.0858F, -2.3802F, -14.0234F));

        PartDefinition coxa = leg_.addOrReplaceChild("coxa", CubeListBuilder.create(), PartPose.offset(0.0F, 8.6495F, 0.2616F));

        PartDefinition coxa_r1 = coxa.addOrReplaceChild("coxa_r1", CubeListBuilder.create().texOffs(0, 9).addBox(1.5F, -4.5F, -5.0F, 5.0F, 15.0F, 9.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 1.5F, 1.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition joelho = coxa.addOrReplaceChild("joelho", CubeListBuilder.create().texOffs(0, 9).addBox(-3.5F, -4.5F, -5.0F, 7.0F, 9.0F, 9.25F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 11.0F, 5.0F));

        PartDefinition tornozelo = joelho.addOrReplaceChild("tornozelo", CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, -1.0F));

        PartDefinition tornozelo_r1 = tornozelo.addOrReplaceChild("tornozelo_r1", CubeListBuilder.create().texOffs(0, 7).addBox(1.5F, -1.5F, -4.25F, 5.0F, 14.0F, 7.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -1.5F, 2.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition pe = tornozelo.addOrReplaceChild("pe", CubeListBuilder.create().texOffs(0, 13).addBox(-4.75F, -2.125F, -9.0F, 9.5F, 6.25F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.604F, -5.9879F));

        // ---- lado esquerdo (o modelo original so tinha braco e perna do lado direito) ----
        PartDefinition armd_l = chest.addOrReplaceChild("armd_l", CubeListBuilder.create(), PartPose.offset(-23.8796F, -7.4671F, -29.9F));

        PartDefinition upperarm_l = armd_l.addOrReplaceChild("upperarm_l", CubeListBuilder.create().texOffs(0, 16).addBox(-22.5F, -6.0F, -6.0F, 23.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.2583F, -0.4826F, -0.1F));

        PartDefinition arm_l = upperarm_l.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(0, 16).addBox(-25.0F, -7.575F, -7.25F, 26.0F, 15.15F, 14.5F, new CubeDeformation(0.0F)), PartPose.offset(-14.9822F, -0.075F, 0.0F));

        PartDefinition hand_l = arm_l.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(0, 13).addBox(-14.5F, -3.575F, -7.25F, 13.0F, 7.15F, 14.5F, new CubeDeformation(0.0F)), PartPose.offset(-23.5F, -3.0F, 0.0F));

        PartDefinition thumb_l = hand_l.addOrReplaceChild("thumb_l", CubeListBuilder.create().texOffs(0, 10).addBox(-0.3679F, -27.2537F, -17.7892F, 4.45F, 4.75F, 10.25F, new CubeDeformation(0.0F)), PartPose.offset(-5.523F, 23.5603F, 0.125F));

        PartDefinition fngers_l = hand_l.addOrReplaceChild("fngers_l", CubeListBuilder.create().texOffs(0, 7).addBox(-7.0F, -1.575F, -1.75F, 7.0F, 3.15F, 3.5F, new CubeDeformation(0.0F))
        .texOffs(0, 6).addBox(-13.0F, -1.575F, -1.75F, 6.0F, 3.15F, 3.5F, new CubeDeformation(0.0F)), PartPose.offset(-14.5F, -2.0F, -5.5F));

        PartDefinition fngers2_l = hand_l.addOrReplaceChild("fngers2_l", CubeListBuilder.create().texOffs(0, 7).addBox(-7.0F, -1.575F, -1.75F, 7.0F, 3.15F, 3.5F, new CubeDeformation(0.0F))
        .texOffs(0, 6).addBox(-13.0F, -1.575F, -1.75F, 6.0F, 3.15F, 3.5F, new CubeDeformation(0.0F)), PartPose.offset(-14.5F, -2.0F, 0.0F));

        PartDefinition fngers3_l = hand_l.addOrReplaceChild("fngers3_l", CubeListBuilder.create().texOffs(0, 7).addBox(-7.0F, -1.575F, -1.75F, 7.0F, 3.15F, 3.5F, new CubeDeformation(0.0F))
        .texOffs(0, 6).addBox(-13.0F, -1.575F, -1.75F, 6.0F, 3.15F, 3.5F, new CubeDeformation(0.0F)), PartPose.offset(-14.5F, -2.0F, 5.5F));

        PartDefinition leg__l = hip.addOrReplaceChild("leg__l", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -4.8505F, -7.2384F, 7.0F, 15.0F, 14.25F, new CubeDeformation(0.0F)), PartPose.offset(-14.0858F, -2.3802F, -14.0234F));

        PartDefinition coxa_l = leg__l.addOrReplaceChild("coxa_l", CubeListBuilder.create(), PartPose.offset(0.0F, 8.6495F, 0.2616F));

        PartDefinition coxa_r1_l = coxa_l.addOrReplaceChild("coxa_r1_l", CubeListBuilder.create().texOffs(0, 9).addBox(-6.5F, -4.5F, -5.0F, 5.0F, 15.0F, 9.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 1.5F, 1.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition joelho_l = coxa_l.addOrReplaceChild("joelho_l", CubeListBuilder.create().texOffs(0, 9).addBox(-3.5F, -4.5F, -5.0F, 7.0F, 9.0F, 9.25F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 11.0F, 5.0F));

        PartDefinition tornozelo_l = joelho_l.addOrReplaceChild("tornozelo_l", CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, -1.0F));

        PartDefinition tornozelo_r1_l = tornozelo_l.addOrReplaceChild("tornozelo_r1_l", CubeListBuilder.create().texOffs(0, 7).addBox(-6.5F, -1.5F, -4.25F, 5.0F, 14.0F, 7.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -1.5F, 2.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition pe_l = tornozelo_l.addOrReplaceChild("pe_l", CubeListBuilder.create().texOffs(0, 13).addBox(-4.75F, -2.125F, -9.0F, 9.5F, 6.25F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.604F, -5.9879F));

        PartDefinition neck = monster.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -6.0F, -34.0F));

        PartDefinition neck_r1 = neck.addOrReplaceChild("neck_r1", CubeListBuilder.create().texOffs(0, 11).addBox(-7.0F, -11.0F, -2.0F, 18.0F, 18.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 1.0F, -3.0F, 0.0F, 0.0F, -0.7854F));

        PartDefinition jawneck_d_r1 = neck.addOrReplaceChild("jawneck_d_r1", CubeListBuilder.create().texOffs(0, 7).addBox(-2.75F, -4.5F, -2.5F, 4.75F, 9.0F, 7.0F, new CubeDeformation(0.0F))
        .texOffs(0, 7).addBox(17.25F, -4.5F, -2.5F, 4.75F, 9.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.0F, 1.5F, -3.5F, 0.3927F, 0.0F, 0.0F));

        PartDefinition lomb_r1 = neck.addOrReplaceChild("lomb_r1", CubeListBuilder.create().texOffs(0, 10).addBox(-3.0F, -1.5F, -3.5F, 6.0F, 6.0F, 10.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -14.5F, -5.25F, 0.3927F, 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(1.0F, -1.0F, -4.0F));

        PartDefinition tophead = head.addOrReplaceChild("tophead", CubeListBuilder.create().texOffs(0, 16).addBox(-13.0F, -19.0F, 0.0F, 26.0F, 7.75F, 16.25F, new CubeDeformation(-0.004F)), PartPose.offset(0.0F, 17.1F, -17.6F));

        PartDefinition crainum_r1 = tophead.addOrReplaceChild("crainum_r1", CubeListBuilder.create().texOffs(0, 16).addBox(-13.0F, -13.5F, -14.0F, 26.0F, 11.25F, 35.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.1F, 1.6F, 0.3927F, 0.0F, 0.0F));

        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 16).addBox(-13.75F, -11.5F, -13.5F, 27.5F, 7.25F, 29.75F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 17.1F, -17.6F));

        PartDefinition teeth = jaw.addOrReplaceChild("teeth", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -4.352F, -2.6216F, -0.0873F, 0.0F, 0.0F));

        PartDefinition jaw_teeth_d = teeth.addOrReplaceChild("jaw_teeth_d", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -12.0F));

        PartDefinition teeth1 = jaw_teeth_d.addOrReplaceChild("teeth1", CubeListBuilder.create(), PartPose.offset(0.0F, -1.3066F, 0.5412F));

        PartDefinition cube_r1 = teeth1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.7F, 2.0F, 0.0F, 0.45F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 2.0F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth2 = jaw_teeth_d.addOrReplaceChild("teeth2", CubeListBuilder.create(), PartPose.offset(0.0F, -1.3066F, 0.5412F));

        PartDefinition cube_r2 = teeth2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.8F, 3.0F, 0.0F, 0.7F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.55F, 3.25F, 0.0F, 0.45F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.1F, 2.75F, 0.0F, 0.35F, 0.75F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth3 = jaw_teeth_d.addOrReplaceChild("teeth3", CubeListBuilder.create(), PartPose.offset(0.0F, -1.3066F, 0.5412F));

        PartDefinition cube_r3 = teeth3.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.45F, 3.75F, 0.0F, 0.2F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 3.75F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth4 = jaw_teeth_d.addOrReplaceChild("teeth4", CubeListBuilder.create(), PartPose.offset(0.0F, -1.3066F, 0.5412F));

        PartDefinition cube_r4 = teeth4.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.45F, 3.75F, 0.0F, 0.2F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 3.75F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth5 = jaw_teeth_d.addOrReplaceChild("teeth5", CubeListBuilder.create(), PartPose.offset(0.0F, -1.3066F, 0.5412F));

        PartDefinition cube_r5 = teeth5.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.45F, 4.75F, 0.0F, 0.2F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 4.5F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth6 = jaw_teeth_d.addOrReplaceChild("teeth6", CubeListBuilder.create(), PartPose.offset(0.0F, -1.3066F, 0.5412F));

        PartDefinition cube_r6 = teeth6.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.45F, 5.4F, 0.0F, 0.2F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 5.25F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth7 = jaw_teeth_d.addOrReplaceChild("teeth7", CubeListBuilder.create(), PartPose.offset(0.0F, -1.3066F, 0.5412F));

        PartDefinition cube_r7 = teeth7.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.7F, 1.25F, 0.0F, 0.45F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.5F, 1.0F, 0.0F, 0.75F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 1.25F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition jaw_teeth_f = teeth.addOrReplaceChild("jaw_teeth_f", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.0784F, -2.594F, -17.0968F, 1.1781F, -1.1781F, -1.5708F));

        PartDefinition teeth16 = jaw_teeth_f.addOrReplaceChild("teeth16", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r8 = teeth16.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.6F, 5.0F, 0.0F, 0.5F, 0.15F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.8F, 5.5F, 0.0F, 0.7F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.55F, 5.15F, 0.0F, 0.45F, 0.35F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.2F, 4.95F, 0.0F, 0.45F, 0.95F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth20 = jaw_teeth_f.addOrReplaceChild("teeth20", CubeListBuilder.create(), PartPose.offset(3.75F, 1.9823F, 3.6782F));

        PartDefinition cube_r9 = teeth20.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.8F, 5.2F, 0.0F, 0.5F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.5F, 5.4F, 0.0F, 0.2F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.3F, 4.95F, 0.0F, 0.55F, 0.75F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth18 = jaw_teeth_f.addOrReplaceChild("teeth18", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r10 = teeth18.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.55F, 4.3F, 0.0F, 0.6F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.75F, 3.9F, 0.0F, 0.8F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.7F, 4.05F, 0.0F, 0.75F, 0.3F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.05F, 3.75F, 0.0F, 0.3F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth15 = jaw_teeth_f.addOrReplaceChild("teeth15", CubeListBuilder.create(), PartPose.offset(3.75F, 2.8624F, 1.5533F));

        PartDefinition cube_r11 = teeth15.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.85F, 4.3F, 0.0F, 0.6F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.95F, 3.9F, 0.0F, 0.7F, 0.3F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.9F, 4.15F, 0.0F, 0.65F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 3.75F, 0.0F, 0.5F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth17 = jaw_teeth_f.addOrReplaceChild("teeth17", CubeListBuilder.create(), PartPose.offset(3.75F, 3.2451F, 0.6294F));

        PartDefinition cube_r12 = teeth17.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.65F, 4.2F, 0.0F, 0.4F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.85F, 3.85F, 0.0F, 0.6F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.95F, 3.95F, 0.0F, 0.7F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.35F, 3.75F, 0.0F, 0.6F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth19 = jaw_teeth_f.addOrReplaceChild("teeth19", CubeListBuilder.create(), PartPose.offset(3.75F, 3.6278F, -0.2945F));

        PartDefinition cube_r13 = teeth19.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.85F, 4.3F, 0.0F, 0.9F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.65F, 3.85F, 0.0F, 0.7F, 0.35F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -1.05F, 4.15F, 0.0F, 1.1F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.05F, 3.75F, 0.0F, 0.3F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition jaw_teeth_e = teeth.addOrReplaceChild("jaw_teeth_e", CubeListBuilder.create(), PartPose.offsetAndRotation(3.65F, -3.5713F, -14.5587F, 2.3562F, 0.0F, 3.1416F));

        PartDefinition teeth8 = jaw_teeth_e.addOrReplaceChild("teeth8", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r14 = teeth8.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.5F, 2.0F, 0.0F, 0.25F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 2.0F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth9 = jaw_teeth_e.addOrReplaceChild("teeth9", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r15 = teeth9.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.8F, 5.4F, 0.0F, 0.7F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.55F, 5.15F, 0.0F, 0.45F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.1F, 5.15F, 0.0F, 0.35F, 0.75F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth10 = jaw_teeth_e.addOrReplaceChild("teeth10", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r16 = teeth10.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.95F, 3.95F, 0.0F, 0.7F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 3.75F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth11 = jaw_teeth_e.addOrReplaceChild("teeth11", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r17 = teeth11.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.45F, 3.75F, 0.0F, 0.2F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 3.75F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth12 = jaw_teeth_e.addOrReplaceChild("teeth12", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r18 = teeth12.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.7F, 4.5F, 0.0F, 0.45F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 4.5F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth13 = jaw_teeth_e.addOrReplaceChild("teeth13", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r19 = teeth13.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.7F, 3.15F, 0.0F, 0.45F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 3.0F, 0.0F, 0.5F, 0.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth14 = jaw_teeth_e.addOrReplaceChild("teeth14", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r20 = teeth14.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.25F, 1.0F, 0.0F, 0.5F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.35F, 1.25F, 0.0F, 0.6F, 0.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition head_teeh = head.addOrReplaceChild("head_teeh", CubeListBuilder.create(), PartPose.offset(0.0F, 15.0F, -17.25F));

        PartDefinition jaw_teeth_ft = head_teeh.addOrReplaceChild("jaw_teeth_ft", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.0784F, -7.096F, -20.4185F, 0.0F, -1.5708F, 2.7489F));

        PartDefinition teeth21 = jaw_teeth_ft.addOrReplaceChild("teeth21", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r21 = teeth21.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.6F, 5.0F, 0.0F, 0.5F, 0.15F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.8F, 5.5F, 0.0F, 0.7F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -1.05F, 5.15F, 0.0F, 0.95F, 0.35F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.2F, 4.95F, 0.0F, 0.45F, 0.95F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth22 = jaw_teeth_ft.addOrReplaceChild("teeth22", CubeListBuilder.create(), PartPose.offset(3.75F, 1.9823F, 3.6782F));

        PartDefinition cube_r22 = teeth22.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.8F, 5.2F, 0.0F, 0.5F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.5F, 5.4F, 0.0F, 0.2F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.3F, 4.95F, 0.0F, 0.55F, 0.75F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth23 = jaw_teeth_ft.addOrReplaceChild("teeth23", CubeListBuilder.create(), PartPose.offset(3.75F, 2.4415F, 2.5696F));

        PartDefinition cube_r23 = teeth23.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.55F, 4.3F, 0.0F, 0.6F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.75F, 3.9F, 0.0F, 0.8F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.95F, 4.05F, 0.0F, 1.0F, 0.3F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.05F, 3.75F, 0.0F, 0.3F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth24 = jaw_teeth_ft.addOrReplaceChild("teeth24", CubeListBuilder.create(), PartPose.offset(3.75F, 2.8624F, 1.5533F));

        PartDefinition cube_r24 = teeth24.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.85F, 4.3F, 0.0F, 0.6F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.95F, 3.9F, 0.0F, 0.7F, 0.3F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -1.15F, 4.15F, 0.0F, 0.9F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.25F, 3.75F, 0.0F, 0.5F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth25 = jaw_teeth_ft.addOrReplaceChild("teeth25", CubeListBuilder.create(), PartPose.offset(3.75F, 3.2451F, 0.6294F));

        PartDefinition cube_r25 = teeth25.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.65F, 4.3F, 0.0F, 0.4F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.55F, 3.85F, 0.0F, 0.3F, 0.25F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.95F, 4.05F, 0.0F, 0.7F, 0.3F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.35F, 3.75F, 0.0F, 0.6F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition teeth26 = jaw_teeth_ft.addOrReplaceChild("teeth26", CubeListBuilder.create(), PartPose.offset(3.75F, 3.6278F, -0.2945F));

        PartDefinition cube_r26 = teeth26.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75F, -0.85F, 4.3F, 0.0F, 0.9F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -0.65F, 3.85F, 0.0F, 0.7F, 0.35F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-0.75F, -1.05F, 4.15F, 0.0F, 1.1F, 0.2F, new CubeDeformation(0.0F))
        .texOffs(0, 1).addBox(-0.75F, -0.05F, 3.75F, 0.0F, 0.3F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -1.0F, -6.0F, 0.3927F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(MonstroCorrupto entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        boolean ataque = entity.isAggressive();

        // cabeca acompanha o alvo
        this.head.yRot = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
        this.head.xRot = Mth.clamp(headPitch, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        // mandibula: respira parado, abre ao atacar
        this.jaw.xRot = (ataque ? 0.38F : 0.06F) + Mth.sin(ageInTicks * 0.08F) * 0.03F;

        // pernas (a esquerda em oposicao)
        float passo = Mth.cos(limbSwing * 0.5F) * 0.9F * limbSwingAmount;
        this.leg.xRot = passo;
        this.legL.xRot = -passo;

        // bracos: o modelo original esta em "T"; aqui eles descem e se apoiam a frente.
        // zRot abaixa o braco; yRot o leva para frente/tras (antes de abaixar).
        float baixa = ataque ? 0.25F : 1.0F;
        float frente = ataque ? 0.55F : 1.1F;
        float balanco = passo * 0.6F + Mth.sin(ageInTicks * 0.07F) * 0.04F;
        float golpe = ataque ? 0.6F + Mth.sin(ageInTicks * 0.45F) * 0.25F : 0.0F;

        this.arm.zRot = baixa;
        this.arm.yRot = balanco + golpe;
        this.foreArm.yRot = frente;

        this.armL.zRot = -baixa;
        this.armL.yRot = balanco - golpe;
        this.foreArmL.yRot = -frente;
    }
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.monster.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}