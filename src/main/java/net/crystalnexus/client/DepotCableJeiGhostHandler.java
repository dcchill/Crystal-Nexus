package net.crystalnexus.client;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.crystalnexus.client.gui.DepotCableConnectionScreen;
import net.crystalnexus.network.payload.C2S_DepotCableFilter;
import net.crystalnexus.world.inventory.DepotCableConnectionMenu;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class DepotCableJeiGhostHandler implements IGhostIngredientHandler<DepotCableConnectionScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(DepotCableConnectionScreen screen, ITypedIngredient<I> ingredient,
            boolean doStart) {
        ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return List.of();
        List<Target<I>> targets = new ArrayList<>(DepotCableConnectionMenu.FILTER_SLOTS);
        for (int slot = 0; slot < DepotCableConnectionMenu.FILTER_SLOTS; slot++) {
            int targetSlot = slot;
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(screen.getGuiLeft() + DepotCableConnectionMenu.SLOT_X + targetSlot * 18,
                            screen.getGuiTop() + DepotCableConnectionMenu.FILTER_Y, 16, 16);
                }

                @Override
                public void accept(I ingredient) {
                    screen.getMenu().setFilter(targetSlot, stack);
                    PacketDistributor.sendToServer(new C2S_DepotCableFilter(screen.getMenu().containerId,
                            targetSlot, BuiltInRegistries.ITEM.getKey(stack.getItem())));
                }
            });
        }
        return List.copyOf(targets);
    }

    @Override
    public void onComplete() {
    }
}
