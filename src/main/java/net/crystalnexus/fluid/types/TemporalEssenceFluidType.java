package net.crystalnexus.fluid.types;

import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

import net.crystalnexus.init.CrystalnexusModFluidTypes;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class TemporalEssenceFluidType extends FluidType {
	public TemporalEssenceFluidType() {
		super(FluidType.Properties.create().density(-1000).fallDistanceModifier(0F).canExtinguish(true).supportsBoating(true).canHydrate(true).motionScale(0.007D)
				.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
	}

	@SubscribeEvent
	public static void registerFluidTypeExtensions(RegisterClientExtensionsEvent event) {
		event.registerFluidType(new IClientFluidTypeExtensions() {
			private static final ResourceLocation STILL_TEXTURE = ResourceLocation.parse("crystalnexus:block/temporal_essence");
			private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.parse("crystalnexus:block/temporal_essence_flowing");

			@Override
			public ResourceLocation getStillTexture() {
				return STILL_TEXTURE;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return FLOWING_TEXTURE;
			}

			@Override
			public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
				return new Vector3f(0x54 / 255f, 0x23 / 255f, 0x3E / 255f);
			}
		}, CrystalnexusModFluidTypes.TEMPORAL_ESSENCE_TYPE.get());
	}
}
