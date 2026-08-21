package net.crystalnexus.jei_recipes;

import net.crystalnexus.client.gui.MultiblockStructurePreview;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record MultiblockStructureRecipe(
        ResourceLocation id,
        Component title,
        List<ItemStack> ingredients,
        MultiblockStructurePreview preview
) {
}
