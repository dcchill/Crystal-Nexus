package net.crystalnexus.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import net.crystalnexus.procedures.LUNADroneOnEntityTickUpdateProcedure;
import net.crystalnexus.entity.LUNADroneEntity;
import net.crystalnexus.client.model.animations.droneAnimation;
import net.crystalnexus.client.model.Modeldrone;

public class LUNADroneRenderer extends MobRenderer<LUNADroneEntity, Modeldrone<LUNADroneEntity>> {
	public LUNADroneRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(Modeldrone.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(LUNADroneEntity entity) {
		return ResourceLocation.parse("crystalnexus:textures/entities/drone_texture.png");
	}

	private static final class AnimatedModel extends Modeldrone<LUNADroneEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<LUNADroneEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(LUNADroneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, droneAnimation.idle, ageInTicks, 1f);
				this.animateWalk(droneAnimation.forward, limbSwing, limbSwingAmount, 1f, 1f);
				this.animate(entity.animationState2, droneAnimation.power_up, ageInTicks, 1f);
				if (LUNADroneOnEntityTickUpdateProcedure.execute(entity.level(), entity))
					this.animateWalk(droneAnimation.forward_w_box, limbSwing, limbSwingAmount, 1f, 1f);
				this.animate(entity.animationState4, droneAnimation.idle, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(LUNADroneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}