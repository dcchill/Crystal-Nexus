package net.crystalnexus.client;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.crystalnexus.client.gui.AssemblyLineScreen;
import net.crystalnexus.network.AssemblyLineSlotsSet;
import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import net.neoforged.neoforge.fluids.FluidStack;

/** Copies the currently displayed JEI recipe into the selected graph node. */
public final class AssemblyLineJeiTransferHandler implements IUniversalRecipeTransferHandler<AssemblyLineMenu> {
    @Override public Class<? extends AssemblyLineMenu> getContainerClass() { return AssemblyLineMenu.class; }
    @Override public Optional<MenuType<AssemblyLineMenu>> getMenuType() {
        return Optional.of(CrystalnexusModMenus.ASSEMBLY_LINE.get());
    }

    @Override public IRecipeTransferError transferRecipe(AssemblyLineMenu menu, Object recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            List<String> inputs = items(slots, RecipeIngredientRole.INPUT);
            List<String> outputs = items(slots, RecipeIngredientRole.OUTPUT);
            List<String> inputFluids = fluids(slots, RecipeIngredientRole.INPUT);
            List<String> outputFluids = fluids(slots, RecipeIngredientRole.OUTPUT);
            int node = net.minecraft.client.Minecraft.getInstance().screen instanceof AssemblyLineScreen screen
                ? screen.transferTargetNode()
                : AssemblyLineScreen.rememberedSelection(menu);
            if (node < 0) {
                node = menu.controller.graph().nodes.stream().filter(candidate -> candidate.machine != 0)
                    .map(candidate -> candidate.id).findFirst().orElse(-1);
            }
            if ((!inputs.isEmpty() || !outputs.isEmpty() || !inputFluids.isEmpty() || !outputFluids.isEmpty())
                && node >= 0) {
                PacketDistributor.sendToServer(new AssemblyLineSlotsSet(menu.containerId,
                        node, inputs, outputs, inputFluids, outputFluids));
            }

        }
        return null;
    }

    private static List<String> fluids(IRecipeSlotsView slots, RecipeIngredientRole role) {
        return slots.getSlotViews(role).stream()
            .map(slot -> slot.getDisplayedIngredient()
                .map(ITypedIngredient::getIngredient).orElse(null))
            .filter(FluidStack.class::isInstance)
            .map(FluidStack.class::cast)
            .filter(fluid -> !fluid.isEmpty())
            .map(fluid -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString())
            .distinct()
            .toList();
    }

    private static List<String> items(IRecipeSlotsView slots, RecipeIngredientRole role) {
        return slots.getSlotViews(role).stream()
                .map(slot -> slot.getDisplayedItemStack().orElseGet(() ->
                    slot.getItemStacks().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY)))
                .filter(stack -> !stack.isEmpty())
                .map(stack -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .toList();
    }
}
