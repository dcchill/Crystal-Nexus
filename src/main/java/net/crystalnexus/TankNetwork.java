package net.crystalnexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.*;

public final class TankNetwork {
    private TankNetwork() {}

    public static void rebuildAround(Level level, BlockPos changedPos) {
        if (!(level instanceof ServerLevel)) return;

        Set<BlockPos> seeds = new HashSet<>();
        seeds.add(changedPos);
        for (Direction d : Direction.values()) seeds.add(changedPos.relative(d));

        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos seed : seeds) {
            if (visited.contains(seed)) continue;
            if (!(level.getBlockEntity(seed) instanceof TankBlockEntity)) continue;

            Set<BlockPos> comp = collectComponent(level, seed);
            visited.addAll(comp);
            rebuildComponent(level, comp);
        }
    }

    private static Set<BlockPos> collectComponent(Level level, BlockPos start) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> out = new HashSet<>();
        q.add(start);
        out.add(start);

        while (!q.isEmpty()) {
            BlockPos p = q.removeFirst();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (out.contains(n)) continue;
                if (level.getBlockEntity(n) instanceof TankBlockEntity) {
                    out.add(n);
                    q.add(n);
                }
            }
        }
        return out;
    }

    private static void rebuildComponent(Level level, Set<BlockPos> members) {
        if (members.isEmpty()) return;

        BlockPos controllerPos = members.stream()
                .min(Comparator.comparingLong(BlockPos::asLong))
                .orElseThrow();

        BlockEntity cbe = level.getBlockEntity(controllerPos);
        if (!(cbe instanceof TankBlockEntity newController)) return;

        // Snapshot unique old controllers
        Map<BlockPos, TankBlockEntity> oldControllers = new HashMap<>();
        for (BlockPos p : members) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof TankBlockEntity t)) continue;
            TankBlockEntity c = t.getController();
            if (c != null) oldControllers.put(c.getBlockPos(), c);
        }

        FluidStack proto = FluidStack.EMPTY;
        int total = 0;

        for (TankBlockEntity oc : oldControllers.values()) {
            int amt = oc.getTank().getFluidAmount();
            if (amt <= 0) continue;

            FluidStack fs = oc.getTank().getFluid().copy();
            if (fs.isEmpty()) continue;

            if (proto.isEmpty()) {
                proto = fs.copy();
                proto.setAmount(0);
            } else if (!FluidStack.isSameFluidSameComponents(proto, fs)) {
                continue; // don’t mix
            }

            total += amt;
        }

        // Rewire
        for (BlockPos p : members) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof TankBlockEntity t)) continue;
            t.setControllerPos(controllerPos);
            if (!p.equals(controllerPos)) t.setMemberCount(1);
        }

        newController.setControllerPos(controllerPos);
        newController.setMemberCount(members.size());

        // Restore
        if (!proto.isEmpty()) {
            int cap = newController.getTank().getCapacity();
            int clamped = Math.min(total, cap);
            FluidStack finalStack = proto.copy();
            finalStack.setAmount(clamped);
            newController.getTank().setFluid(finalStack);
        }

        // Clear old controllers except the new controller
        for (Map.Entry<BlockPos, TankBlockEntity> e : oldControllers.entrySet()) {
            if (e.getKey().equals(controllerPos)) continue;
            e.getValue().getTank().setFluid(FluidStack.EMPTY);
        }

        for (BlockPos p : members) {
            level.sendBlockUpdated(p, level.getBlockState(p), level.getBlockState(p), 3);
        }
    }
}
