package net.crystalnexus.mixin;

import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.core.Holder;

import net.crystalnexus.init.CrystalnexusModBiomes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;

@Mixin(NoiseGeneratorSettings.class)
public class NoiseGeneratorSettingsMixin implements CrystalnexusModBiomes.CrystalnexusModNoiseGeneratorSettings {
	@Unique
	private Holder<DimensionType> crystalnexus_dimensionTypeReference;

	@WrapMethod(method = "surfaceRule")
	public SurfaceRules.RuleSource surfaceRule(Operation<SurfaceRules.RuleSource> original) {
		SurfaceRules.RuleSource retval = original.call();
		if (this.crystalnexus_dimensionTypeReference != null) {
			retval = CrystalnexusModBiomes.adaptSurfaceRule(retval, this.crystalnexus_dimensionTypeReference);
		}
		return retval;
	}

	@Override
	public void setcrystalnexusDimensionTypeReference(Holder<DimensionType> dimensionType) {
		this.crystalnexus_dimensionTypeReference = dimensionType;
	}
}