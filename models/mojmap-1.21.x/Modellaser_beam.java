// Made with Blockbench 5.0.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modellaser_beam<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "laser_beam"), "main");
	private final ModelPart group;

	public Modellaser_beam(ModelPart root) {
		this.group = root.getChild("group");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition group = partdefinition.addOrReplaceChild("group",
				CubeListBuilder.create().texOffs(-8, 0).addBox(-2.0F, 8.0F, 0.0F, 2.0F, 0.0F, 8.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-8.0F, 24.0F, -1.0F, 0.0F, 1.5708F, -1.5708F));

		PartDefinition cube_r1 = group
				.addOrReplaceChild("cube_r1",
						CubeListBuilder.create().texOffs(-8, 0).addBox(-1.0F, 0.0F, -4.0F, 2.0F, 0.0F, 8.0F,
								new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(-1.0F, 8.0F, 4.0F, 0.0F, 0.0F, 1.5708F));

		return LayerDefinition.create(meshdefinition, 16, 16);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		group.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}