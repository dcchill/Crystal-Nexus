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
public class Modelhover_pack<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("crystalnexus", "modelhover_pack"), "main");
	public final ModelPart hoverpack;
	public final ModelPart bone;
	public final ModelPart right;
	public final ModelPart bone3;
	public final ModelPart bone2;
	public final ModelPart left;
	public final ModelPart bone4;
	public final ModelPart bone6;
	public final ModelPart bone7;

	public Modelhover_pack(ModelPart root) {
		this.hoverpack = root.getChild("hoverpack");
		this.bone = this.hoverpack.getChild("bone");
		this.right = this.bone.getChild("right");
		this.bone3 = this.right.getChild("bone3");
		this.bone2 = this.hoverpack.getChild("bone2");
		this.left = this.bone2.getChild("left");
		this.bone4 = this.left.getChild("bone4");
		this.bone6 = root.getChild("bone6");
		this.bone7 = root.getChild("bone7");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition hoverpack = partdefinition.addOrReplaceChild("hoverpack", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition bone = hoverpack.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(28, 28).addBox(-11.001F, 0.999F, 1.999F, 8.002F, 8.002F, 3.002F, new CubeDeformation(0.0F)), PartPose.offset(7.0F, 0.0F, 0.0F));
		PartDefinition cube_r1 = bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(28, 14).addBox(-0.5F, 0.5F, 0.5F, 1.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, 3.5F, 4.5F, -0.7854F, 0.0F, 0.0F));
		PartDefinition right = bone.addOrReplaceChild("right", CubeListBuilder.create(), PartPose.offset(-7.0F, 9.0F, 3.0F));
		PartDefinition cube_r2 = right.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(16, 30).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.0F, 2.0F, 2.0F, 0.0F, -0.7854F, 0.0F));
		PartDefinition bone3 = right.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -6.1947F, -5.5258F, 7.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.5F, -3.8053F, 7.5258F, 0.3927F, 0.0F, 0.0F));
		PartDefinition cube_r3 = bone3.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 30).addBox(-3.5F, 0.5F, 0.5F, 7.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 1.3053F, -0.0258F, 0.6981F, 0.0F, 0.0F));
		PartDefinition bone2 = hoverpack.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(-7.0F, 0.0F, 0.0F));
		PartDefinition cube_r4 = bone2.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(28, 14).addBox(-0.5F, 0.5F, 0.5F, 1.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.5F, 3.5F, 4.5F, -0.7854F, 0.0F, 0.0F));
		PartDefinition left = bone2.addOrReplaceChild("left", CubeListBuilder.create(), PartPose.offset(7.0F, 9.0F, 3.0F));
		PartDefinition cube_r5 = left.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(16, 30).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 2.0F, 2.0F, 0.0F, 0.7854F, 0.0F));
		PartDefinition bone4 = left.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(0, 15).addBox(-3.5F, -6.1947F, -5.5258F, 7.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(7.5F, -3.8053F, 7.5258F, 0.3927F, 0.0F, 0.0F));
		PartDefinition cube_r6 = bone4.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 30).addBox(-3.5F, 0.5F, 0.5F, 7.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 1.3053F, -0.0258F, 0.6981F, 0.0F, 0.0F));
		PartDefinition bone6 = partdefinition.addOrReplaceChild("bone6", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition bone7 = partdefinition.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		hoverpack.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		bone6.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		bone7.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}