package net.crystalnexus.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.crystalnexus.CrystalnexusMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.crystalnexus.client.render.ParticleAcceleratorControllerRenderer;
import net.crystalnexus.client.render.QuarryBlockEntityRenderer;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.block.entity.TankBlockEntity;
import net.crystalnexus.block.entity.PipeStraightBlockEntity;
import net.crystalnexus.block.entity.ParticleAcceleratorControllerBlockEntity;
import net.crystalnexus.block.entity.QuarryBlockEntity;
import net.crystalnexus.block.entity.GravitationalArrayControllerBlockEntity;
import net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity;
import net.crystalnexus.block.entity.SolarEngineControllerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.QuadTransformers;
import org.jetbrains.annotations.Nullable;

import net.crystalnexus.client.render.ConveyerBeltBER;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.item.LaserSaberItem;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {
	private static final ModelResourceLocation LASER_SABER = ModelResourceLocation.inventory(
			ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "laser_saber"));
	private static final ModelResourceLocation LASER_SABER_GLOW = ModelResourceLocation.standalone(
			ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "item/laser_saber_glow"));
	private static final List<String> ROTATING_MODELS = List.of(
			"yellow_dwarf_star", "orange_star", "blue_star", "pink_star", "dead_star", "terra", "caelus", "boreas", "meteor");

	@SubscribeEvent
	public static void animateModels(ModelEvent.ModifyBakingResult event) {
		for (String name : ROTATING_MODELS) {
			ModelResourceLocation location = new ModelResourceLocation(
					ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, name), "inventory");
			event.getModels().computeIfPresent(location, (ignored, model) -> new RotatingItemModel(model));
		}
		BakedModel glow = event.getModels().get(LASER_SABER_GLOW);
		if (glow != null) {
			event.getModels().computeIfPresent(LASER_SABER,
					(ignored, model) -> new LaserSaberModel(model, new FullbrightModel(glow)));
		}
	}

	@SubscribeEvent
	public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
		event.register(LASER_SABER_GLOW);
	}

	private static final class LaserSaberModel extends BakedModelWrapper<BakedModel> {
		private final BakedModel glow;

		private LaserSaberModel(BakedModel originalModel, BakedModel glow) {
			super(originalModel);
			this.glow = glow;
		}

		@Override
		public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
			return LaserSaberItem.isPowered(stack) ? List.of(this, glow) : List.of(this);
		}

		@Override
		public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
			originalModel.applyTransform(context, poseStack, leftHand);
			return this;
		}
	}

	private static final class FullbrightModel extends BakedModelWrapper<BakedModel> {
		private FullbrightModel(BakedModel originalModel) {
			super(originalModel);
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
			return originalModel.getQuads(state, side, random).stream().map(quad -> {
				BakedQuad copy = new BakedQuad(quad.getVertices().clone(), quad.getTintIndex(),
						quad.getDirection(), quad.getSprite(), false, false);
				QuadTransformers.settingMaxEmissivity().processInPlace(copy);
				return copy;
			}).toList();
		}

		@Override
		public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
			return List.of(Sheets.translucentItemSheet());
		}
	}

	private static final class RotatingItemModel extends BakedModelWrapper<BakedModel> {
		private RotatingItemModel(BakedModel originalModel) {
			super(originalModel);
		}

		@Override
		public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
			return List.of(this);
		}

		@Override
		public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
			originalModel.applyTransform(context, poseStack, leftHand);
			if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
					|| context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
					|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
					|| context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
				poseStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % 6000L) * 0.06F));
			}
			return this;
		}
	}

@SubscribeEvent
public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {

    event.registerBlockEntityRenderer(
        (BlockEntityType<ConveyerBeltBaseBlockEntity>)
            CrystalnexusModBlockEntities.CONVEYER_BELT.get(),
        ConveyerBeltBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<ConveyerBeltBaseBlockEntity>)
            CrystalnexusModBlockEntities.CONVEYER_BELT_INPUT.get(),
        ConveyerBeltBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<ConveyerBeltBaseBlockEntity>)
            CrystalnexusModBlockEntities.CONVEYER_BELT_OUTPUT.get(),
        ConveyerBeltBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<TankBlockEntity>)
            CrystalnexusModBlockEntities.TANK.get(),
        net.crystalnexus.client.renderer.TankBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<PipeStraightBlockEntity>)
            CrystalnexusModBlockEntities.PIPE_STRAIGHT.get(),
        net.crystalnexus.client.renderer.PipeStraightBER::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<ParticleAcceleratorControllerBlockEntity>)
            CrystalnexusModBlockEntities.PARTICLE_ACCELERATOR_CONTROLLER.get(),
        ParticleAcceleratorControllerRenderer::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<QuarryBlockEntity>)
            CrystalnexusModBlockEntities.QUARRY.get(),
        QuarryBlockEntityRenderer::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<GravitationalArrayControllerBlockEntity>)
            CrystalnexusModBlockEntities.GRAVITATIONAL_ARRAY_CONTROLLER.get(),
        net.crystalnexus.client.renderer.GravitationalArrayRenderer::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<SolarSimulatorControllerBlockEntity>)
            CrystalnexusModBlockEntities.SOLAR_SIMULATOR_CONTROLLER.get(),
        net.crystalnexus.client.renderer.SolarSimulatorRenderer::new
    );

    event.registerBlockEntityRenderer(
        (BlockEntityType<SolarEngineControllerBlockEntity>)
            CrystalnexusModBlockEntities.SOLAR_ENGINE_CONTROLLER.get(),
        net.crystalnexus.client.renderer.SolarEngineRenderer::new
    );

}
}
