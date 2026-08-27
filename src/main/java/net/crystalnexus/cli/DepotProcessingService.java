package net.crystalnexus.cli;

import net.crystalnexus.block.DepotCableBlock;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DepotProcessingService {
    private static final Map<UUID, Map<ResourceLocation, Integer>> NEXT_MACHINE_INDEX = new ConcurrentHashMap<>();
    private DepotProcessingService() {
    }

    public static DepotSavedData.CraftingJob tick(ServerPlayer player, DepotSavedData depot) {
        DepotSavedData.CraftingJob job = depot.getCraftingJob();
        return job == null ? null : tick(player, depot, job.id());
    }

    public static DepotSavedData.CraftingJob tick(ServerPlayer player, DepotSavedData depot, int jobId) {
        DepotSavedData.CraftingJob job = depot.getCraftingJob(jobId);
        DepotSavedData.CraftingStep step = job == null ? null : job.currentStep();
        if (step == null || !step.processing()) return null;

        List<DepotNetwork.DepotMachineEndpoint> machines = new ArrayList<>(DepotNetwork.machineEndpoints(player));
        if (!step.machineTypes().isEmpty()) {
            machines.removeIf(endpoint -> !step.machineTypes().contains(BuiltInRegistries.BLOCK.getKey(
                    endpoint.level().getBlockState(endpoint.machinePos()).getBlock())));
        }
        ResourceLocation preferredMachine = depot.getPreferredMachine(step.outputId());
        if (preferredMachine != null) {
            machines.sort(java.util.Comparator.comparing(endpoint -> !preferredMachine.equals(BuiltInRegistries.BLOCK.getKey(
                    endpoint.level().getBlockState(endpoint.machinePos()).getBlock()))));
        }
        DepotSavedData.ProcessingTask task = depot.getProcessingTask(jobId);
        DepotNetwork.DepotMachineEndpoint machine;
        if (task == null) {
            machine = selectMachine(player, depot, machines, step, jobId);
            if (machine == null) return null;
            task = new DepotSavedData.ProcessingTask(machine.level().dimension().location(), machine.machinePos(),
                    step.inputs(), step.outputs());
        } else {
            DepotSavedData.ProcessingTask active = task;
            machine = machines.stream().filter(endpoint -> endpoint.machinePos().equals(active.machinePos())
                    && endpoint.level().dimension().location().equals(active.dimension())).findFirst().orElse(null);
            if (machine == null) return null;
        }

        List<DepotSavedData.SlotEntry> remainingInputs = new ArrayList<>(task.remainingInputs());
        Map<ResourceLocation, Long> inserted = new HashMap<>();
        // Insert each slot entry into a consecutive slot position so machines with
        // order-sensitive recipes (e.g. Matter Transmutation Table) receive items in
        // the correct positions: entry 0 -> slot 0, entry 1 -> slot 1, ...
        List<IItemHandler> handlers = handlers(machine.level(), machine.machinePos());
        int totalSlots = handlers.stream().mapToInt(IItemHandler::getSlots).sum();
        int slot = 0;
        for (int index = 0; index < task.remainingInputs().size(); index++) {
            DepotSavedData.SlotEntry entry = task.remainingInputs().get(index);
            if (entry.count() <= 0) continue;
            boolean fluid = DepotSavedData.isFluidKey(entry.itemId());
            long accepted = fluid ? insertFluid(machine, entry.itemId(), entry.count(), false)
                    : insertOrdered(handlers, slot, entry.itemId(), entry.count());
            if (accepted <= 0) continue;
            inserted.merge(entry.itemId(), accepted, DepotSavedData::saturatedAdd);
            remainingInputs.set(index, new DepotSavedData.SlotEntry(entry.itemId(),
                    Math.max(0, entry.count() - accepted)));
            if (!fluid) slot = Math.min(totalSlots - 1, slot + 1);
        }
        remainingInputs.removeIf(entry -> entry.count() <= 0);

        Map<ResourceLocation, Long> remainingOutputs = new HashMap<>(task.remainingOutputs());
        Map<ResourceLocation, Long> extracted = new HashMap<>();
        if (remainingInputs.isEmpty()) {
            for (Map.Entry<ResourceLocation, Long> entry : task.remainingOutputs().entrySet()) {
                long amount = extract(machine, entry.getKey(), entry.getValue());
                if (amount <= 0) continue;
                extracted.put(entry.getKey(), amount);
                long left = entry.getValue() - amount;
                if (left == 0) remainingOutputs.remove(entry.getKey());
                else remainingOutputs.put(entry.getKey(), left);
            }
        }
        return depot.updateProcessingTask(jobId, new DepotSavedData.ProcessingTask(task.dimension(), task.machinePos(),
                remainingInputs, remainingOutputs), inserted, extracted);
    }

    private static DepotNetwork.DepotMachineEndpoint selectMachine(ServerPlayer player, DepotSavedData depot,
            List<DepotNetwork.DepotMachineEndpoint> machines, DepotSavedData.CraftingStep step, int jobId) {
        List<DepotNetwork.DepotMachineEndpoint> eligible = machines.stream()
            .filter(endpoint -> DepotCableBlock.isDefaultMode(endpoint.level().getBlockState(endpoint.cablePos())))
            .filter(endpoint -> !depot.isProcessingMachineInUse(jobId, endpoint.level().dimension().location(), endpoint.machinePos()))
            .filter(endpoint -> canInsertAll(endpoint, step.inputs()))
            .filter(endpoint -> clearOldOutputs(endpoint, step.outputs(), depot)).toList();
        if (eligible.isEmpty()) return null;
        if (!depot.isMachineLoadBalancing()) return eligible.getFirst();

        ResourceLocation preferred = depot.getPreferredMachine(step.outputId());
        ResourceLocation type = preferred != null ? preferred : blockId(eligible.getFirst());
        List<DepotNetwork.DepotMachineEndpoint> matching = eligible.stream()
            .filter(endpoint -> type.equals(blockId(endpoint))).toList();
        if (matching.size() < 2) return matching.isEmpty() ? eligible.getFirst() : matching.getFirst();
        int next = NEXT_MACHINE_INDEX.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
            .merge(type, 1, (current, increment) -> current == Integer.MAX_VALUE ? 0 : current + 1);
        return matching.get(Math.floorMod(next - 1, matching.size()));
    }

    private static ResourceLocation blockId(DepotNetwork.DepotMachineEndpoint endpoint) {
        return BuiltInRegistries.BLOCK.getKey(endpoint.level().getBlockState(endpoint.machinePos()).getBlock());
    }

    private static boolean canInsertAll(DepotNetwork.DepotMachineEndpoint endpoint,
            List<DepotSavedData.SlotEntry> inputs) {
        List<IItemHandler> handlers = handlers(endpoint.level(), endpoint.machinePos());
        int totalSlots = handlers.stream().mapToInt(IItemHandler::getSlots).sum();
        int slot = 0;
        for (DepotSavedData.SlotEntry entry : inputs) {
            if (DepotSavedData.isFluidKey(entry.itemId())) {
                if (insertFluid(endpoint, entry.itemId(), entry.count(), true) < entry.count()) return false;
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(entry.itemId());
            if (item == null || !endpoint.config().accepts(new ItemStack(item))) return false;
            if (insertOrdered(handlers, slot, entry.itemId(), entry.count(), true) < entry.count()) return false;
            slot = Math.min(totalSlots - 1, slot + 1);
        }
        return true;
    }

    private static long insertFluid(DepotNetwork.DepotMachineEndpoint endpoint,
            ResourceLocation resourceKey, long amount, boolean simulate) {
        ResourceLocation fluidId = DepotSavedData.fluidId(resourceKey);
        var fluid = fluidId == null ? null : BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null || amount <= 0) return 0;
        int remaining = (int) Math.min(Integer.MAX_VALUE, amount);
        Set<IFluidHandler> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Direction side : Direction.values()) {
            IFluidHandler handler = endpoint.level().getCapability(Capabilities.FluidHandler.BLOCK,
                    endpoint.machinePos(), side);
            if (handler == null || !seen.add(handler)) continue;
            remaining -= handler.fill(new FluidStack(fluid, remaining), simulate
                    ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
            if (remaining <= 0) break;
        }
        if (remaining > 0) {
            IFluidHandler handler = endpoint.level().getCapability(Capabilities.FluidHandler.BLOCK,
                    endpoint.machinePos(), null);
            if (handler != null && seen.add(handler)) remaining -= handler.fill(new FluidStack(fluid, remaining),
                    simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }
        return amount - Math.max(0, remaining);
    }

    private static long insertOrdered(List<IItemHandler> handlers, int slotIndex,
            ResourceLocation id, long amount) {
        return insertOrdered(handlers, slotIndex, id, amount, false);
    }

    /**
     * Inserts {@code amount} of {@code id} starting at slot {@code slotIndex},
     * continuing into subsequent slots if the amount exceeds a slot's capacity.
     * This preserves recipe slot order: each call begins where the previous left off.
     */
    private static long insertOrdered(List<IItemHandler> handlers, int slotIndex,
            ResourceLocation id, long amount, boolean simulate) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || amount <= 0) return 0;
        long remaining = amount;
        int maxStack = item.getDefaultMaxStackSize();
        int cursor = 0;
        outer:
        for (IItemHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (cursor < slotIndex) {
                    cursor++;
                    continue;
                }
                ItemStack existing = handler.getStackInSlot(slot);
                if (!existing.isEmpty() && !BuiltInRegistries.ITEM.getKey(existing.getItem()).equals(id)) {
                    cursor++;
                    continue;
                }
                // Capability insertion can bypass menu-level mayPlace checks.
                // Respect the handler's slot validity first so output-only slots
                // (for example the Particle Accelerator output) never receive an
                // ingredient from the depot.
                if (!handler.isItemValid(slot, new ItemStack(item))) {
                    cursor++;
                    continue;
                }
                int space = maxStack - existing.getCount();
                if (space <= 0) {
                    cursor++;
                    continue;
                }
                int toInsert = (int) Math.min(remaining, space);
                ItemStack leftover = handler.insertItem(slot, new ItemStack(item, toInsert), simulate);
                remaining -= toInsert - leftover.getCount();
                cursor++;
                if (remaining == 0) break outer;
            }
        }
        return amount - remaining;
    }

    private static boolean clearOldOutputs(DepotNetwork.DepotMachineEndpoint endpoint,
            Map<ResourceLocation, Long> outputs, DepotSavedData depot) {
        for (ResourceLocation output : outputs.keySet()) {
            long removed = extract(endpoint, output, depot.getFree());
            if (removed > 0) depot.add(output, removed);
            if (extract(endpoint, output, 1, true) > 0) return false;
        }
        return true;
    }

    private static long extract(DepotNetwork.DepotMachineEndpoint endpoint, ResourceLocation id, long amount) {
        return extract(endpoint, id, amount, false);
    }

    private static long extract(DepotNetwork.DepotMachineEndpoint endpoint, ResourceLocation id, long amount,
            boolean simulate) {
        if (amount <= 0) return 0;
        long remaining = amount;
        for (IItemHandler handler : handlers(endpoint.level(), endpoint.machinePos())) {
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack stack = handler.getStackInSlot(slot).copy();
                if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) continue;
                ItemStack taken = handler.extractItem(slot, (int) Math.min(Integer.MAX_VALUE, remaining), simulate);
                if (!taken.isEmpty() && BuiltInRegistries.ITEM.getKey(taken.getItem()).equals(id)) {
                    remaining -= Math.min(remaining, taken.getCount());
                }
            }
            if (remaining == 0) break;
        }
        return amount - remaining;
    }

    private static List<IItemHandler> handlers(ServerLevel level, BlockPos pos) {
        List<IItemHandler> handlers = new ArrayList<>();
        Set<IItemHandler> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Direction side : Direction.values()) add(level, pos, side, handlers, seen);
        add(level, pos, null, handlers, seen);
        return handlers;
    }

    private static void add(ServerLevel level, BlockPos pos, Direction side,
            List<IItemHandler> handlers, Set<IItemHandler> seen) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (handler != null && seen.add(handler)) handlers.add(handler);
    }
}
