package net.crystalnexus.init;

import net.crystalnexus.CrystalnexusMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrystalnexusModDataComponents {
    public static final DeferredRegister.DataComponents REGISTRY =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CrystalnexusMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> MATERIAL =
        REGISTRY.registerComponentType("material", builder -> builder
            .persistent(ResourceLocation.CODEC)
            .networkSynchronized(ResourceLocation.STREAM_CODEC));

    private CrystalnexusModDataComponents() {}
}
