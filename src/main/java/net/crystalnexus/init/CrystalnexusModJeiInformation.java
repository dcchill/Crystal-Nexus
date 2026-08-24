package net.crystalnexus.init;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;

import java.util.List;

@JeiPlugin
public class CrystalnexusModJeiInformation implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.parse("crystalnexus:information");
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addIngredientInfo(List.of(new ItemStack(CrystalnexusModBlocks.CRYSTAL_CRUSHER.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.crystalnexus.cystal_crusher"));
		registration.addIngredientInfo(List.of(new ItemStack(CrystalnexusModBlocks.DUST_SEPARATOR.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.crystalnexus.dust_separator_jei"));
		registration.addIngredientInfo(List.of(new ItemStack(CrystalnexusModItems.EE_MATTER.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.crystalnexus.ee_matter_fe"));
		registration.addIngredientInfo(List.of(new ItemStack(CrystalnexusModItems.ENERGY_SINGULARITY.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.crystalnexus.energy_singularity_fe"));
		registration.addIngredientInfo(List.of(new ItemStack(CrystalnexusModItems.EMPTY_FUEL_CELL.get()), new ItemStack(CrystalnexusModItems.OIL_FUEL_CELL.get()), new ItemStack(CrystalnexusModItems.GAS_FUEL_CELL.get()),
				new ItemStack(CrystalnexusModBlocks.FLUID_PACKAGER.get())), VanillaTypes.ITEM_STACK, Component.translatable("jei.crystalnexus.fluid_packager_jei_info"));
		registration.addIngredientInfo(List.of(new ItemStack(CrystalnexusModBlocks.BLU_TNT.get()), new ItemStack(CrystalnexusModBlocks.INVERTIUM_CRYSTAL_BLOCK.get())), VanillaTypes.ITEM_STACK,
				Component.translatable("jei.crystalnexus.crystal_formation"));
	}
}
