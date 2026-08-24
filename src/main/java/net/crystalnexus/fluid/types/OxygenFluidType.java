package net.crystalnexus.fluid.types;

import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class OxygenFluidType extends FluidType {
	public OxygenFluidType() {
		super(Properties.create().density(-1).viscosity(100)
				.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
				.sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
	}

	@SubscribeEvent
	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerFluidType(new IClientFluidTypeExtensions() {
			private static final ResourceLocation STILL = ResourceLocation.parse("crystalnexus:block/compressed_oxygenpng");
			private static final ResourceLocation FLOWING = ResourceLocation.parse("crystalnexus:block/compressed_oxygen_flowing");

			@Override
			public ResourceLocation getStillTexture() {
				return STILL;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return FLOWING;
			}
		}, CrystalnexusModFluidTypes.OXYGEN_TYPE.get());
	}
}
