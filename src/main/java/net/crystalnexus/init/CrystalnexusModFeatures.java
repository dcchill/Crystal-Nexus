package net.crystalnexus.init;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.world.feature.ResourceMeteorFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrystalnexusModFeatures {
    public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(Registries.FEATURE, CrystalnexusMod.MODID);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> RESOURCE_METEOR = REGISTRY.register("resource_meteor",
            () -> new ResourceMeteorFeature(NoneFeatureConfiguration.CODEC));

    private CrystalnexusModFeatures() {
    }
}
