package net.crystalnexus.item;

import net.crystalnexus.init.CrystalnexusModDataComponents;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class GeneratedSingularityItem extends Item {
    public static final int COST = 10_240;

    public GeneratedSingularityItem() {
        super(new Item.Properties().durability(COST).rarity(Rarity.RARE));
    }

    public static ItemStack create(ItemStack material) {
        if (!ResourceCometItem.isMaterial(material)) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(CrystalnexusModItems.GENERATED_SINGULARITY.get());
        result.set(CrystalnexusModDataComponents.MATERIAL.get(), BuiltInRegistries.ITEM.getKey(material.getItem()));
        return result;
    }

    public static ItemStack material(ItemStack singularity) {
        if (!(singularity.getItem() instanceof GeneratedSingularityItem)) return ItemStack.EMPTY;
        ResourceLocation id = singularity.get(CrystalnexusModDataComponents.MATERIAL.get());
        return id != null && BuiltInRegistries.ITEM.containsKey(id)
            ? BuiltInRegistries.ITEM.get(id).getDefaultInstance() : ItemStack.EMPTY;
    }

    @Override public Component getName(ItemStack stack) {
        ItemStack material = material(stack);
        return material.isEmpty() ? super.getName(stack)
            : Component.translatable("item.crystalnexus.generated_singularity.named", material.getHoverName());
    }
}
