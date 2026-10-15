package net.crystalnexus.block.entity;

import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.item.MeteorDataDiskItem;
import net.crystalnexus.multiblock.HeavenshatterMaterials;
import net.crystalnexus.multiblock.HeavenshatterMeteor;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.recipe.GravitationalArrayCostSchedule;
import net.crystalnexus.world.inventory.HeavenshatterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HeavenshatterBeamControllerBlockEntity extends BlockEntity implements MenuProvider, MultiblockPortTarget {
    public static final int CAPACITY = 2_000_000, LOCK_TICKS = 60, ESCAPE_TICKS = 100;
    private static final ResourceLocation TEMPLATE = ResourceLocation.parse("crystalnexus:heavenshatter_beam");
    public enum Stage { LOCATING, LOCKED, RETRIEVING, ARRIVED, ESCAPING }
    public enum Status { INCOMPLETE, TUNING, LOCKED, PULLING, ESCAPING, BLOCKED, UNLOADED, HEIGHT_LIMIT, AWAITING_CLEAR, INVALID_MATERIAL, NO_POWER, NO_SIGNALS, PAUSED }

    private final EnergyStorage energy = new EnergyStorage(CAPACITY,
        CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.maxReceive(), Integer.MAX_VALUE) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int accepted = super.receiveEnergy(amount, simulate);
            if (!simulate && accepted > 0) setChanged();
            return accepted;
        }
    };
    private final List<BlockPos> inputs = new ArrayList<>();
    private final ItemStackHandler meteorFilter = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return MeteorDataDiskItem.read(stack).isPresent(); }
        @Override protected void onContentsChanged(int slot) {
            if (level instanceof ServerLevel server && (stage == Stage.LOCATING || stage == Stage.LOCKED)) resetLocator(server);
            else setChanged();
        }
    };
    public ItemStackHandler meteorFilter() { return meteorFilter; }
    private List<BlockPos> emitters = List.of();
    private List<BlockState> palette = List.of();
    private List<HeavenshatterMeteor.Cell> cells = List.of();
    private Stage stage = Stage.LOCATING;
    private Status status = Status.INCOMPLETE;
    private boolean formed;
    private boolean structureUnloaded;
    private int frequency = 50, bearing = 180;
    private int frequencySignal, bearingSignal, lockProgress, diameter, progress, escapeTicks;
    private int duration = HeavenshatterMeteor.DURATION;
    private long seed;
    private BlockPos arrival = BlockPos.ZERO;
    private int validationDelay;
    private Status clearance = Status.UNLOADED;
    private long clientUpdateTick;
    private final net.crystalnexus.celestial.CelestialScanner scanner = new net.crystalnexus.celestial.CelestialScanner();
    private long lastScanTick = Long.MIN_VALUE;
    private boolean arrivedMeteorIndexed;
    public net.crystalnexus.celestial.Comet lockedComet() { return scanner.lockedComet(); }
    public boolean trackingIdentified() { return scanner.identified(); }
    public boolean trackingStable() { return scanner.alignmentTicks >= 60; }
    public CompoundTag trackingState() {
        CompoundTag tag = scanner.publicState();
        tag.putInt("stage", stage.ordinal()); tag.putString("status", status.name().toLowerCase(java.util.Locale.ROOT));
        tag.putInt("energy", energy.getEnergyStored()); tag.putBoolean("formed", formed);
        return tag;
    }

    public HeavenshatterBeamControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.HEAVENSHATTER_BEAM_CONTROLLER.get(), pos, state);
    }

    public Stage stage() { return stage; }
    public Status status() { return status; }
    public boolean isFormed() { return formed; }
    public int frequency() { return frequency; }
    public int bearing() { return bearing; }
    public int frequencySignal() { return frequencySignal; }
    public int bearingSignal() { return bearingSignal; }
    public int lockProgress() { return lockProgress; }
    public int diameter() { return diameter; }
    public int progress() { return progress; }
    public int duration() { return duration; }
    public int escapeTicks() { return escapeTicks; }
    public long cost() { return diameter == 0 ? 0 : HeavenshatterMeteor.energyCost(diameter); }
    public BlockPos arrival() { return arrival; }
    public List<BlockPos> emitters() { return emitters; }
    public List<BlockState> palette() { return palette; }
    public List<HeavenshatterMeteor.Cell> cells() { return cells; }
    public Component statusMessage() { return Component.translatable("gui.crystalnexus.heavenshatter.status." + status.name().toLowerCase(java.util.Locale.ROOT)); }

    public double visualProgress(float partialTick) {
        double elapsed = level == null ? 0 : level.getGameTime() - clientUpdateTick + partialTick;
        return Math.min(duration, progress + (status == Status.PULLING ? Math.min(5, Math.max(0, elapsed)) : 0));
    }

    public double visualEscape(float partialTick) {
        double elapsed = level == null ? 0 : level.getGameTime() - clientUpdateTick + partialTick;
        return Math.min(1, (escapeTicks + Math.min(5, Math.max(0, elapsed))) / ESCAPE_TICKS);
    }

    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel server)) return false;
        BlockPos center = worldPosition.relative(getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(), 11);
        structureUnloaded = false;
        for (int x = -11; x <= 11; x++) for (int z = -11; z <= 11; z++) {
            BlockPos pos = center.offset(x, 0, z);
            if (server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) structureUnloaded = true;
        }
        if (structureUnloaded) {
            onControllerRemoved();
            clearance = Status.UNLOADED;
            validationDelay = 20;
            return false;
        }
        var match = StructureNbtValidator.validate(server, TEMPLATE, worldPosition,
            getBlockState().getValue(HorizontalDirectionalBlock.FACING), getBlockState().getBlock(), HorizontalDirectionalBlock.FACING,
            Map.of(CrystalnexusModBlocks.CARBON_BLOCK.get(), Set.of(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())), true, false);
        List<BlockPos> next = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
        for (BlockPos old : inputs) {
            if (!next.contains(old) && inputAt(old) instanceof MachineEnergyInputBlockEntity port) port.unbindController(worldPosition);
        }
        inputs.clear();
        for (BlockPos pos : next) {
            if (inputAt(pos) instanceof MachineEnergyInputBlockEntity port) {
                port.bindController(worldPosition);
                inputs.add(pos);
            }
        }
        formed = match.isPresent() && !inputs.isEmpty() && inputs.size() == next.size();
        if (formed) {
            var structure = match.orElseThrow();
            var points = structure.positionsFor(CrystalnexusModBlocks.GRAVITY_CONTROL_POINT.get());
            int highest = points.stream().mapToInt(BlockPos::getY).max().orElse(worldPosition.getY());
            // The middle point on each upper stack supplies one beam per side.
            emitters = points.stream().filter(pos -> pos.getY() == highest - 1).toList();
            if (stage == Stage.LOCATING) {
                int top = structure.templatePositions().values().stream().flatMap(List::stream).mapToInt(BlockPos::getY).max().orElse(worldPosition.getY());
                arrival = new BlockPos((int) Math.floor(structure.center().x - 7), top + 4, (int) Math.floor(structure.center().z - 7));
            }
        }
        clearance = formed ? checkClearance() : Status.INCOMPLETE;
        validationDelay = 20;
        setChanged();
        return formed;
    }

    private Status checkClearance() {
        if (!(level instanceof ServerLevel server)) return Status.UNLOADED;
        if (arrival.getY() < level.getMinBuildHeight() || arrival.getY() + HeavenshatterMeteor.MAX_DIAMETER > level.getMaxBuildHeight()) return Status.HEIGHT_LIMIT;
        int minOffset = (15 - HeavenshatterMeteor.MAX_DIAMETER) / 2;
        for (int x = minOffset; x < minOffset + HeavenshatterMeteor.MAX_DIAMETER; x++) for (int z = minOffset; z < minOffset + HeavenshatterMeteor.MAX_DIAMETER; z++) {
            BlockPos pos = arrival.offset(x, 0, z);
            var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) return Status.UNLOADED;
            if (chunk.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() & 15, pos.getZ() & 15) + 1 > pos.getY()) return Status.BLOCKED;
        }
        return null;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        if (stage == Stage.ARRIVED && !arrivedMeteorIndexed && diameter >= HeavenshatterMeteor.MIN_DIAMETER && palette.size() >= 2) {
            if (palette.stream().allMatch(state -> HeavenshatterMaterials.eligible(state, server, meteorBase())))
                net.crystalnexus.data.SummonedMeteorSavedData.get(server).register(meteorBase(), diameter, seed, palette);
            arrivedMeteorIndexed = true;
        }
        Status previous = status;
        if (validationDelay-- <= 0) validateStructureNow();
        if (stage == Stage.ESCAPING) {
            status = Status.ESCAPING;
            escapeTicks++;
            setChanged();
            if (escapeTicks >= ESCAPE_TICKS) resetLocator(server);
        } else if (!formed) status = structureUnloaded ? Status.UNLOADED : Status.INCOMPLETE;
        else if (stage == Stage.ARRIVED) {
            status = Status.AWAITING_CLEAR;
            BlockPos base = meteorBase();
            if (cells.stream().allMatch(cell -> level.getBlockState(base.offset(cell.x(), cell.y(), cell.z())).isAir())) resetLocator(server);
        } else if (stage == Stage.LOCATING) {
            if (!meteorFilter.getStackInSlot(0).isEmpty()) {
                var record = MeteorDataDiskItem.read(meteorFilter.getStackInSlot(0));
                if (record.isEmpty() || record.orElseThrow().palette().stream().anyMatch(state -> !HeavenshatterMaterials.eligible(state, server, worldPosition)))
                    status = Status.INVALID_MATERIAL;
                else if (energy.getEnergyStored() < 1000) status = Status.NO_POWER;
                else {
                    energy.extractEnergy(1000, false);
                    scanner.lockRecorded(record.orElseThrow());
                    locateMeteor(server);
                }
            } else if (lastScanTick != server.getGameTime()) {
                lastScanTick = server.getGameTime();
                if (scanner.candidates.isEmpty() && (status != Status.NO_SIGNALS || server.getGameTime() % 20 == 0)) scanner.generate(server, worldPosition);
                boolean viewer = false;
                for (var player : server.players()) if (player.containerMenu instanceof HeavenshatterMenu menu
                    && menu.controller() == this && menu.stillValid(player)) { viewer = true; break; }
                boolean powered = viewer && !scanner.candidates.isEmpty() && energy.getEnergyStored() >= 1000;
                if (powered) energy.extractEnergy(1000, false);
                if (viewer) scanner.tick(powered);
                frequency = scanner.frequency; bearing = scanner.azimuth;
                frequencySignal = scanner.frequencySignal; bearingSignal = scanner.strength; lockProgress = scanner.alignmentTicks;
                status = scanner.candidates.isEmpty() ? Status.NO_SIGNALS : !viewer ? Status.PAUSED : powered ? Status.TUNING : Status.NO_POWER;
                setChanged();
            }
        } else if (clearance != null) status = clearance;
        else if (stage == Stage.LOCKED) status = Status.LOCKED;
        else retrieve();
        if (!formed && stage == Stage.LOCATING) scanner.tick(false);
        if (previous != status || (stage != Stage.LOCATING && server.getGameTime() % 5 == 0)) sync();
    }

    private void locateMeteor(ServerLevel server) {
        var comet = scanner.lockedComet();
        if (comet == null) return;
        diameter = comet.diameter(); seed = comet.seed(); palette = comet.palette(); duration = HeavenshatterMeteor.duration(comet.distance());
        cells = HeavenshatterMeteor.cells(diameter, seed, palette.size());
        frequency = scanner.frequency; bearing = scanner.azimuth;
        frequencySignal = scanner.frequencySignal; bearingSignal = scanner.strength; lockProgress = scanner.alignmentTicks;
        stage = Stage.LOCKED; status = Status.LOCKED; sync();
    }

    private void retrieve() {
        if (palette.size() < 2 || palette.stream().anyMatch(state -> !HeavenshatterMaterials.eligible(state, level, worldPosition))) {
            status = Status.INVALID_MATERIAL;
            return;
        }
        if (progress < duration) {
            int needed = Math.toIntExact(GravitationalArrayCostSchedule.next(cost(), progress, duration));
            if (energy.getEnergyStored() < needed) {
                stage = Stage.ESCAPING;
                status = Status.ESCAPING;
                escapeTicks = 0;
                sync();
                return;
            }
            energy.extractEnergy(needed, false);
            progress++;
            status = Status.PULLING;
            setChanged();
        }
        if (progress == duration) materialize();
    }

    private void materialize() {
        clearance = checkClearance();
        if (clearance != null) { status = clearance; return; }
        BlockPos base = meteorBase();
        for (var cell : cells) {
            if (!level.getBlockState(base.offset(cell.x(), cell.y(), cell.z())).isAir()) {
                status = Status.BLOCKED;
                return;
            }
        }
        for (var cell : cells) level.setBlock(base.offset(cell.x(), cell.y(), cell.z()), palette.get(cell.material()), 3);
        net.crystalnexus.data.SummonedMeteorSavedData.get((ServerLevel) level).register(base, diameter, seed, palette);
        arrivedMeteorIndexed = true;
        stage = Stage.ARRIVED;
        status = Status.AWAITING_CLEAR;
        clearance = Status.BLOCKED;
        sync();
    }

    public BlockPos meteorBase() { return arrival.offset((15 - diameter) / 2, 0, (15 - diameter) / 2); }

    private void resetLocator(ServerLevel server) {
        arrivedMeteorIndexed = false;
        stage = Stage.LOCATING;
        status = Status.TUNING;
        scanner.reset();
        scanner.generate(server, worldPosition);
        lockProgress = progress = diameter = frequencySignal = bearingSignal = escapeTicks = 0;
        duration = HeavenshatterMeteor.DURATION;
        seed = 0;
        palette = List.of();
        cells = List.of();
        sync();
    }

    public void controls(Player player, int frequency, int bearing, boolean beginPull) {
        controls(player, scanner.selected, frequency, bearing, scanner.elevation, beginPull ? 2 : 0);
    }

    public void controls(Player player, int candidate, int frequency, int bearing, int elevation, int action) {
        if (!(level instanceof ServerLevel server) || !(player.containerMenu instanceof HeavenshatterMenu menu)
            || menu.controller() != this || !menu.stillValid(player) || frequency < 0 || frequency > 100
            || bearing < 0 || bearing > 359 || elevation < 0 || elevation > 90 || action < 0 || action > 2) return;
        if (action == 2) {
            if (stage != Stage.LOCKED) return;
            validateStructureNow();
            if (formed && clearance == null && stage == Stage.LOCKED) {
                stage = Stage.RETRIEVING; status = Status.PULLING; sync();
            }
            return;
        }
        if (stage != Stage.LOCATING || !formed || energy.getEnergyStored() < 1000 || candidate < 0 || candidate >= scanner.candidates.size()) return;
        if (action == 1) {
            if (!scanner.identified() || scanner.alignmentTicks < 60) return;
            validateStructureNow();
            if (formed && energy.getEnergyStored() >= 1000 && candidate == scanner.selected
                && frequency == scanner.frequency && bearing == scanner.azimuth && elevation == scanner.elevation && scanner.lock()) locateMeteor(server);
        } else if (menu.acceptControls(server.getGameTime())) {
            scanner.controls(candidate, bearing, elevation, frequency);
            this.frequency = scanner.frequency; this.bearing = scanner.azimuth;
            setChanged();
        }
    }

    @Override public EnergyStorage multiblockEnergyInput() { return energy; }
    @Override public boolean acceptsMultiblockPort(BlockPos pos) { return formed && inputs.contains(pos); }

    public void onControllerRemoved() {
        if (level != null) for (BlockPos pos : inputs) {
            if (inputAt(pos) instanceof MachineEnergyInputBlockEntity port) port.unbindController(worldPosition);
        }
        inputs.clear();
        formed = false;
    }

    private BlockEntity inputAt(BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return null;
        var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.heavenshatter_beam_controller"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new HeavenshatterMenu(id, inventory, this); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writePublic(tag, registries);
        return tag;
    }

    private void writePublic(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("meteorFilter", meteorFilter.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putBoolean("formed", formed);
        tag.putInt("stage", stage.ordinal());
        tag.putInt("status", status.ordinal());
        tag.putInt("frequency", frequency);
        tag.putInt("bearing", bearing);
        tag.putInt("frequencySignal", frequencySignal);
        tag.putInt("bearingSignal", bearingSignal);
        tag.putInt("lock", lockProgress);
        tag.putInt("diameter", diameter);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putInt("escapeTicks", escapeTicks);
        tag.putLong("seed", seed);
        tag.putLong("arrival", arrival.asLong());
        tag.putLongArray("emitters", emitters.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag materials = new ListTag();
        for (BlockState state : palette) materials.add(NbtUtils.writeBlockState(state));
        tag.put("palette", materials);
    }

    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writePublic(tag, registries);
        // Puzzle answers stay in the world save, never in block entity update packets.
        tag.put("celestial", scanner.save());
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        meteorFilter.deserializeNBT(registries, tag.getCompound("meteorFilter"));
        arrivedMeteorIndexed = false;
        if (tag.contains("celestial")) scanner.load(tag.getCompound("celestial"), registries);
        int oldDiameter = diameter;
        long oldSeed = seed;
        energy.deserializeNBT(registries, IntTag.valueOf(Math.clamp(tag.getInt("energy"), 0, CAPACITY)));
        formed = tag.getBoolean("formed");
        stage = Stage.values()[Math.clamp(tag.getInt("stage"), 0, Stage.values().length - 1)];
        status = Status.values()[Math.clamp(tag.getInt("status"), 0, Status.values().length - 1)];
        frequency = Math.clamp(tag.getInt("frequency"), 0, 100);
        bearing = Math.clamp(tag.getInt("bearing"), 0, 359);
        frequencySignal = tag.getInt("frequencySignal");
        bearingSignal = tag.getInt("bearingSignal");
        lockProgress = Math.clamp(tag.getInt("lock"), 0, LOCK_TICKS);
        diameter = tag.getInt("diameter");
        if (diameter != 0) diameter = Math.clamp(diameter, 7, HeavenshatterMeteor.MAX_DIAMETER);
        duration = tag.contains("duration") ? Math.clamp(tag.getInt("duration"), HeavenshatterMeteor.DURATION, HeavenshatterMeteor.MAX_DURATION)
            : scanner.lockedComet() == null ? HeavenshatterMeteor.DURATION : HeavenshatterMeteor.duration(scanner.lockedComet().distance());
        progress = Math.clamp(tag.getInt("progress"), 0, duration);
        escapeTicks = Math.clamp(tag.getInt("escapeTicks"), 0, ESCAPE_TICKS);
        seed = tag.getLong("seed");
        arrival = BlockPos.of(tag.getLong("arrival"));
        emitters = java.util.Arrays.stream(tag.getLongArray("emitters")).mapToObj(BlockPos::of).toList();
        List<BlockState> materials = new ArrayList<>();
        ListTag saved = tag.getList("palette", Tag.TAG_COMPOUND);
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < Math.min(5, saved.size()); i++) materials.add(NbtUtils.readBlockState(blocks, saved.getCompound(i)));
        List<BlockState> nextPalette = List.copyOf(materials);
        if (diameter > 0 && nextPalette.size() >= 2) {
            if (oldDiameter != diameter || oldSeed != seed || !palette.equals(nextPalette) || cells.isEmpty())
                cells = HeavenshatterMeteor.cells(diameter, seed, nextPalette.size());
        }
        else cells = List.of();
        palette = nextPalette;
        validationDelay = 0;
        if (level != null && level.isClientSide) clientUpdateTick = level.getGameTime();
    }
}
