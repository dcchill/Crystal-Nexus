// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modeljet_pack<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "jet_pack"), "main");
	private final ModelPart bone;
	private final ModelPart intake;
	private final ModelPart intake2;
	private final ModelPart body2;
	private final ModelPart wing;
	private final ModelPart wing2;
	private final ModelPart bone2;
	private final ModelPart bone3;

	public Modeljet_pack(ModelPart root) {
		this.bone = root.getChild("bone");
		this.intake = this.bone.getChild("intake");
		this.intake2 = this.bone.getChild("intake2");
		this.body2 = this.bone.getChild("body2");
		this.wing = this.bone.getChild("wing");
		this.wing2 = this.bone.getChild("wing2");
		this.bone2 = root.getChild("bone2");
		this.bone3 = root.getChild("bone3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(),
				PartPose.offset(0.0F, -1.0F, 0.0F));

		PartDefinition intake = bone.addOrReplaceChild("intake",
				CubeListBuilder.create().texOffs(22, 0).addBox(-2.0F, -3.808F, -2.1276F, 4.0F, 7.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(5.0F, 4.808F, 5.1276F, 0.0F, 0.7854F, 0.0F));

		PartDefinition cube_r1 = intake.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(24, 22).addBox(-2.0F, -3.5F, -2.0F, 4.0F, 5.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 5.6603F, 1.0596F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r2 = intake.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(20, 34).addBox(1.0F, -3.0F, -1.0F, 4.0F, 2.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.0F, -1.736F, -0.5165F, 0.3927F, 0.0F, 0.0F));

		PartDefinition intake2 = bone.addOrReplaceChild("intake2",
				CubeListBuilder.create().texOffs(22, 0).addBox(-2.0F, -3.808F, -2.1276F, 4.0F, 7.0F, 4.0F,
						new CubeDeformation(0.001F)),
				PartPose.offsetAndRotation(-5.0F, 4.808F, 5.1276F, 0.0F, -0.7854F, 0.0F));

		PartDefinition cube_r3 = intake2.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(24, 22).addBox(-2.0F, -3.5F, -2.0F, 4.0F, 5.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 5.6603F, 1.0596F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r4 = intake2.addOrReplaceChild("cube_r4",
				CubeListBuilder.create().texOffs(20, 34).addBox(1.0F, -3.0F, -1.0F, 4.0F, 2.0F, 4.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.0F, -1.736F, -0.5165F, 0.3927F, 0.0F, 0.0F));

		PartDefinition body2 = bone.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F,
				-23.0F, 2.0F, 8.0F, 8.0F, 3.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition wing = bone
				.addOrReplaceChild("wing",
						CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, -3.0F, -1.0F, 11.0F, 6.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(2.0F, 4.0F, 4.0F, 0.0F, -0.2182F, 0.0F));

		PartDefinition cube_r5 = wing.addOrReplaceChild("cube_r5",
				CubeListBuilder.create().texOffs(16, 31).addBox(-1.5F, 0.0F, -0.5F, 9.0F, 2.0F, 1.0F,
						new CubeDeformation(-0.001F)),
				PartPose.offsetAndRotation(0.5F, 2.0F, -0.5F, 0.0F, 0.0F, 0.2618F));

		PartDefinition wing2 = bone
				.addOrReplaceChild("wing2",
						CubeListBuilder.create().texOffs(0, 18).addBox(-10.0F, -3.0F, -1.0F, 11.0F, 6.0F, 1.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(-2.0F, 4.0F, 4.0F, 0.0F, 0.2182F, 0.0F));

		PartDefinition cube_r6 = wing2.addOrReplaceChild("cube_r6",
				CubeListBuilder.create().texOffs(16, 31).addBox(-7.5F, 1.0F, -0.5F, 9.0F, 2.0F, 1.0F,
						new CubeDeformation(-0.001F)),
				PartPose.offsetAndRotation(-0.5F, 1.0F, -0.5F, 0.0F, 0.0F, -0.2618F));

		PartDefinition bone2 = partdefinition.addOrReplaceChild("bone2", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bone2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bone3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}