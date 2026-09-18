package net.crystalnexus.assembly;

import net.crystalnexus.block.entity.*;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.jei_recipes.*;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.*;
import net.crystalnexus.block.entity.IronSmelterBlockEntity;
import net.crystalnexus.block.entity.CrystalSmelterBlockEntity;
import net.crystalnexus.block.entity.ChlorophyteSmelterBlockEntity;
import net.crystalnexus.block.entity.InvertiumSmelterBlockEntity;
import net.crystalnexus.block.entity.UltimaSmelterBlockEntity;

/** Small adapters preserve each worker's normal processing, sounds and energy costs. */
public record AssemblyLineMachine(BlockEntity entity, Container inventory, Kind kind, IEnergyStorage energy, SideProfile profile) {
    public enum Kind { CRUSHER, CIRCUIT_PRESS, PARTS_ASSEMBLER, SMELTER, GENERIC }
    public static final String OWNER = "assembly_owner", TASK = "assembly_task", RECIPE = "assembly_recipe";
    public static AssemblyLineMachine at(Level level, BlockPos pos) {
        return at(level, pos, null);
    }
    public static AssemblyLineMachine at(Level level, BlockPos pos, SideProfile profile) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CrystalCrusherBlockEntity c) return new AssemblyLineMachine(c, c, Kind.CRUSHER, c.getEnergyStorage(), profile);
        if (be instanceof CircuitPressBlockEntity c)
            return new AssemblyLineMachine(c, c, Kind.CIRCUIT_PRESS, c.getEnergyStorage(), profile);
        if (be instanceof PartsAssemblerBlockEntity c) return new AssemblyLineMachine(c, c, Kind.PARTS_ASSEMBLER, c.getEnergyStorage(), profile);
        // Detect Crystal Nexus smelter blocks
        if (be instanceof IronSmelterBlockEntity c || be instanceof CrystalSmelterBlockEntity c2
            || be instanceof ChlorophyteSmelterBlockEntity c3 || be instanceof InvertiumSmelterBlockEntity c4
            || be instanceof UltimaSmelterBlockEntity c5) {
            IEnergyStorage energyStorage = getEnergyStorage(level, pos, profile);
            return new AssemblyLineMachine(be, (Container) be, Kind.SMELTER, energyStorage, profile);
        }
        // Accept any block with an inventory and/or energy capability as a generic worker.
        if (be instanceof Container c) {
            IEnergyStorage energyStorage = getEnergyStorage(level, pos, profile);
            return new AssemblyLineMachine(be, c, Kind.GENERIC, energyStorage, profile);
        }
        // External machines may expose only item or fluid capabilities rather than
        // implementing Container or exposing energy directly.
        IEnergyStorage energyStorage = getEnergyStorage(level, pos, profile);
        if (energyStorage != null || hasItemHandler(level, pos) || hasFluidHandler(level, pos)) {
            Container minimalInventory = createMinimalContainer(be);
            return new AssemblyLineMachine(be, minimalInventory, Kind.GENERIC, energyStorage, profile);
        }
        return null;
    }

    /** Attempts to get energy storage via NeoForge capability system. */
    private static IEnergyStorage getEnergyStorage(Level level, BlockPos pos, SideProfile profile) {
        if (level == null) return null;
        if (profile != null) {
            for (Direction side : profile.energyOrder()) {
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
                if (storage != null && (storage.canReceive() || storage.canExtract())) return storage;
            }
        }
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (storage != null) return storage;
        for (Direction side : profile == null ? Direction.values() : profile.energyOrder()) {
            storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            if (storage != null) return storage;
        }
        return null;
    }

    private static boolean hasItemHandler(Level level, BlockPos pos) {
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) return true;
        for (Direction side : Direction.values())
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null) return true;
        return false;
    }

    private static boolean hasFluidHandler(Level level, BlockPos pos) {
        if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null) return true;
        for (Direction side : Direction.values())
            if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null) return true;
        return false;
    }

    /** Creates a minimal non-functional container for blocks that have energy but no inventory. */
    private static Container createMinimalContainer(BlockEntity be) {
        return new Container() {
            @Override public int getContainerSize() { return 0; }
            @Override public boolean isEmpty() { return true; }
            @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
            @Override public ItemStack removeItem(int slot, int count) { return ItemStack.EMPTY; }
            @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
            @Override public void setItem(int slot, ItemStack stack) {}
            @Override public void setChanged() { be.setChanged(); }
            @Override public boolean stillValid(Player player) { return true; }
            @Override public void clearContent() {}
        };
    }
    public static Kind kind(RecipeHolder<?> recipe) {
        if (recipe.value() instanceof OreCrushingJeiRecipe) return Kind.CRUSHER;
        if (recipe.value() instanceof CircuitPressingRecipe) return Kind.CIRCUIT_PRESS;
        if (recipe.value() instanceof PartsAssemblingRecipe) return Kind.PARTS_ASSEMBLER;
        return null;
    }
    public int[] inputs() { return kind == Kind.CIRCUIT_PRESS ? new int[]{0, 2} : new int[]{0}; }
    public boolean supports(RecipeHolder<?> recipe) {
        return kind != Kind.GENERIC && kind(recipe) == kind && (!(recipe.value() instanceof OreCrushingJeiRecipe r)
            || MachineTier.from(entity.getBlockState()).supports(r.minimumMachineTier()));
    }
    public boolean empty() {
        if (kind == Kind.GENERIC) return false;
        // Do not claim an inventory while a player is editing it through an already-open menu.
        if (entity.getLevel().players().stream().anyMatch(p ->
            p.containerMenu instanceof net.crystalnexus.world.inventory.CrusherGuiMenu m && m.x == entity.getBlockPos().getX() && m.y == entity.getBlockPos().getY() && m.z == entity.getBlockPos().getZ()
            || p.containerMenu instanceof net.crystalnexus.world.inventory.CircuitPressGUIMenu m2 && m2.x == entity.getBlockPos().getX() && m2.y == entity.getBlockPos().getY() && m2.z == entity.getBlockPos().getZ()
            || p.containerMenu.slots.stream().anyMatch(s -> s.container == inventory))) return false;
        for (int i : inputs()) if (!inventory.getItem(i).isEmpty()) return false;
        return inventory.getItem(1).isEmpty() && progress() == 0;
    }
    public int progress() {
        return entity instanceof PartsAssemblerBlockEntity p ? p.getData().get(0) : (int) entity.getPersistentData().getDouble("progress");
    }
    public int startingEnergy(RecipeHolder<?> recipe) {
        ItemStack upgrade = inventory.getItem(kind == Kind.CIRCUIT_PRESS ? 3 : 2);
        if (recipe.value() instanceof PartsAssemblingRecipe p) return MachineUpgradeHelper.energyCost(upgrade, p.energyPerTick());
        return MachineUpgradeHelper.energyCost(entity.getBlockState(), upgrade, kind == Kind.CRUSHER ? 4096 : 2048);
    }
    public IItemHandler itemHandler() {
        if (entity.getLevel() == null) return null;
        if (kind == Kind.GENERIC && !(entity instanceof Container)) {
            IItemHandler capability = entity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), null);
            if (capability != null) return capability;
            for (Direction side : itemOrder(profile)) {
                capability = entity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), side);
                if (capability != null) return capability;
            }
        }
        if (entity instanceof Container c) return new InvWrapper(c);
        IItemHandler handler = entity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), null);
        if (handler != null) return handler;
        for (Direction side : itemOrder(profile)) {
            handler = entity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, entity.getBlockPos(), side);
            if (handler != null) return handler;
        }
        return null;
    }
    public IFluidHandler fluidHandler() {
        if (entity.getLevel() == null) return null;
        IFluidHandler handler = entity.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, entity.getBlockPos(), null);
        if (handler != null) return handler;
        for (Direction side : fluidOrder(profile)) {
            handler = entity.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, entity.getBlockPos(), side);
            if (handler != null) return handler;
        }
        return null;
    }
    private static Direction[] ordered(SideProfile profile) {
        return profile == null ? Direction.values() : profile.itemOrder();
    }
    private static Direction[] itemOrder(SideProfile profile) { return profile == null ? Direction.values() : profile.itemOrder(); }
    private static Direction[] fluidOrder(SideProfile profile) { return profile == null ? Direction.values() : profile.fluidOrder(); }
    public static final class SideProfile {
        private final int insertMask, extractMask, fillMask, drainMask, receiveMask, extractEnergyMask;
        private final Direction[] itemOrder, fluidOrder, energyOrder;
        public SideProfile(int insertMask, int extractMask, int fillMask, int drainMask, int receiveMask, int extractEnergyMask) {
            this.insertMask = insertMask; this.extractMask = extractMask; this.fillMask = fillMask; this.drainMask = drainMask;
            this.receiveMask = receiveMask; this.extractEnergyMask = extractEnergyMask;
            itemOrder = order(insertMask, extractMask); fluidOrder = order(fillMask, drainMask); energyOrder = order(receiveMask, extractEnergyMask);
        }
        private static Direction[] order(int preferred, int secondary) {
            List<Direction> result = new ArrayList<>();
            for (Direction d : Direction.values()) if ((preferred & 1 << d.ordinal()) != 0) result.add(d);
            for (Direction d : Direction.values()) if ((secondary & 1 << d.ordinal()) != 0 && !result.contains(d)) result.add(d);
            for (Direction d : Direction.values()) if (!result.contains(d)) result.add(d);
            return result.toArray(Direction[]::new);
        }
        public Direction[] itemOrder() { return itemOrder; }
        public Direction[] fluidOrder() { return fluidOrder; }
        public Direction[] energyOrder() { return energyOrder; }
        public int insertMask() { return insertMask; } public int extractMask() { return extractMask; }
        public int fillMask() { return fillMask; } public int drainMask() { return drainMask; }
        public int receiveMask() { return receiveMask; } public int extractEnergyMask() { return extractEnergyMask; }
    }
    public void assign(BlockPos controller, int task, RecipeHolder<?> recipe) {
        if (entity instanceof PartsAssemblerBlockEntity p && recipe.value() instanceof PartsAssemblingRecipe r) p.setSelectedMode(r.mode().ordinal());
        var tag = entity.getPersistentData();
        tag.putLong(OWNER, controller.asLong()); tag.putInt(TASK, task); tag.putString(RECIPE, recipe.id().toString());
        entity.setChanged();
    }
    public void release() {
        var tag = entity.getPersistentData(); tag.remove(OWNER); tag.remove(TASK); tag.remove(RECIPE); entity.setChanged();
    }
    /** An unloaded owner is paused, never interpreted as permission to restart independently. */
    public static boolean mayTick(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !be.getPersistentData().contains(OWNER)) return true;
        BlockPos owner = BlockPos.of(be.getPersistentData().getLong(OWNER));
        if (!level.hasChunkAt(owner)) return false;
        if (level.getBlockEntity(owner) instanceof AssemblyLineControllerBlockEntity controller)
            return controller.mayRun(pos, be.getPersistentData().getInt(TASK));
        AssemblyLineMachine worker = at(level, pos);
        if (worker != null) worker.release();
        return true;
    }
    public static ResourceLocation assignedRecipe(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getPersistentData().contains(RECIPE)
            ? ResourceLocation.tryParse(be.getPersistentData().getString(RECIPE)) : null;
    }
    public static void consumeAssignedEnergy(IEnergyStorage storage, int cost) {
        // FE transfer limits must not discount an assigned operation's recipe cost.
        while (cost > 0) {
            int paid = storage.extractEnergy(cost, false);
            if (paid == 0) throw new IllegalStateException("Assigned machine cannot consume recipe energy");
            cost -= paid;
        }
    }
}
