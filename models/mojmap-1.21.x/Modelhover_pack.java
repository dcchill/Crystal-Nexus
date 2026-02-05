// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelhover_pack<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "hover_pack"), "main");
	private final ModelPart hoverpack;
	private final ModelPart bone;
	private final ModelPart right;
	private final ModelPart bone3;
	private final ModelPart bone2;
	private final ModelPart left;
	private final ModelPart bone4;
	private final ModelPart bone6;
	private final ModelPart bone7;

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

		PartDefinition hoverpack = partdefinition.addOrReplaceChild("hoverpack", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition bone = hoverpack.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(28, 28)
				.addBox(-11.001F, 0.999F, 1.999F, 8.002F, 8.002F, 3.002F, new CubeDeformation(0.0F)),
				PartPose.offset(7.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = bone.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(28, 14).addBox(-0.5F, 0.5F, 0.5F, 1.0F, 5.0F, 9.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.5F, 3.5F, 4.5F, -0.7854F, 0.0F, 0.0F));

		PartDefinition right = bone.addOrReplaceChild("right", CubeListBuilder.create(),
				PartPose.offset(-7.0F, 9.0F, 3.0F));

		PartDefinition cube_r2 = right.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(16, 30).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 6.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.0F, 2.0F, 2.0F, 0.0F, -0.7854F, 0.0F));

		PartDefinition bone3 = right.addOrReplaceChild("bone3",
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -6.1947F, -5.5258F, 7.0F, 8.0F, 7.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.5F, -3.8053F, 7.5258F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r3 = bone3.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(0, 30).addBox(-3.5F, 0.5F, 0.5F, 7.0F, 3.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 1.3053F, -0.0258F, 0.6981F, 0.0F, 0.0F));

		PartDefinition bone2 = hoverpack.addOrReplaceChild("bone2", CubeListBuilder.create(),
				PartPose.offset(-7.0F, 0.0F, 0.0F));

		PartDefinition cube_r4 = bone2
				.addOrReplaceChild("cube_r4",
						CubeListBuilder.create().texOffs(28, 14).addBox(-0.5F, 0.5F, 0.5F, 1.0F, 5.0F, 9.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(4.5F, 3.5F, 4.5F, -0.7854F, 0.0F, 0.0F));

		PartDefinition left = bone2.addOrReplaceChild("left", CubeListBuilder.create(),
				PartPose.offset(7.0F, 9.0F, 3.0F));

		PartDefinition cube_r5 = left
				.addOrReplaceChild("cube_r5",
						CubeListBuilder.create().texOffs(16, 30).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 6.0F, 2.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(4.0F, 2.0F, 2.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition bone4 = left.addOrReplaceChild("bone4",
				CubeListBuilder.create().texOffs(0, 15).addBox(-3.5F, -6.1947F, -5.5258F, 7.0F, 8.0F, 7.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(7.5F, -3.8053F, 7.5258F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r6 = bone4.addOrReplaceChild("cube_r6",
				CubeListBuilder.create().texOffs(0, 30).addBox(-3.5F, 0.5F, 0.5F, 7.0F, 3.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 1.3053F, -0.0258F, 0.6981F, 0.0F, 0.0F));

		PartDefinition bone6 = partdefinition.addOrReplaceChild("bone6", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone7 = partdefinition.addOrReplaceChild("bone7", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		hoverpack.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bone6.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bone7.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}