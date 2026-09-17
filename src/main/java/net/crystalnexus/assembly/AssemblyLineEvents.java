package net.crystalnexus.assembly;

import net.crystalnexus.block.entity.AssemblyLineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.*;

@EventBusSubscriber(modid = "crystalnexus")
public final class AssemblyLineEvents {
    private static final Map<Level, Set<BlockPos>> CONTROLLERS = new WeakHashMap<>();
    public static void register(AssemblyLineControllerBlockEntity be) { CONTROLLERS.computeIfAbsent(be.getLevel(), l -> new HashSet<>()).add(be.getBlockPos()); }
    public static void unregister(AssemblyLineControllerBlockEntity be) { var set = CONTROLLERS.get(be.getLevel()); if (set != null) set.remove(be.getBlockPos()); }
    public static void changed(Level level, BlockPos pos, boolean force) {
        if (level.isClientSide) return;
        for (BlockPos controller : List.copyOf(CONTROLLERS.getOrDefault(level, Set.of())))
            if (level.hasChunkAt(controller) && level.getBlockEntity(controller) instanceof AssemblyLineControllerBlockEntity be) be.changedAt(pos, force);
    }
    @SubscribeEvent public static void neighbors(BlockEvent.NeighborNotifyEvent e) { if (e.getLevel() instanceof Level l) changed(l, e.getPos(), false); }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e) { if (!e.isCanceled() && e.getLevel() instanceof Level l) changed(l, e.getPos(), true); }
    @SubscribeEvent public static void placing(BlockEvent.EntityPlaceEvent e) { if (!e.isCanceled() && e.getLevel() instanceof Level l) changed(l, e.getPos(), true); }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e) { for (BlockPos p : e.getAffectedBlocks()) changed(e.getLevel(), p, true); }
    @SubscribeEvent public static void load(ChunkEvent.Load e) { chunks(e); }
    @SubscribeEvent public static void unload(ChunkEvent.Unload e) { chunks(e); }
    private static void chunks(ChunkEvent e) {
        if (!(e.getLevel() instanceof Level l) || l.isClientSide) return;
        for (BlockPos p : List.copyOf(CONTROLLERS.getOrDefault(l, Set.of()))) {
            if (Math.abs((p.getX() >> 4)-e.getChunk().getPos().x) <= 2 && Math.abs((p.getZ() >> 4)-e.getChunk().getPos().z) <= 2
                && l.hasChunkAt(p) && l.getBlockEntity(p) instanceof AssemblyLineControllerBlockEntity be) be.invalidate();
        }
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock e) {
        var be = e.getLevel().getBlockEntity(e.getPos());
        if (be != null && be.getPersistentData().contains(AssemblyLineMachine.OWNER)) e.setCanceled(true);
    }
    @SubscribeEvent public static void recipesReloaded(net.neoforged.neoforge.event.OnDatapackSyncEvent e) {
        if (e.getPlayer() != null) return;
        for (var entry : CONTROLLERS.entrySet()) for (BlockPos pos : List.copyOf(entry.getValue()))
            if (entry.getKey().hasChunkAt(pos) && entry.getKey().getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity be) be.invalidate();
    }
}
