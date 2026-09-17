package net.crystalnexus.assembly;

import net.crystalnexus.block.entity.AssemblyLineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;
import net.neoforged.neoforge.fluids.FluidStack;

/** Bounded server-side execution facade for the persisted graph. */
public final class AssemblyGraphRuntime {
    private AssemblyGraphRuntime() {}

    public static void tick(AssemblyLineControllerBlockEntity controller) {
        if (!controller.graphEnabled()) return;
        for (AssemblyGraph.Node node : controller.graph().nodes) {
            if (node.machine == 0) continue;
            if (node.block.endsWith(":multiblock_item_input") || node.block.endsWith(":multiblock_item_output")) continue;
            BlockPos pos = BlockPos.of(node.machine);
            var worker = controller.machineAt(pos);
            if (worker == null) { node.status = "Machine missing"; continue; }
            routeInputs(controller, node, worker);
            routeFluidInputs(controller, node, worker);
            routeFluidOutputs(controller, node, worker);
            routeOutputs(controller, node, worker);
            node.status = "Running";
        }
        routeSinks(controller);
    }

    private static void routeSinks(AssemblyLineControllerBlockEntity controller) {
        for (AssemblyGraph.Node sink : controller.graph().nodes) {
            for (int socket = 0; socket < sink.inputSockets; socket++) {
                for (AssemblyGraph.Edge edge : controller.graph().edges) {
                    if (edge.to() != sink.id || edge.input() != socket) continue;
                    ItemStack offered = controller.takeGraphItem(edge.from(), edge.output(), 64);
                    if (offered.isEmpty()) continue;
                    if (!controller.insertGraphSink(sink.id, socket, offered)) controller.returnGraphItem(edge.from(), edge.output(), offered);
                }
            }
        }
    }

    private static void routeFluidInputs(AssemblyLineControllerBlockEntity controller, AssemblyGraph.Node node, AssemblyLineMachine worker) {
        var handler = worker.fluidHandler();
        if (handler == null) return;
        for (AssemblyGraph.Edge edge : controller.graph().edges) {
            if (edge.to() != node.id) continue;
            FluidStack offered = controller.takeGraphFluid(edge.from(), edge.output(), 1000);
            if (offered.isEmpty()) continue;
            int accepted = handler.fill(offered, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (accepted < offered.getAmount()) {
                offered.shrink(accepted);
                controller.returnGraphFluid(edge.from(), edge.output(), offered);
            }
        }
    }

    private static void routeFluidOutputs(AssemblyLineControllerBlockEntity controller, AssemblyGraph.Node node, AssemblyLineMachine worker) {
        var handler = worker.fluidHandler();
        if (handler == null) return;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (fluid.isEmpty()) continue;
                if (node.outputFluids == null || tank >= node.outputFluids.length || node.outputFluids[tank] == null || node.outputFluids[tank].isEmpty()
                    || !net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals(node.outputFluids[tank])) continue;
            final int outputSocket = tank;
            boolean connected = controller.graph().edges.stream().anyMatch(e -> e.from() == node.id && e.output() == outputSocket);
            if (!connected) {
                FluidStack moved = handler.drain(Math.min(1000, fluid.getAmount()), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                if (!moved.isEmpty() && controller.exportGraphFluid(moved)) handler.drain(moved.getAmount(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private static void routeInputs(AssemblyLineControllerBlockEntity controller, AssemblyGraph.Node node, AssemblyLineMachine worker) {
        IItemHandler target = worker.itemHandler();
        if (target == null) return;
        for (int socket = 0; socket < node.inputSockets; socket++) {
            int slot = node.inputSlots != null && socket < node.inputSlots.length ? node.inputSlots[socket] : socket;
            if (slot < 0 || slot >= target.getSlots()) continue;
            ItemStack current = target.getStackInSlot(slot);
            if (!current.isEmpty()) continue;
            for (int edgeIndex = 0; edgeIndex < controller.graph().edges.size(); edgeIndex++) {
                AssemblyGraph.Edge edge = controller.graph().edges.get(edgeIndex);
                if (edge.to() != node.id || edge.input() != socket) continue;
                ItemStack offered = controller.takeGraphItem(edge.from(), edge.output(), 1);
                if (offered.isEmpty()) continue;
                ItemStack leftover = target.insertItem(slot, offered, false);
                if (!leftover.isEmpty()) controller.returnGraphItem(edge.from(), edge.output(), leftover);
                if (leftover.getCount() < offered.getCount()) return;
            }
        }
    }

    private static void routeOutputs(AssemblyLineControllerBlockEntity controller, AssemblyGraph.Node node, AssemblyLineMachine worker) {
        if (node.outputSlots == null) return;
        for (int outputId = 0; outputId < node.outputSlots.length; outputId++) {
            int slot = node.outputSlots[outputId];
            ItemStack output = worker.inventory().getItem(slot);
            if (output.isEmpty()) continue;
            final int socket = outputId;
            boolean connected = controller.graph().edges.stream().anyMatch(e -> e.from() == node.id && e.output() == socket);
            boolean export = node.outputExport != null && outputId < node.outputExport.length && node.outputExport[outputId];
            if (!connected && export && controller.exportGraphItem(node.id, outputId, output.copy())) {
                worker.inventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }
}
