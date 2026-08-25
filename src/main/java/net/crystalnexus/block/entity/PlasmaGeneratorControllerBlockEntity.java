package net.crystalnexus.block.entity;

import net.crystalnexus.block.PlasmaGeneratorControllerBlock;
import net.crystalnexus.block.HeatingCoreBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.world.inventory.PlasmaGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PlasmaGeneratorControllerBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, MultiblockPortTarget {
    public static final int TANK_CAPACITY = 100;
    public static final int ARGON_PER_TICK = 1;
    public static final int GENERATION_PER_TICK = 512_000;
    private static final int VALIDATION_INTERVAL = 20;
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "plasma_generator");

    private final FluidTank argonTank = new FluidTank(TANK_CAPACITY,
        stack -> stack.is(CrystalnexusModFluids.ARGON.get())) {
        @Override protected void onContentsChanged() { sync(); }
    };
	private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(
		100_000_000, MachineEnergyOutputBlockEntity.MAX_TRANSFER, this::sync);
    private final List<BlockPos> fluidInputs = new ArrayList<>();
    private final List<BlockPos> energyOutputs = new ArrayList<>();
    private final List<BlockPos> heatingCores = new ArrayList<>();
    @Nullable private Vec3 formationCenter;
    private boolean formed;
    private boolean operating;
    private int outputPerTick;
    private String status = "Incomplete Structure";
    private int validationDelay;

    public PlasmaGeneratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.PLASMA_GENERATOR_CONTROLLER.get(), pos, state);
    }

    public FluidTank getArgonTank() { return argonTank; }
    public boolean isFormed() { return formed; }
    public boolean isOperating() { return formed && operating; }
    public int getOutputPerTick() { return outputPerTick; }
    public String getStatus() { return status; }
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
        energyOutputs.forEach(pos -> {
            if (serverLevel.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) output.pushEnergy();
        });

        if (!formed) { updateOperating(false, 0, "Incomplete Structure"); return; }
        if (argonTank.getFluidAmount() < ARGON_PER_TICK) { updateOperating(false, 0, "Waiting for Argon"); return; }

        int availableOutput = availableOutputCapacity(GENERATION_PER_TICK);
        if (availableOutput <= 0) { updateOperating(false, 0, "Energy Output Full"); return; }

        argonTank.drain(ARGON_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
        int generated = distributeEnergy(Math.min(GENERATION_PER_TICK, availableOutput));
        updateOperating(generated > 0, generated, generated > 0 ? "Generating" : "Energy Output Full");
    }

    private void validateStructure(ServerLevel level) {
        Optional<StructureNbtValidator.Match> match = StructureNbtValidator.validate(level, STRUCTURE, worldPosition,
            getBlockState().getValue(PlasmaGeneratorControllerBlock.FACING),
            CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get(), PlasmaGeneratorControllerBlock.FACING,
            Map.of(CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get(), Set.of(
                CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())),
            Set.of(CrystalnexusModBlocks.HEATING_CORE.get()), true, false);
        List<BlockPos> substitutions = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
        List<BlockPos> previousHeatingCores = List.copyOf(heatingCores);
        List<BlockPos> nextInputs = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())).toList();
        List<BlockPos> nextOutputs = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get())).toList();
        boolean nextFormed = match.isPresent() && !nextInputs.isEmpty()
            && nextInputs.size() + nextOutputs.size() == substitutions.size();
        heatingCores.clear();
        match.ifPresent(found -> heatingCores.addAll(found.positionsFor(CrystalnexusModBlocks.HEATING_CORE.get())));
        if (formed && operating && !nextFormed) {
            plasmaArcFailure(level, previousHeatingCores);
            return;
        }

        for (BlockPos old : List.copyOf(fluidInputs)) if (!nextInputs.contains(old)
            && level.getBlockEntity(old) instanceof MachineFluidInputBlockEntity input) input.unbindController(worldPosition);
        fluidInputs.clear();
        for (BlockPos pos : nextInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) {
            input.bindController(worldPosition);
            fluidInputs.add(pos);
        }
        for (BlockPos old : List.copyOf(energyOutputs)) if (!nextOutputs.contains(old)
            && level.getBlockEntity(old) instanceof MachineEnergyOutputBlockEntity output) output.unbindController(worldPosition);
        energyOutputs.clear();
        for (BlockPos pos : nextOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output) {
            output.bindController(worldPosition);
            energyOutputs.add(pos);
        }

        if (!nextFormed) setHeatingCoresActive(previousHeatingCores, false);
        Vec3 nextCenter = nextFormed ? match.orElseThrow().center() : null;
        if (formed != nextFormed || !Objects.equals(formationCenter, nextCenter)) {
            formed = nextFormed;
            formationCenter = nextCenter;
            sync();
        } else formed = nextFormed;
    }

	@Override public IFluidHandler multiblockFluidInput() { return argonTank; }

    private int availableOutputCapacity(int requested) {
		return energy.generateEnergy(requested, true);
    }

    private int distributeEnergy(int requested) {
		return energy.generateEnergy(requested, false);
    }

	@Override public IEnergyStorage multiblockEnergyOutput() { return energy; }

    private void updateOperating(boolean nextOperating, int nextOutput, String nextStatus) {
        setHeatingCoresActive(heatingCores, nextOperating);
        if (operating == nextOperating && outputPerTick == nextOutput && status.equals(nextStatus)) return;
        operating = nextOperating;
        outputPerTick = nextOutput;
        status = nextStatus;
        sync();
    }

    private void setHeatingCoresActive(List<BlockPos> cores, boolean active) {
        if (level == null) return;
        for (BlockPos pos : cores) {
            BlockState state = level.getBlockState(pos);
            if (state.is(CrystalnexusModBlocks.HEATING_CORE.get()) && state.getValue(HeatingCoreBlock.LIT) != active)
                level.setBlock(pos, state.setValue(HeatingCoreBlock.LIT, active), 3);
        }
    }

    private void plasmaArcFailure(ServerLevel level, List<BlockPos> cores) {
        Vec3 center = formationCenter == null ? Vec3.atCenterOf(worldPosition) : formationCenter;
        setHeatingCoresActive(cores, false);
        cores.stream().filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.HEATING_CORE.get()))
            .forEach(pos -> level.destroyBlock(pos, false));
        argonTank.drain(argonTank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 160, 3.0, 2.0, 3.0, 0.35);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 8, 1.0, 1.0, 1.0, 0);
        level.playSound(null, BlockPos.containing(center), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 5.0F, 1.4F);
        level.explode(null, center.x, center.y, center.z, 4.5F, Level.ExplosionInteraction.NONE);
        shutDown();
        status = "Plasma Arc Failure";
        sync();
    }

    public void onControllerRemoved() {
        if (operating && level instanceof ServerLevel serverLevel) {
            plasmaArcFailure(serverLevel, List.copyOf(heatingCores));
            return;
        }
        shutDown();
    }

    private void shutDown() {
        setHeatingCoresActive(heatingCores, false);
        if (level != null) {
            for (BlockPos pos : fluidInputs) if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input)
                input.unbindController(worldPosition);
            for (BlockPos pos : energyOutputs) if (level.getBlockEntity(pos) instanceof MachineEnergyOutputBlockEntity output)
                output.unbindController(worldPosition);
        }
        fluidInputs.clear();
        energyOutputs.clear();
        heatingCores.clear();
        formed = false;
        operating = false;
        outputPerTick = 0;
        formationCenter = null;
    }

    @Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.plasma_generator_controller"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PlasmaGeneratorMenu(id, inventory, this);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("argon") instanceof CompoundTag fluid) argonTank.readFromNBT(registries, fluid);
		if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
        formed = tag.getBoolean("formed");
        operating = tag.getBoolean("operating");
        outputPerTick = tag.getInt("outputPerTick");
        status = tag.contains("status", Tag.TAG_STRING) ? tag.getString("status") : "Incomplete Structure";
        formationCenter = tag.contains("formationX", Tag.TAG_DOUBLE)
            ? new Vec3(tag.getDouble("formationX"), tag.getDouble("formationY"), tag.getDouble("formationZ")) : null;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("argon", argonTank.writeToNBT(registries, new CompoundTag()));
		tag.put("energy", energy.serializeNBT(registries));
        tag.putBoolean("formed", formed);
        tag.putBoolean("operating", operating);
        tag.putInt("outputPerTick", outputPerTick);
        tag.putString("status", status);
        if (formationCenter != null) {
            tag.putDouble("formationX", formationCenter.x);
            tag.putDouble("formationY", formationCenter.y);
            tag.putDouble("formationZ", formationCenter.z);
        }
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
