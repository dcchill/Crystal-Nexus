package net.crystalnexus.fluid.types;
import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class AtmosphereFluidType extends FluidType {
    public AtmosphereFluidType() { super(Properties.create().density(-1).viscosity(100)
        .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
        .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY)); }
    @SubscribeEvent public static void registerClientExtensions(RegisterClientExtensionsEvent event) { event.registerFluidType(new IClientFluidTypeExtensions() {
        private static final ResourceLocation STILL = ResourceLocation.parse("crystalnexus:block/atmosphere");
        private static final ResourceLocation FLOWING = ResourceLocation.parse("crystalnexus:block/atmosphere_flowing");
        public ResourceLocation getStillTexture() { return STILL; } public ResourceLocation getFlowingTexture() { return FLOWING; }
    }, CrystalnexusModFluidTypes.ATMOSPHERE_TYPE.get()); }
}
