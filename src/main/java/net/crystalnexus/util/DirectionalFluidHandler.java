package net.crystalnexus.util;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Automation view of a tank; machine processing retains direct access to its storage. */
public final class DirectionalFluidHandler implements IFluidHandler {
    private final IFluidHandler storage;
    private final boolean input;

    private DirectionalFluidHandler(IFluidHandler storage, boolean input) {
        this.storage = storage;
        this.input = input;
    }

    public static IFluidHandler input(IFluidHandler storage) { return new DirectionalFluidHandler(storage, true); }
    public static IFluidHandler output(IFluidHandler storage) { return new DirectionalFluidHandler(storage, false); }

    @Override public int getTanks() { return storage.getTanks(); }
    @Override public FluidStack getFluidInTank(int tank) { return storage.getFluidInTank(tank).copy(); }
    @Override public int getTankCapacity(int tank) { return storage.getTankCapacity(tank); }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return input && storage.isFluidValid(tank, stack); }
    @Override public int fill(FluidStack stack, FluidAction action) { return input ? storage.fill(stack, action) : 0; }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) { return input ? FluidStack.EMPTY : storage.drain(stack, action); }
    @Override public FluidStack drain(int amount, FluidAction action) { return input ? FluidStack.EMPTY : storage.drain(amount, action); }
}
