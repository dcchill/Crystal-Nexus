package net.crystalnexus.fluid.types;

import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class BloodFluidType extends FluidType {
    public BloodFluidType() { super(Properties.create().density(1060).viscosity(1200)); }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.parse("crystalnexus:block/blood_still");
            private static final ResourceLocation FLOWING = ResourceLocation.parse("crystalnexus:block/blood_flowing");
            @Override public ResourceLocation getStillTexture() { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOWING; }
            @Override public int getTintColor() { return 0xff9f1825; }
        }, CrystalnexusModFluidTypes.BLOOD_TYPE.get());
    }
}
