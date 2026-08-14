package net.crystalnexus.fluid.types;

import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class MineralSlurryFluidType extends FluidType {
    public MineralSlurryFluidType() {
        super(Properties.create().density(1800).viscosity(1800)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
    }

    @Override public Component getDescription(FluidStack stack) {
        return net.crystalnexus.processing.MaterialProcessingCatalog.slurryMaterial(stack)
            .map(material -> Component.literal(displayName(material) + " Mineral Slurry"))
            .orElseGet(() -> Component.literal("Mineral Slurry"));
    }

    private static String displayName(ResourceLocation material) {
        String[] words = material.getPath().replace('/', '_').split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) if (!word.isEmpty()) {
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    @SubscribeEvent
    public static void registerClientExtension(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.parse("crystalnexus:block/acidic_slurry_still");
            private static final ResourceLocation FLOWING = ResourceLocation.parse("crystalnexus:block/acidic_slurry_flowing");
            @Override public ResourceLocation getStillTexture() { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOWING; }
            @Override public int getTintColor() { return 0xff7f95a3; }
            @Override public int getTintColor(FluidStack stack) {
                return net.crystalnexus.client.MineralSlurryColors.tint(stack);
            }
        }, CrystalnexusModFluidTypes.MINERAL_SLURRY_TYPE.get());
    }
}
