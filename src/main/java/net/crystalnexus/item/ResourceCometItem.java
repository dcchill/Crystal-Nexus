package net.crystalnexus.item;

import net.crystalnexus.init.CrystalnexusModDataComponents;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class ResourceCometItem extends Item {
    public static final TagKey<Item> COMET_MATERIAL = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("crystalnexus", "comet_material"));

    public ResourceCometItem() { super(new Item.Properties()); }

    public static boolean isMaterial(ItemStack stack) {
        return isBaseMaterial(stack) && stack.is(COMET_MATERIAL);
    }

    private static boolean isBaseMaterial(ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof ResourceCometItem)
            && stack.getMaxStackSize() > 1
            && ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance());
    }

    public static List<ItemStack> materials() {
        return BuiltInRegistries.ITEM.stream().map(Item::getDefaultInstance)
            .filter(ResourceCometItem::isMaterial).toList();
    }

    public static ItemStack create(ItemStack material) {
        if (!isMaterial(material)) return ItemStack.EMPTY;
        ItemStack comet = new ItemStack(CrystalnexusModItems.RESOURCE_COMET.get());
        comet.set(CrystalnexusModDataComponents.MATERIAL.get(), BuiltInRegistries.ITEM.getKey(material.getItem()));
        return comet;
    }

    public static ItemStack material(ItemStack comet) {
        if (!(comet.getItem() instanceof ResourceCometItem)) return ItemStack.EMPTY;
        var id = comet.get(CrystalnexusModDataComponents.MATERIAL.get());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        ItemStack result = BuiltInRegistries.ITEM.get(id).getDefaultInstance();
        // The tag gates new crafting; already-forged comets retain their target.
        return isBaseMaterial(result) ? result : ItemStack.EMPTY;
    }

    public static boolean isSingularity(ItemStack stack) {
        return stack.is(CrystalnexusModItems.DIAMOND_SINGULARITY.get())
            || stack.is(CrystalnexusModItems.ENERGY_SINGULARITY.get())
            || stack.is(CrystalnexusModItems.EMERALD_SINGULARITY.get());
    }

    @Override public Component getName(ItemStack stack) {
        ItemStack material = material(stack);
        return material.isEmpty() ? super.getName(stack)
            : Component.translatable("item.crystalnexus.resource_comet.named", material.getHoverName());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack material = material(stack);
        tooltip.add(material.isEmpty() ? Component.translatable("tooltip.crystalnexus.resource_comet.invalid")
            : Component.translatable("tooltip.crystalnexus.resource_comet", material.getHoverName()));
    }
}
