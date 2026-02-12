package net.crystalnexus.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modeldrone<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crystalnexus", "modeldrone"), "main");
	public final ModelPart bone;
	public final ModelPart arm_l;
	public final ModelPart arm_l_part;
	public final ModelPart arm_r;
	public final ModelPart arm_r_part;
	public final ModelPart prop_r;
	public final ModelPart bone3;
	public final ModelPart prop_l;
	public final ModelPart bone2;
	public final ModelPart box;

	public Modeldrone(ModelPart root) {
		this.bone = root.getChild("bone");
		this.arm_l = this.bone.getChild("arm_l");
		this.arm_l_part = this.arm_l.getChild("arm_l_part");
		this.arm_r = this.bone.getChild("arm_r");
		this.arm_r_part = this.arm_r.getChild("arm_r_part");
		this.prop_r = this.bone.getChild("prop_r");
		this.bone3 = this.prop_r.getChild("bone3");
		this.prop_l = this.bone.getChild("prop_l");
		this.bone2 = this.prop_l.getChild("bone2");
		this.box = this.bone.getChild("box");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -17.0F, -6.0F, 12.0F, 9.0F, 17.0F, new CubeDeformation(0.0F)).texOffs(58, 0)
				.addBox(6.0F, -16.0F, -4.0F, 5.0F, 7.0F, 13.0F, new CubeDeformation(0.0F)).texOffs(58, 0).addBox(-11.0F, -16.0F, -4.0F, 5.0F, 7.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition cube_r1 = bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(88, 29).addBox(-1.0F, -3.0F, 0.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.0F, -17.0F, 0.0F, 0.1572F, -0.3614F, -0.4215F));
		PartDefinition cube_r2 = bone.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(88, 29).addBox(-2.0F, -3.0F, 0.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.0F, -17.0F, 0.0F, 0.1572F, 0.3614F, 0.4215F));
		PartDefinition arm_l = bone.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(44, 84).addBox(9.0F, -9.0F, 1.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 0.0F, -2.0F));
		PartDefinition arm_l_part = arm_l.addOrReplaceChild("arm_l_part", CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, -1.0F, -21.5F, 3.0F, 2.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(11.5F, -7.0F, 3.5F));
		PartDefinition arm_r = bone.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(44, 84).addBox(-14.0F, -9.0F, 1.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, -2.0F));
		PartDefinition arm_r_part = arm_r.addOrReplaceChild("arm_r_part", CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, -1.0F, -21.5F, 3.0F, 2.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.5F, -7.0F, 3.5F));
		PartDefinition prop_r = bone.addOrReplaceChild("prop_r", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition cube_r3 = prop_r.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 67).addBox(-1.0F, -2.0F, -1.0F, 4.0F, 2.0F, 15.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-10.0F, -12.0F, 1.0F, 0.0F, -1.5708F, 0.0F));
		PartDefinition bone3 = prop_r.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(84, 85).addBox(-1.0F, -1.3F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-22.0F, -14.7F, 2.0F));
		PartDefinition cube_r4 = bone3.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(76, 67).addBox(-1.0F, 0.0F, -4.5F, 2.0F, 0.0F, 9.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -0.3F, -5.5F, 0.0F, 0.0F, -0.3927F));
		PartDefinition cube_r5 = bone3.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(76, 76).addBox(-1.0F, 0.0F, -4.5F, 2.0F, 0.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.3F, 5.5F, 0.0F, 0.0F, 0.3927F));
		PartDefinition cube_r6 = bone3.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(58, 20).addBox(-4.5F, 0.0F, -1.0F, 9.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.5F, -0.3F, 0.0F, -0.3927F, 0.0F, 0.0F));
		PartDefinition cube_r7 = bone3.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(58, 22).addBox(-4.5F, 0.0F, -1.0F, 9.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, -0.3F, 0.0F, 0.3927F, 0.0F, 0.0F));
		PartDefinition prop_l = bone.addOrReplaceChild("prop_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 3.1416F, 0.0F));
		PartDefinition cube_r8 = prop_l.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 67).addBox(-1.0F, -2.0F, -1.0F, 4.0F, 2.0F, 15.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-10.0F, -12.0F, 1.0F, 0.0F, -1.5708F, 0.0F));
		PartDefinition bone2 = prop_l.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(84, 85).addBox(-1.0F, -1.3F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-22.0F, -14.7F, 2.0F));
		PartDefinition cube_r9 = bone2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(76, 67).addBox(-1.0F, 0.0F, -4.5F, 2.0F, 0.0F, 9.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -0.3F, -5.5F, 0.0F, 0.0F, 0.3927F));
		PartDefinition cube_r10 = bone2.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(76, 76).addBox(-1.0F, 0.0F, -4.5F, 2.0F, 0.0F, 9.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -0.3F, 5.5F, 0.0F, 0.0F, -0.3927F));
		PartDefinition cube_r11 = bone2.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(58, 20).addBox(-4.5F, 0.0F, -1.0F, 9.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.5F, -0.3F, 0.0F, 0.3927F, 0.0F, 0.0F));
		PartDefinition cube_r12 = bone2.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(58, 22).addBox(-4.5F, 0.0F, -1.0F, 9.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.5F, -0.3F, 0.0F, -0.3927F, 0.0F, 0.0F));
		PartDefinition box = bone.addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 26).addBox(-8.0F, -5.0F, -5.0F, 16.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -13.0F, -15.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}