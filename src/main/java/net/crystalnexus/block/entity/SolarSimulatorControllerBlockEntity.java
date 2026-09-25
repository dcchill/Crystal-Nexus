package net.crystalnexus.block.entity;

import net.crystalnexus.block.SolarSimulatorControllerBlock;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.recipe.GravitationalArrayCostSchedule;
import net.crystalnexus.recipe.DysonOutput;
import net.crystalnexus.world.inventory.SolarSimulatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

public final class SolarSimulatorControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
    public static final int DURATION = 3;
    public static final int DYSON_SLOT_LIMIT = 1024;
    private static final int ENERGY_PER_ITEM = 600_000;
    private static final int STAR_SLOT = 4;
    private static final int REQUIRED_ENERGY_TRANSFER = GravitationalArrayCostSchedule.maximumStep(
        (long) STAR_SLOT * 8 * ENERGY_PER_ITEM, DURATION);
    private static final int VALIDATION_INTERVAL = 20;
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "solar_sim");
    private static final List<TagKey<Item>> TERRA = tags("raw_materials/iron", "raw_materials/copper", "raw_materials/coal", "raw_materials/tin", "raw_materials/silver");
    private static final List<TagKey<Item>> CAELUS = tags("raw_materials/gold", "raw_materials/lead", "dusts/redstone", "raw_materials/nickel");
    private static final List<TagKey<Item>> BOREAS = tags("gems/diamond", "gems/quartz", "gems/certus_quartz", "raw_materials/azurine");
    private static final List<TagKey<Item>> METEOR = tags("raw_materials/obsidrax", "raw_materials/uranium", "raw_materials/platinum");
    private static final List<TagKey<Item>> NOX = tags("gems/amethyst");

    private NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);
    private final int[] dysonCounts = new int[STAR_SLOT];
    private boolean dysonMode;
    private final GeneratorEnergyStorage dysonEnergy = new GeneratorEnergyStorage(1_000_000_000, Integer.MAX_VALUE, this::sync);
    private final List<BlockPos> energyOutputs = new ArrayList<>();
	private final EnergyStorage energyStorage = new EnergyStorage(
		Math.max(CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.capacity(), REQUIRED_ENERGY_TRANSFER),
		Math.max(CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.maxReceive(), REQUIRED_ENERGY_TRANSFER),
		Math.max(CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.maxExtract(), REQUIRED_ENERGY_TRANSFER)) {
		@Override public int receiveEnergy(int amount, boolean simulate) {
            if (dysonMode) return 0;
			int moved = super.receiveEnergy(amount, simulate); if (!simulate && moved > 0) sync(); return moved;
		}
		@Override public int extractEnergy(int amount, boolean simulate) {
			int moved = super.extractEnergy(amount, simulate); if (!simulate && moved > 0) sync(); return moved;
		}
	};
    private final List<BlockPos> energyInputs = new ArrayList<>();
    private final List<BlockPos> outputs = new ArrayList<>();
    private final List<BlockPos> fluidOutputs = new ArrayList<>();
    private final FluidTank[] fluidOutputTanks = { createFluidOutputTank(), createFluidOutputTank(), createFluidOutputTank() };
    private final IFluidHandler fluidOutput = new IFluidHandler() {
        @Override public int getTanks() { return fluidOutputTanks.length; }
        @Override public FluidStack getFluidInTank(int tank) { return fluidOutputTanks[tank].getFluid(); }
        @Override public int getTankCapacity(int tank) { return fluidOutputTanks[tank].getCapacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return fluidOutputTanks[tank].isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) {
            for (FluidTank tank : fluidOutputTanks)
                if (!tank.isEmpty() && FluidStack.isSameFluidSameComponents(tank.getFluid(), resource))
                    return tank.fill(resource, action);
            for (FluidTank tank : fluidOutputTanks)
                if (tank.isEmpty()) return tank.fill(resource, action);
            return 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            for (FluidTank tank : fluidOutputTanks)
                if (FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) return tank.drain(resource, action);
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            for (FluidTank tank : fluidOutputTanks) if (!tank.isEmpty()) return tank.drain(amount, action);
            return FluidStack.EMPTY;
        }
    };
    private boolean formed;
    @Nullable private Vec3 formationCenter;
    private boolean renderActive;
    private int inactiveRenderTicks;
    private int progress;
    private long consumedEnergy;
    private int validationDelay;

    public SolarSimulatorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.SOLAR_SIMULATOR_CONTROLLER.get(), pos, state);
    }

    private FluidTank createFluidOutputTank() {
        return new FluidTank(4000) {
            @Override protected void onContentsChanged() { sync(); }
        };
    }

    public boolean isFormed() { return formed; }
    public boolean isDysonMode() { return dysonMode; }
    public int getDysonCount(int slot) { return slot >= 0 && slot < STAR_SLOT ? dysonCounts[slot] : 0; }
    public int getDysonStructures() { return dysonTotal(CrystalnexusModItems.DYSON_STRUCTURE.get()); }
    private DysonOutput.Result dysonOutput() {
        return DysonOutput.calculate(getDysonStructures(), dysonTotal(CrystalnexusModItems.CARBON_SOLAR_SHEET.get()),
            dysonTotal(CrystalnexusModItems.SOLAR_SHEET.get()), starMultiplier(stacks.get(STAR_SLOT)));
    }
    public int getDysonCarbonSheets() { return dysonOutput().carbonSheets(); }
    public int getDysonSolarSheets() { return dysonOutput().solarSheets(); }
    public int getDysonPotentialFePerTick() { return dysonOutput().fePerTick(); }
    public int getDysonEnergyStored() { return dysonEnergy.getEnergyStored(); }
    private int dysonTotal(Item item) {
        int total = 0;
        for (int slot = 0; slot < STAR_SLOT; slot++) if (stacks.get(slot).is(item)) total += dysonCounts[slot];
        return total;
    }
    public boolean setDysonMode(boolean next) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (next == dysonMode) return true;
        for (int slot = 0; slot < STAR_SLOT; slot++) if (!stacks.get(slot).isEmpty() || dysonCounts[slot] != 0) return false;
        dysonMode = next;
        progress = 0;
        consumedEnergy = 0;
        renderActive = false;
        inactiveRenderTicks = 0;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        sync();
        return true;
    }
    public int insertDyson(int slot, ItemStack input, int amount, boolean simulate) {
        if (!dysonMode || slot < 0 || slot >= STAR_SLOT || !canPlaceItem(slot, input)) return 0;
        ItemStack existing = stacks.get(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, input)) return 0;
        int moved = Math.min(Math.min(amount, input.getCount()), DYSON_SLOT_LIMIT - dysonCounts[slot]);
        if (moved > 0 && !simulate) {
            if (existing.isEmpty()) stacks.set(slot, input.copyWithCount(1));
            dysonCounts[slot] += moved;
            sync();
        }
        return moved;
    }
    public ItemStack extractDyson(int slot, int amount, boolean simulate) {
        if (!dysonMode || slot < 0 || slot >= STAR_SLOT || amount <= 0 || dysonCounts[slot] <= 0) return ItemStack.EMPTY;
        ItemStack icon = stacks.get(slot);
        int moved = Math.min(Math.min(amount, icon.getMaxStackSize()), dysonCounts[slot]);
        ItemStack result = icon.copyWithCount(moved);
        if (!simulate) {
            dysonCounts[slot] -= moved;
            if (dysonCounts[slot] == 0) stacks.set(slot, ItemStack.EMPTY);
            sync();
        }
        return result;
    }
    public void dropDysonContents() {
        if (!dysonMode || level == null || level.isClientSide) return;
        for (int slot = 0; slot < STAR_SLOT; slot++) {
            while (dysonCounts[slot] > 0) {
                ItemStack dropped = extractDyson(slot, dysonCounts[slot], false);
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), dropped);
            }
        }
    }
    public int getProgress() { return progress; }
    public int getDuration() { return DURATION; }
    public boolean isRenderActive() { return formed && renderActive; }
    @Nullable public Vec3 getFormationCenter() { return formationCenter; }

    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        return formed;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (validationDelay-- <= 0) {
            validateStructure(serverLevel);
            validationDelay = VALIDATION_INTERVAL;
        }
        if (dysonMode) {
            if (formed) {
                for (BlockPos pos : energyOutputs) if (serverLevel.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output)
                    output.pushEnergy();
            }
            int produced = formed ? dysonEnergy.generateEnergy(getDysonPotentialFePerTick(), false) : 0;
            if (produced > 0) markRenderActive(); else markRenderInactive();
            return;
        }
        int multiplier = starMultiplier(stacks.get(STAR_SLOT));
        int planets = planetCount();
        if (!formed || multiplier == 0 || planets == 0) {
            if (progress != 0 || consumedEnergy != 0 || renderActive) {
                progress = 0; consumedEnergy = 0; renderActive = false; inactiveRenderTicks = 0; sync();
            }
            return;
        }

        // Older worlds may have saved the former false-complete value.
        progress = Math.min(progress, DURATION - 1);
        List<ItemStack> results = progress == DURATION - 1 && !outputs.isEmpty() ? createResults(serverLevel, multiplier) : List.of();
        List<FluidStack> fluidResults = progress == DURATION - 1 && !fluidOutputs.isEmpty() ? createFluidResults(multiplier) : List.of();
        if (progress == DURATION - 1 && (results.isEmpty() && fluidResults.isEmpty() || !canFit(results, fluidResults))) { markRenderInactive(); return; }

        long totalEnergy = (long) planets * multiplier * ENERGY_PER_ITEM;
        long nextEnergy = GravitationalArrayCostSchedule.cumulative(totalEnergy, progress + 1, DURATION);
        int extracted = extractEnergy((int) (nextEnergy - consumedEnergy), false);
        consumedEnergy += extracted;
        if (extracted > 0) markRenderActive(); else markRenderInactive();
        if (consumedEnergy < nextEnergy) return;

        progress++;
        if (progress >= DURATION) {
            insert(results, fluidResults);
            progress = 0;
            consumedEnergy = 0;
            sync();
        } else if (progress % 20 == 0) sync();
    }

    private void validateStructure(ServerLevel level) {
        Optional<StructureNbtValidator.Match> match = StructureNbtValidator.validate(level, STRUCTURE, worldPosition,
            getBlockState().getValue(SolarSimulatorControllerBlock.FACING), CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get(),
            SolarSimulatorControllerBlock.FACING, Map.of(
                CrystalnexusModBlocks.TUNGSTEN_BLOCK.get(), Set.of(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(), CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get(), CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get()),
                CrystalnexusModBlocks.TUNGSTEN_MACHINE_FRAME.get(), Set.of(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(), CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get(), CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())),
            Set.of(CrystalnexusModBlocks.CARBON_GLASS.get(), CrystalnexusModBlocks.COOLING_COIL.get(),
                CrystalnexusModBlocks.GRAVITY_CONTROL_POINT.get(), CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get(),
                CrystalnexusModBlocks.TUNGSTEN_BLOCK.get(), CrystalnexusModBlocks.TUNGSTEN_MACHINE_FRAME.get()),
            true, false, Map.of(
                CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get(), 1,
                CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get(), 2,
                CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(), 2,
                CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get(), 2,
                CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get(), 2));
        List<BlockPos> previousEnergyInputs = List.copyOf(energyInputs);
        List<BlockPos> previousEnergyOutputs = List.copyOf(energyOutputs);
        List<BlockPos> previousFluidOutputs = List.copyOf(fluidOutputs);
        energyInputs.clear();
        energyOutputs.clear();
        outputs.clear();
        fluidOutputs.clear();
        match.ifPresent(found -> {
            energyInputs.addAll(found.substitutionPositions().stream()
                .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())).toList());
            energyOutputs.addAll(found.substitutionPositions().stream()
                .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())).toList());
            outputs.addAll(found.substitutionPositions().stream()
                .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get())).toList());
            fluidOutputs.addAll(found.substitutionPositions().stream()
                .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get())).toList());
        });
        for (BlockPos old : previousEnergyInputs) {
            if (!energyInputs.contains(old) && level.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input)
                input.unbindController(worldPosition);
        }
        for (BlockPos old : previousEnergyOutputs) if (!energyOutputs.contains(old) || !dysonMode)
            if (level.getBlockEntity(old) instanceof MachineEnergyOutputBlockEntity output) output.unbindController(worldPosition);
        if (!dysonMode) energyOutputs.clear();
        for (BlockPos old : previousFluidOutputs) {
            if (!fluidOutputs.contains(old) && level.getBlockEntity(old) instanceof MultiblockFluidOutputBlockEntity output)
                output.unbindController(worldPosition);
        }
        for (BlockPos outputPos : fluidOutputs) {
            if (level.getBlockEntity(outputPos) instanceof MultiblockFluidOutputBlockEntity output)
                output.bindController(worldPosition);
        }
        energyInputs.removeIf(pos -> {
            if (dysonMode) return true;
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) {
                input.bindController(worldPosition);
                return false;
            }
            return true;
        });
        energyOutputs.removeIf(pos -> {
            if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) {
                if (dysonMode) output.bindController(worldPosition);
                return false;
            }
            return true;
        });
        boolean next = dysonMode ? match.isPresent() && !energyOutputs.isEmpty()
            : match.isPresent() && !energyInputs.isEmpty()
            && (!outputs.isEmpty() && outputs.stream().allMatch(pos -> level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity)
                || !fluidOutputs.isEmpty() && fluidOutputs.stream().allMatch(pos -> level.getBlockEntity(pos) instanceof MultiblockFluidOutputBlockEntity));
        Vec3 nextCenter = next ? match.orElseThrow().center() : null;
        if (formed != next || !Objects.equals(formationCenter, nextCenter)) {
            formed = next; formationCenter = nextCenter; sync();
        } else formed = next;
    }

    private List<ItemStack> createResults(ServerLevel level, int multiplier) {
        List<ItemStack> results = new ArrayList<>();
        for (int slot = 0; slot < STAR_SLOT; slot++) {
            if (stacks.get(slot).is(CrystalnexusModItems.NOX.get())) {
                Item item = level.random.nextFloat() < 0.05F ? CrystalnexusModItems.DARK_MATTER.get()
                    : level.random.nextBoolean() ? net.minecraft.world.item.Items.AMETHYST_SHARD : net.minecraft.world.item.Items.ECHO_SHARD;
                results.add(new ItemStack(item, multiplier));
                continue;
            }
            ItemStack cometMaterial = net.crystalnexus.item.ResourceCometItem.material(stacks.get(slot));
            if (!cometMaterial.isEmpty()) {
                results.add(cometMaterial.copyWithCount(multiplier));
                continue;
            }
            List<TagKey<Item>> pool = planetPool(stacks.get(slot));
            List<Item> available = pool.stream().map(this::firstItem).flatMap(Optional::stream).toList();
            if (!available.isEmpty()) results.add(new ItemStack(available.get(level.random.nextInt(available.size())), multiplier));
        }
        return results;
    }

    private List<FluidStack> createFluidResults(int multiplier) {
        List<FluidStack> results = new ArrayList<>();
        for (int slot = 0; slot < STAR_SLOT; slot++) {
            FluidStack result = fluidResult(stacks.get(slot), multiplier);
            if (!result.isEmpty()) results.add(result);
        }
        return results;
    }

    private FluidStack fluidResult(ItemStack stack, int multiplier) {
        if (stack.is(CrystalnexusModItems.CAELUS.get()))
            return new FluidStack(CrystalnexusModFluids.NITROGEN.get(), 50 * multiplier);
        if (stack.is(CrystalnexusModItems.NOX.get()))
            return new FluidStack(CrystalnexusModFluids.ARGON.get(), 10 * multiplier);
        if (stack.is(CrystalnexusModItems.TERRA.get()))
            return new FluidStack(CrystalnexusModFluids.ATMOSPHERE.get(), 25 * multiplier);
        return FluidStack.EMPTY;
    }

    private Optional<Item> firstItem(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.getTag(tag).flatMap(items -> items.stream().findFirst()).map(holder -> holder.value());
    }

    private boolean canFit(List<ItemStack> results, List<FluidStack> fluidResults) {
        if (!fluidResults.isEmpty() && !mergeFluidResults(fluidResults).stream()
            .allMatch(result -> fluidOutput.fill(result, IFluidHandler.FluidAction.SIMULATE) == result.getAmount())) return false;
        List<ItemStack> snapshot = new ArrayList<>();
        for (BlockPos pos : outputs) if (level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity output)
            for (int slot = 0; slot < output.getContainerSize(); slot++) snapshot.add(output.getItem(slot).copy());
        for (ItemStack result : results) if (!insertInto(snapshot, result.copy())) return false;
        return true;
    }

    private void insert(List<ItemStack> results, List<FluidStack> fluidResults) {
        if (!fluidResults.isEmpty())
            for (FluidStack result : mergeFluidResults(fluidResults))
                fluidOutput.fill(result, IFluidHandler.FluidAction.EXECUTE);
        for (ItemStack result : results) {
            ItemStack remaining = result.copy();
            for (BlockPos pos : outputs) {
                if (!(level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity output)) continue;
                for (int slot = 0; slot < output.getContainerSize() && !remaining.isEmpty(); slot++) {
                    ItemStack current = output.getItem(slot);
                    if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remaining)) continue;
                    int room = current.isEmpty() ? remaining.getMaxStackSize() : current.getMaxStackSize() - current.getCount();
                    int moved = Math.min(room, remaining.getCount());
                    if (current.isEmpty()) output.setItem(slot, remaining.copyWithCount(moved)); else current.grow(moved);
                    remaining.shrink(moved);
                }
                output.setChanged();
            }
        }
    }

    private static List<FluidStack> mergeFluidResults(List<FluidStack> results) {
        List<FluidStack> merged = new ArrayList<>();
        for (FluidStack result : results) {
            FluidStack existing = merged.stream()
                .filter(candidate -> FluidStack.isSameFluidSameComponents(candidate, result))
                .findFirst().orElse(null);
            if (existing == null) merged.add(result.copy());
            else existing.grow(result.getAmount());
        }
        return merged;
    }

    private static boolean insertInto(List<ItemStack> slots, ItemStack remaining) {
        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            ItemStack current = slots.get(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remaining)) continue;
            int room = current.isEmpty() ? remaining.getMaxStackSize() : current.getMaxStackSize() - current.getCount();
            int moved = Math.min(room, remaining.getCount());
            if (current.isEmpty()) slots.set(slot, remaining.copyWithCount(moved)); else current.grow(moved);
            remaining.shrink(moved);
        }
        return remaining.isEmpty();
    }

    private int extractEnergy(int amount, boolean simulate) {
		return energyStorage.extractEnergy(amount, simulate);
    }

    @Override public EnergyStorage multiblockEnergyInput() { return energyStorage; }
    @Override public IEnergyStorage multiblockEnergyOutput() { return dysonMode && formed ? dysonEnergy : null; }
    @Override public boolean acceptsMultiblockPort(BlockPos pos) { return energyInputs.contains(pos) || energyOutputs.contains(pos) || fluidOutputs.contains(pos); }
    @Override public IFluidHandler multiblockFluidOutput() { return fluidOutput; }

    public void onControllerRemoved() {
        if (level != null) for (BlockPos pos : energyInputs)
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
        energyInputs.clear();
        if (level != null) for (BlockPos pos : energyOutputs)
            if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) output.unbindController(worldPosition);
        energyOutputs.clear();
        outputs.clear();
        formed = false;
        formationCenter = null;
        renderActive = false;
    }

    private void markRenderActive() {
        inactiveRenderTicks = 0;
        if (!renderActive) { renderActive = true; sync(); }
    }

    private void markRenderInactive() {
        if (renderActive && ++inactiveRenderTicks >= 20) { renderActive = false; inactiveRenderTicks = 0; sync(); }
    }

    private int planetCount() { return (int) IntStream.range(0, STAR_SLOT).filter(slot -> !planetPool(stacks.get(slot)).isEmpty()
        || !outputs.isEmpty() && !net.crystalnexus.item.ResourceCometItem.material(stacks.get(slot)).isEmpty()).count(); }
    private static List<TagKey<Item>> planetPool(ItemStack stack) {
        if (stack.is(CrystalnexusModItems.METEOR.get())) return METEOR;
        if (stack.is(CrystalnexusModItems.TERRA.get())) return TERRA;
        if (stack.is(CrystalnexusModItems.CAELUS.get())) return CAELUS;
        if (stack.is(CrystalnexusModItems.BOREAS.get())) return BOREAS;
		if (stack.is(CrystalnexusModItems.NOX.get())) return NOX;
        return List.of();
    }
    private static int starMultiplier(ItemStack stack) {
        if (stack.is(CrystalnexusModItems.YELLOW_DWARF_STAR.get())) return 1;
        if (stack.is(CrystalnexusModItems.ORANGE_STAR.get())) return 2;
        if (stack.is(CrystalnexusModItems.BLUE_STAR.get())) return 4;
        if (stack.is(CrystalnexusModItems.PINK_STAR.get())) return 8;
        return 0;
    }
    private static List<TagKey<Item>> tags(String... paths) {
        return java.util.Arrays.stream(paths).map(path -> TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path))).toList();
    }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.solar_simulator_controller"); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == STAR_SLOT) return starMultiplier(stack) > 0;
        if (slot < 0 || slot >= STAR_SLOT) return false;
        if (dysonMode) return stack.is(CrystalnexusModItems.DYSON_STRUCTURE.get())
            || stack.is(CrystalnexusModItems.SOLAR_SHEET.get()) || stack.is(CrystalnexusModItems.CARBON_SOLAR_SHEET.get());
        return !planetPool(stack).isEmpty() || !net.crystalnexus.item.ResourceCometItem.material(stack).isEmpty();
    }
    @Override public int getMaxStackSize() { return dysonMode ? DYSON_SLOT_LIMIT : super.getMaxStackSize(); }
    @Override public ItemStack removeItem(int slot, int amount) {
        return dysonMode && slot >= 0 && slot < STAR_SLOT ? extractDyson(slot, amount, false) : super.removeItem(slot, amount);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return dysonMode && slot >= 0 && slot < STAR_SLOT ? extractDyson(slot, DYSON_SLOT_LIMIT, false) : super.removeItemNoUpdate(slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (dysonMode && slot >= 0 && slot < STAR_SLOT) {
            // Hopper/container merges write an icon with its added amount included; do not replace the reserve.
            if (level != null && !level.isClientSide && !stack.isEmpty()) {
                ItemStack icon = stacks.get(slot);
                if (icon.isEmpty()) insertDyson(slot, stack, stack.getCount(), false);
                else if (ItemStack.isSameItemSameComponents(icon, stack) && stack.getCount() > 1)
                    insertDyson(slot, stack, stack.getCount() - 1, false);
            }
            return;
        }
        super.setItem(slot, stack);
    }
    @Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, 5).toArray(); }
    // Normal hopper extraction calls removeItem; insertion calls setItem with an icon plus the inserted amount.
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack) && (!dysonMode || slot == STAR_SLOT ||
            dysonCounts[slot] < DYSON_SLOT_LIMIT && (stacks.get(slot).isEmpty() || ItemStack.isSameItemSameComponents(stacks.get(slot), stack)));
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) { return new SolarSimulatorMenu(id, inventory, this); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, registries);
        dysonMode = tag.getBoolean("dysonMode");
        for (int slot = 0; slot < STAR_SLOT; slot++) {
            int count = tag.getInt("dysonCount" + slot);
            if (dysonMode && canPlaceItem(slot, stacks.get(slot)) && count > 0 && count <= DYSON_SLOT_LIMIT) {
                dysonCounts[slot] = count;
                stacks.set(slot, stacks.get(slot).copyWithCount(1));
            } else if (dysonMode) {
                dysonCounts[slot] = 0;
                stacks.set(slot, ItemStack.EMPTY);
            }
        }
		if (tag.get("energy") instanceof IntTag energy) energyStorage.deserializeNBT(registries, energy);
        if (tag.get("dysonEnergy") instanceof IntTag energy) dysonEnergy.deserializeNBT(registries, energy);
        for (int i = 0; i < fluidOutputTanks.length; i++)
            if (tag.get("fluidOutput" + i) instanceof CompoundTag fluid) fluidOutputTanks[i].readFromNBT(registries, fluid);
        formed = tag.getBoolean("formed"); progress = tag.getInt("progress"); consumedEnergy = tag.getLong("consumedEnergy");
        renderActive = tag.getBoolean("renderActive");
        formationCenter = tag.contains("formationX")
            ? new Vec3(tag.getDouble("formationX"), tag.getDouble("formationY"), tag.getDouble("formationZ")) : null;
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, registries);
        tag.putBoolean("dysonMode", dysonMode);
        for (int slot = 0; slot < STAR_SLOT; slot++) tag.putInt("dysonCount" + slot, dysonCounts[slot]);
		tag.put("energy", energyStorage.serializeNBT(registries));
        tag.put("dysonEnergy", dysonEnergy.serializeNBT(registries));
        for (int i = 0; i < fluidOutputTanks.length; i++)
            tag.put("fluidOutput" + i, fluidOutputTanks[i].writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("formed", formed); tag.putInt("progress", progress); tag.putLong("consumedEnergy", consumedEnergy);
        tag.putBoolean("renderActive", renderActive);
        if (formationCenter != null) {
            tag.putDouble("formationX", formationCenter.x); tag.putDouble("formationY", formationCenter.y); tag.putDouble("formationZ", formationCenter.z);
        }
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
