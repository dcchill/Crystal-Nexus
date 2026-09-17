package net.crystalnexus.client;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.stack.EmiStack;
import net.crystalnexus.client.gui.AssemblyLineScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native EMI support; JEMI does not bridge JEI ghost ingredient handlers. */
@EmiEntrypoint
public final class AssemblyLineEmiPlugin implements EmiPlugin {
    @Override public void register(EmiRegistry registry) {
        registry.addDragDropHandler(AssemblyLineScreen.class, new EmiDragDropHandler.BoundsBased<>((screen, target) -> {
            for (AssemblyLineScreen.GhostTarget ghost : screen.ghostTargets(ItemStack.EMPTY)) {
                target.accept(new Bounds(ghost.area().getX(), ghost.area().getY(), ghost.area().getWidth(), ghost.area().getHeight()), ingredient -> {
                    // Check if it's a fluid by looking for non-empty EMI stacks
                    for (EmiStack emiStack : ingredient.getEmiStacks()) {
                        if (emiStack.isEmpty()) continue;
                        // Try to get item stack from EMI stack
                        ItemStack itemStack = emiStack.getItemStack();
                        if (!itemStack.isEmpty()) {
                            screen.acceptGhostItem(ghost.nodeId(), ghost.socket(), itemStack);
                            return;
                        }
                        // If no item, it might be a fluid - try to extract fluid
                        FluidStack fluid = extractFluid(emiStack);
                        if (fluid != null && !fluid.isEmpty()) {
                            screen.acceptGhostFluid(ghost.nodeId(), ghost.socket(), fluid);
                            return;
                        }
                    }
                });
            }
        }));
    }
    
    /** Attempts to extract a FluidStack from an EmiStack using reflection. */
    private static FluidStack extractFluid(EmiStack stack) {
        try {
            // First try to get fluid directly from EMI's API
            // Try to call getFluid() method via reflection
            java.lang.reflect.Method getFluid = stack.getClass().getMethod("getFluid");
            Object result = getFluid.invoke(stack);
            if (result instanceof net.neoforged.neoforge.fluids.FluidType ft) {
                // Get fluid from FluidType via builtInRegistryHolder().value()
                try {
                    java.lang.reflect.Method getHolder = ft.getClass().getMethod("builtInRegistryHolder");
                    Object holder = getHolder.invoke(ft);
                    java.lang.reflect.Method getValue = holder.getClass().getMethod("value");
                    Object fluidObj = getValue.invoke(holder);
                    if (fluidObj instanceof net.minecraft.world.level.material.Fluid fluid) {
                        long amount = stack.getAmount();
                        return new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE));
                    }
                } catch (Exception e2) {
                    // Fallback: try FluidType directly
                    try {
                        net.minecraft.world.level.material.Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                            net.minecraft.resources.ResourceLocation.parse(ft.toString()));
                        if (fluid != null) {
                            long amount = stack.getAmount();
                            return new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE));
                        }
                    } catch (Exception e3) {
                        // Give up
                    }
                }
            } else if (result instanceof net.minecraft.world.level.material.Fluid fluid) {
                long amount = stack.getAmount();
                return new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE));
            }
        } catch (Exception e) {
            // Reflection failed
        }
        // Last resort: try parsing toString() for fluid ID
        return parseFluidFromString(stack.toString());
    }
    
    /** Try to parse a fluid from EMI stack's string representation. */
    private static FluidStack parseFluidFromString(String str) {
        try {
            // EMI stacks often look like "fluid_namespace:fluid_id(amount)"
            String parsed = str.replaceAll("[()0-9]", "").trim();
            if (parsed.contains(":") && !parsed.startsWith("item@")) {
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(parsed);
                net.minecraft.world.level.material.Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
                if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    return new FluidStack(fluid, 1000);
                }
            }
        } catch (Exception e) {
            // Failed to parse
        }
        return null;
    }
}