package net.crystalnexus.block.entity;

import net.crystalnexus.block.GravitationalArrayControllerBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.recipe.GravitationalArrayRecipe;
import net.crystalnexus.recipe.GravitationalArrayCostSchedule;
import net.crystalnexus.world.inventory.GravitationalArrayMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

public final class GravitationalArrayControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int TANK_CAPACITY = 1_000_000;
    private static final double FORMATION_Y_OFFSET = -10.0D;
    private static final int VALIDATION_INTERVAL = 20;
    private static final int OUTPUT_SLOT = 4;
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "gravitational_array");
    private NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);
    private final FluidTank temporalFluid = new FluidTank(TANK_CAPACITY,
        stack -> stack.is(CrystalnexusModFluids.TEMPORAL_ESSENCE.get())) {
        @Override protected void onContentsChanged() { sync(); }
    };
    private final List<BlockPos> energyInputs = new ArrayList<>();
    private final List<BlockPos> fluidInputs = new ArrayList<>();
    @Nullable private StructureNbtValidator.Match structure;
    @Nullable private Vec3 formationCenter;
    private boolean formed;
    @Nullable private ResourceLocation activeRecipe;
    private int progress;
    private int activeDuration;
    private long consumedEnergy;
    private int consumedFluid;
    private int validationDelay;

    public GravitationalArrayControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.GRAVITATIONAL_ARRAY_CONTROLLER.get(), pos, state);
    }

    public FluidTank getTemporalFluidTank() { return temporalFluid; }
    public int getProgress() { return progress; }
    public int getActiveDuration() { return activeDuration; }
    public boolean isFormed() { return formed; }
    @Nullable public Vec3 getFormationCenter() { return formationCenter; }
    @Nullable public Vec3 getFormationRenderCenter() {
        return formationCenter == null ? null : formationCenter.add(0.0D, FORMATION_Y_OFFSET, 0.0D);
    }
    public Optional<GravitationalArrayRecipe> getActiveRecipeForRender() {
        if (level == null || activeRecipe == null) return Optional.empty();
        return level.getRecipeManager().getAllRecipesFor(GravitationalArrayRecipe.Type.INSTANCE).stream()
            .filter(holder -> holder.id().equals(activeRecipe)).map(RecipeHolder::value).findFirst();
    }

    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        return structure != null;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (validationDelay-- <= 0) {
            validateStructure(serverLevel);
            validationDelay = VALIDATION_INTERVAL;
        }
        if (structure == null) return;
        relayFluid();

        RecipeHolder<GravitationalArrayRecipe> holder = activeRecipe == null
            ? findRecipe(serverLevel).orElse(null) : findRecipe(serverLevel, activeRecipe).orElse(null);
        if (holder == null) return;
        GravitationalArrayRecipe recipe = holder.value();
        if (activeRecipe != null && activeDuration == 0) activeDuration = recipe.duration();
        if (!canFinish(recipe) || recipe.consumptionPlan(inputStacks()).length == 0) return;
        if (activeRecipe == null) {
            activeRecipe = holder.id();
            progress = 0;
            activeDuration = recipe.duration();
            consumedEnergy = 0;
            consumedFluid = 0;
            sync();
        }

        if ((serverLevel.getGameTime() & 1L) == 0L) renderFormation(serverLevel, recipe);
        long nextEnergy = GravitationalArrayCostSchedule.cumulative(recipe.energy(), progress + 1, recipe.duration());
        int nextFluid = (int) GravitationalArrayCostSchedule.cumulative(recipe.temporalFluid(), progress + 1, recipe.duration());
        long energyCost = nextEnergy - consumedEnergy;
        int fluidCost = nextFluid - consumedFluid;
        if (temporalFluid.getFluidAmount() < fluidCost) return;

        consumedEnergy += extractEnergy(energyCost);
        if (consumedEnergy < nextEnergy) return;
        temporalFluid.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
        consumedFluid = nextFluid;
        progress++;
        setChanged();
        if (progress >= recipe.duration()) complete(serverLevel, recipe);
        else if (progress % 20 == 0) sync();
    }

    private void validateStructure(ServerLevel level) {
        Direction facing = getBlockState().getValue(GravitationalArrayControllerBlock.FACING);
        Optional<StructureNbtValidator.Match> match = StructureNbtValidator.validate(level, STRUCTURE,
            worldPosition, facing, CrystalnexusModBlocks.GRAVITATIONAL_ARRAY_CONTROLLER.get(),
            GravitationalArrayControllerBlock.FACING,
            Map.of(CrystalnexusModBlocks.TITANIUM_BLOCK.get(), Set.of(
                CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get(), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())),
            true, true);
        List<BlockPos> substitutions = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
        List<BlockPos> nextEnergy = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())).toList();
        List<BlockPos> nextFluid = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())).toList();
        for (BlockPos old : List.copyOf(energyInputs)) {
            if (!nextEnergy.contains(old) && level.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input)
                input.unbindGravitationalController(worldPosition);
        }
        energyInputs.clear();
        for (BlockPos pos : nextEnergy) {
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) {
                input.bindGravitationalController(worldPosition);
                energyInputs.add(pos);
            }
        }
        for (BlockPos old : List.copyOf(fluidInputs)) {
            if (!nextFluid.contains(old) && level.getBlockEntity(old) instanceof MachineFluidInputBlockEntity input)
                input.unbindGravitationalController(worldPosition);
        }
        fluidInputs.clear();
        for (BlockPos pos : nextFluid) {
            if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) {
                input.bindGravitationalController(worldPosition);
                fluidInputs.add(pos);
            }
        }
        boolean wasFormed = formed;
        Vec3 previousCenter = formationCenter;
        structure = !energyInputs.isEmpty() && !fluidInputs.isEmpty()
            && energyInputs.size() + fluidInputs.size() == substitutions.size() ? match.orElse(null) : null;
        formed = structure != null;
        formationCenter = structure == null ? null : structure.center();
        if (wasFormed != formed || !Objects.equals(previousCenter, formationCenter)) sync();
    }

    private void relayFluid() {
        int space = temporalFluid.getSpace();
        for (BlockPos pos : fluidInputs) {
            if (space <= 0) return;
            if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input)
                space -= input.transferTo(temporalFluid, space, worldPosition);
        }
    }

    private Optional<RecipeHolder<GravitationalArrayRecipe>> findRecipe(ServerLevel level) {
        return level.getRecipeManager().getAllRecipesFor(GravitationalArrayRecipe.Type.INSTANCE).stream()
            .filter(holder -> holder.value().consumptionPlan(inputStacks()).length != 0)
            .filter(holder -> canFinish(holder.value())).findFirst();
    }

    private Optional<RecipeHolder<GravitationalArrayRecipe>> findRecipe(ServerLevel level, ResourceLocation id) {
        return level.getRecipeManager().getAllRecipesFor(GravitationalArrayRecipe.Type.INSTANCE).stream()
            .filter(holder -> holder.id().equals(id)).findFirst();
    }

    private List<ItemStack> inputStacks() {
        return List.of(stacks.get(0), stacks.get(1), stacks.get(2), stacks.get(3));
    }

    private boolean canFinish(GravitationalArrayRecipe recipe) {
        ItemStack output = stacks.get(OUTPUT_SLOT);
        ItemStack result = recipe.output();
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private long extractEnergy(long requested) {
        long remaining = requested;
        for (BlockPos pos : energyInputs) {
            if (!(level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input)
                || !input.isBoundTo(worldPosition)) continue;
            remaining -= input.getEnergyStorage().extractEnergy((int) Math.min(Integer.MAX_VALUE, remaining), false);
            if (remaining <= 0) break;
        }
        return requested - remaining;
    }

    private void complete(ServerLevel level, GravitationalArrayRecipe recipe) {
        int[] plan = recipe.consumptionPlan(inputStacks());
        if (plan.length == 0) return;
        for (int slot = 0; slot < plan.length; slot++) stacks.get(slot).shrink(plan[slot]);
        ItemStack result = recipe.output();
        if (stacks.get(OUTPUT_SLOT).isEmpty()) stacks.set(OUTPUT_SLOT, result);
        else stacks.get(OUTPUT_SLOT).grow(result.getCount());
        Vec3 center = getFormationRenderCenter();
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 60, 2.2, 2.2, 2.2, 0.28);
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.55F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.7F, 1.35F);
        activeRecipe = null;
        progress = 0;
        activeDuration = 0;
        consumedEnergy = 0;
        consumedFluid = 0;
        sync();
    }

    private void renderFormation(ServerLevel level, GravitationalArrayRecipe recipe) {
        Vec3 center = getFormationRenderCenter();
        float fraction = Math.min(1.0F, progress / (float) recipe.duration());
        GravitationalArrayRecipe.Visuals visuals = recipe.visuals().orElse(new GravitationalArrayRecipe.Visuals(1, 0.85F, 0.25F, 1));
        float ignition = Math.max(0, (fraction - 0.75F) / 0.25F);
        Vector3f color = new Vector3f(0.65F + (visuals.red() - 0.65F) * ignition,
            0.25F + (visuals.green() - 0.25F) * ignition,
            0.9F + (visuals.blue() - 0.9F) * ignition);
        float radius = fraction < 0.3F ? 2.4F - fraction * 3.0F : 1.5F + fraction * 1.4F;
        int paths = fraction < 0.3F ? 1 : fraction < 0.75F ? 2 : 3;
        double time = level.getGameTime() * (0.08D + ignition * 0.12D);
        DustParticleOptions dust = new DustParticleOptions(color, Math.max(0.15F, fraction * visuals.scale()));
        for (int path = 0; path < paths; path++) {
            double angle = time + path * Math.PI * 2 / paths;
            double y = center.y + Math.sin(angle * 1.7D) * radius * 0.35D;
            level.sendParticles(dust, center.x + Math.cos(angle) * radius, y,
                center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
        int coreCount = fraction < 0.3F ? 1 : fraction < 0.75F ? 3 : 5;
        level.sendParticles(dust, center.x, center.y, center.z, coreCount,
            0.04D + fraction * 0.22D, 0.04D + fraction * 0.22D, 0.04D + fraction * 0.22D, 0.01D);
        if (fraction >= 0.3F && level.getGameTime() % 10 == 0)
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 3,
                radius, radius * 0.45D, radius, 0.16D);
        if (fraction >= 0.75F && level.getGameTime() % 20 == 0)
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 10,
                0.8D + fraction, 0.8D + fraction, 0.8D + fraction, 0.12D);
    }

    public void onControllerRemoved() {
        if (level == null) return;
        for (BlockPos pos : energyInputs) {
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input)
                input.unbindGravitationalController(worldPosition);
        }
        for (BlockPos pos : fluidInputs) {
            if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input)
                input.unbindGravitationalController(worldPosition);
        }
        energyInputs.clear();
        fluidInputs.clear();
        structure = null;
        formationCenter = null;
        formed = false;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.gravitational_array_controller"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot != OUTPUT_SLOT; }
    @Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, 5).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, registries);
        if (tag.get("temporalFluid") instanceof CompoundTag fluid) temporalFluid.readFromNBT(registries, fluid);
        activeRecipe = tag.contains("activeRecipe") ? ResourceLocation.tryParse(tag.getString("activeRecipe")) : null;
        formed = tag.getBoolean("formed");
        formationCenter = tag.contains("formationX", Tag.TAG_DOUBLE)
            ? new Vec3(tag.getDouble("formationX"), tag.getDouble("formationY"), tag.getDouble("formationZ")) : null;
        progress = tag.getInt("progress");
        activeDuration = tag.getInt("activeDuration");
        consumedEnergy = tag.getLong("consumedEnergy");
        consumedFluid = tag.getInt("consumedFluid");
    }

    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, registries);
        tag.put("temporalFluid", temporalFluid.writeToNBT(registries, new CompoundTag()));
        if (activeRecipe != null) tag.putString("activeRecipe", activeRecipe.toString());
        tag.putBoolean("formed", formed);
        if (formationCenter != null) {
            tag.putDouble("formationX", formationCenter.x);
            tag.putDouble("formationY", formationCenter.y);
            tag.putDouble("formationZ", formationCenter.z);
        }
        tag.putInt("progress", progress);
        tag.putInt("activeDuration", activeDuration);
        tag.putLong("consumedEnergy", consumedEnergy);
        tag.putInt("consumedFluid", consumedFluid);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new GravitationalArrayMenu(id, inventory, this);
    }
}
