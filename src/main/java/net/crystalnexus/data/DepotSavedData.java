package net.crystalnexus.data;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.config.CrystalnexusConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

public class DepotSavedData extends SavedData {
    public static final String ID = "crystalnexus_depot";
    public static final int MAX_UPGRADE_LEVEL = 62;
    private static final ResourceLocation UPLINK_ID = ResourceLocation.fromNamespaceAndPath("crystalnexus", "depot_uplink");

    // ===== Capacity / Upgrades =====
    private int upgradeLevel = 0;
    private ResourceLocation controllerDimension;
    private BlockPos controllerPos;
    private final Map<ResourceLocation, ResourceLocation> preferredRecipes = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, ResourceLocation> preferredMachines = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, ProcessingPattern> processingPatterns = new ConcurrentHashMap<>();
    private boolean machineLoadBalancing;
    private CraftingJob craftingJob;
    private ProcessingTask processingTask;
    private int nextCraftingJobId = 1;

    // ===== Stored items =====
    private final Object2LongMap<ResourceLocation> counts = new Object2LongOpenHashMap<>();

    public record Entry(ResourceLocation itemId, long count) {}
    public record SlotEntry(ResourceLocation itemId, long count) {}
    public record ProcessingPattern(ResourceLocation outputId, long outputAmount,
            Map<ResourceLocation, Long> inputs, Map<ResourceLocation, Long> outputs,
            List<ResourceLocation> machineTypes) {
        public ProcessingPattern(ResourceLocation outputId, long outputAmount, Map<ResourceLocation, Long> inputs) {
            this(outputId, outputAmount, inputs, Map.of(outputId, outputAmount), List.of());
        }

        public ProcessingPattern {
            inputs = Map.copyOf(inputs);
            outputs = Map.copyOf(outputs);
            machineTypes = List.copyOf(machineTypes);
        }
    }
    public record ProcessingTask(ResourceLocation dimension, BlockPos machinePos,
            List<SlotEntry> remainingInputs, Map<ResourceLocation, Long> remainingOutputs) {
        public ProcessingTask {
            machinePos = machinePos.immutable();
            remainingInputs = List.copyOf(remainingInputs);
            remainingOutputs = Map.copyOf(remainingOutputs);
        }
    }
    public record CraftingStep(ResourceLocation outputId, long outputAmount, long work,
            List<SlotEntry> inputs, Map<ResourceLocation, Long> outputs, boolean processing,
            List<ResourceLocation> machineTypes) {
        public CraftingStep(ResourceLocation outputId, long outputAmount, long work,
                List<SlotEntry> inputs, Map<ResourceLocation, Long> outputs) {
            this(outputId, outputAmount, work, inputs, outputs, false, List.of());
        }

        public CraftingStep(ResourceLocation outputId, long outputAmount, long work,
                List<SlotEntry> inputs, Map<ResourceLocation, Long> outputs, boolean processing) {
            this(outputId, outputAmount, work, inputs, outputs, processing, List.of());
        }

        public CraftingStep {
            inputs = List.copyOf(inputs);
            outputs = Map.copyOf(outputs);
            machineTypes = List.copyOf(machineTypes);
        }
    }

    public record CraftingJob(int id, ResourceLocation targetId, int amount, long totalWork, long remainingWork,
            long storageReservation, Map<ResourceLocation, Long> reservedInputs, Map<ResourceLocation, Long> outputs,
            Map<ResourceLocation, Long> workingItems, List<CraftingStep> steps) {
        public CraftingJob {
            reservedInputs = Map.copyOf(reservedInputs);
            outputs = Map.copyOf(outputs);
            workingItems = Map.copyOf(workingItems);
            steps = List.copyOf(steps);
        }

        public long reservedSpace() {
            return storageReservation;
        }

        public int currentStepIndex() {
            if (steps.isEmpty()) return 0;
            long completed = totalWork - remainingWork;
            long end = 0;
            for (int i = 0; i < steps.size(); i++) {
                end = saturatedAdd(end, steps.get(i).work());
                if (completed < end) return i;
            }
            return steps.size() - 1;
        }

        public @Nullable CraftingStep currentStep() {
            return steps.isEmpty() ? null : steps.get(currentStepIndex());
        }

        public int currentStepPercent() {
            if (steps.isEmpty()) return (int) Math.min(100, 100.0 * (totalWork - remainingWork) / totalWork);
            int index = currentStepIndex();
            long before = 0;
            for (int i = 0; i < index; i++) before = saturatedAdd(before, steps.get(i).work());
            long done = Math.max(0, totalWork - remainingWork - before);
            return (int) Math.min(100, 100.0 * done / Math.max(1, steps.get(index).work()));
        }
    }
    private static final Map<ResourceLocation, String> SEARCH_CACHE = new ConcurrentHashMap<>();

    public static DepotSavedData get(ServerPlayer player) {
        return get(player.serverLevel(), player.getUUID());
    }

    public static DepotSavedData get(ServerLevel level, UUID playerId) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DepotSavedData::new, DepotSavedData::load),
                ID + "_" + playerId
        );
    }

    public static DepotSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        DepotSavedData data = new DepotSavedData();

        data.upgradeLevel = Math.max(0, Math.min(MAX_UPGRADE_LEVEL, tag.getInt("upgradeLevel")));
        data.controllerDimension = ResourceLocation.tryParse(tag.getString("ControllerDimension"));
        if (data.controllerDimension != null && tag.contains("ControllerPos")) {
            data.controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
        }

        CompoundTag items = tag.getCompound("items");
        for (String key : items.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) data.counts.put(id, items.getLong(key));
        }

        CompoundTag preferences = tag.getCompound("preferredRecipes");
        for (String key : preferences.getAllKeys()) {
            ResourceLocation itemId = ResourceLocation.tryParse(key);
            ResourceLocation recipeId = ResourceLocation.tryParse(preferences.getString(key));
            if (itemId != null && recipeId != null) data.preferredRecipes.put(itemId, recipeId);
        }
        CompoundTag machines = tag.getCompound("preferredMachines");
        for (String key : machines.getAllKeys()) {
            ResourceLocation itemId = ResourceLocation.tryParse(key);
            ResourceLocation machineId = ResourceLocation.tryParse(machines.getString(key));
            if (itemId != null && machineId != null) data.preferredMachines.put(itemId, machineId);
        }
        data.machineLoadBalancing = tag.getBoolean("machineLoadBalancing");

        ListTag patterns = tag.getList("processingPatterns", Tag.TAG_COMPOUND);
        for (int i = 0; i < patterns.size(); i++) {
            CompoundTag pattern = patterns.getCompound(i);
            ResourceLocation outputId = ResourceLocation.tryParse(pattern.getString("output"));
            long amount = pattern.getLong("amount");
            Map<ResourceLocation, Long> inputs = loadCounts(pattern.getCompound("inputs"));
            if (outputId != null && amount > 0 && !inputs.isEmpty()) {
                Map<ResourceLocation, Long> outputs = loadCounts(pattern.getCompound("outputs"));
                data.processingPatterns.put(outputId, new ProcessingPattern(outputId, amount, inputs,
                    outputs.isEmpty() ? Map.of(outputId, amount) : outputs,
                    loadIds(pattern.getList("machineTypes", Tag.TAG_STRING))));
            }
        }

        data.nextCraftingJobId = Math.max(1, tag.getInt("nextCraftingJobId"));
        if (tag.contains("craftingJob")) {
            CompoundTag job = tag.getCompound("craftingJob");
            ResourceLocation targetId = ResourceLocation.tryParse(job.getString("target"));
            int id = job.getInt("id");
            int amount = job.getInt("amount");
            long totalWork = job.getLong("totalWork");
            long remainingWork = job.getLong("remainingWork");
            if (targetId != null && id > 0 && amount > 0 && totalWork > 0 && remainingWork > 0) {
                Map<ResourceLocation, Long> reservedInputs = loadCounts(job.getCompound("reservedInputs"));
                Map<ResourceLocation, Long> outputs = loadCounts(job.getCompound("outputs"));
                List<CraftingStep> steps = loadSteps(job.getList("steps", Tag.TAG_COMPOUND));
                Map<ResourceLocation, Long> workingItems = loadCounts(job.getCompound("workingItems"));
                long reservation = job.contains("storageReservation") ? job.getLong("storageReservation")
                        : Math.max(total(reservedInputs), total(outputs));
                data.craftingJob = new CraftingJob(id, targetId, amount, totalWork, remainingWork,
                        Math.max(0, reservation), reservedInputs, outputs, workingItems, steps);
            }
        }
        if (data.craftingJob != null && tag.contains("processingTask")) {
            CompoundTag task = tag.getCompound("processingTask");
            ResourceLocation dimension = ResourceLocation.tryParse(task.getString("dimension"));
            if (dimension != null && task.contains("machinePos")) {
                data.processingTask = new ProcessingTask(dimension, BlockPos.of(task.getLong("machinePos")),
                        loadSlotEntries(task.getCompound("remainingInputs")),
                        loadCounts(task.getCompound("remainingOutputs")));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("upgradeLevel", upgradeLevel);
        if (controllerDimension != null && controllerPos != null) {
            tag.putString("ControllerDimension", controllerDimension.toString());
            tag.putLong("ControllerPos", controllerPos.asLong());
        }

        CompoundTag items = new CompoundTag();
        counts.object2LongEntrySet().forEach(e -> items.putLong(e.getKey().toString(), e.getLongValue()));
        tag.put("items", items);

        CompoundTag preferences = new CompoundTag();
        preferredRecipes.forEach((itemId, recipeId) -> preferences.putString(itemId.toString(), recipeId.toString()));
        tag.put("preferredRecipes", preferences);
        CompoundTag machines = new CompoundTag();
        preferredMachines.forEach((itemId, machineId) -> machines.putString(itemId.toString(), machineId.toString()));
        tag.put("preferredMachines", machines);
        tag.putBoolean("machineLoadBalancing", machineLoadBalancing);

        ListTag patterns = new ListTag();
        processingPatterns.values().stream().sorted(Comparator.comparing(pattern -> pattern.outputId().toString()))
                .forEach(pattern -> {
                    CompoundTag saved = new CompoundTag();
                    saved.putString("output", pattern.outputId().toString());
                    saved.putLong("amount", pattern.outputAmount());
                    saved.put("inputs", saveCounts(pattern.inputs()));
                    saved.put("outputs", saveCounts(pattern.outputs()));
                    saved.put("machineTypes", saveIds(pattern.machineTypes()));
                    patterns.add(saved);
                });
        tag.put("processingPatterns", patterns);

        tag.putInt("nextCraftingJobId", nextCraftingJobId);
        if (craftingJob != null) {
            CompoundTag job = new CompoundTag();
            job.putInt("id", craftingJob.id());
            job.putString("target", craftingJob.targetId().toString());
            job.putInt("amount", craftingJob.amount());
            job.putLong("totalWork", craftingJob.totalWork());
            job.putLong("remainingWork", craftingJob.remainingWork());
            job.putLong("storageReservation", craftingJob.storageReservation());
            job.put("reservedInputs", saveCounts(craftingJob.reservedInputs()));
            job.put("outputs", saveCounts(craftingJob.outputs()));
            job.put("workingItems", saveCounts(craftingJob.workingItems()));
            job.put("steps", saveSteps(craftingJob.steps()));
            tag.put("craftingJob", job);
        }
        if (processingTask != null) {
            CompoundTag task = new CompoundTag();
            task.putString("dimension", processingTask.dimension().toString());
            task.putLong("machinePos", processingTask.machinePos().asLong());
            task.put("remainingInputs", saveSlotEntries(processingTask.remainingInputs()));
            task.put("remainingOutputs", saveCounts(processingTask.remainingOutputs()));
            tag.put("processingTask", task);
        }

        return tag;
    }

    // ===== Capacity helpers =====

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public void setController(ServerLevel level, BlockPos pos) {
        controllerDimension = level.dimension().location();
        controllerPos = pos.immutable();
        setDirty();
    }

    public void setControllerIfAbsent(ServerLevel level, BlockPos pos) {
        if (controllerDimension == null || controllerPos == null) setController(level, pos);
    }

    public void clearController(ServerLevel level, BlockPos pos) {
        if (isController(level, pos)) {
            controllerDimension = null;
            controllerPos = null;
            setDirty();
        }
    }

    public boolean isController(ServerLevel level, BlockPos pos) {
        return controllerDimension != null && controllerPos != null
                && controllerDimension.equals(level.dimension().location()) && controllerPos.equals(pos);
    }

    public static boolean hasPoweredController(ServerPlayer player) {
        return hasPoweredController(player.serverLevel(), player.getUUID());
    }

    public static boolean hasPoweredController(ServerLevel level, UUID playerId) {
        DepotControllerBlockEntity controller = getController(level, playerId);
        return controller != null && controller.isPowered();
    }

    public static @Nullable DepotControllerBlockEntity getController(ServerLevel level, UUID playerId) {
        DepotSavedData data = get(level, playerId);
        if (data.controllerDimension == null || data.controllerPos == null) return null;
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, data.controllerDimension);
        ServerLevel controllerLevel = level.getServer().getLevel(dimension);
        if (controllerLevel == null || !controllerLevel.hasChunkAt(data.controllerPos)) return null;
        if (controllerLevel.getBlockEntity(data.controllerPos) instanceof DepotControllerBlockEntity controller
                && playerId.equals(controller.getOwner())) return controller;
        return null;
    }

    public static boolean requirePoweredController(ServerPlayer player) {
        if (hasPoweredController(player)) return true;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Your Depot Controller is missing, unloaded, or out of power.")
                .withStyle(net.minecraft.ChatFormatting.RED), true);
        return false;
    }

    /** Doubles capacity per upgrade (BASE * 2^upgradeLevel). */
    public long getCapacity() {
        long baseCapacity = CrystalnexusConfig.MACHINES.depotBaseCapacity();
        if (upgradeLevel >= 63 || baseCapacity > (Long.MAX_VALUE >> upgradeLevel)) return Long.MAX_VALUE;
        return baseCapacity << upgradeLevel;
    }

    /** Total items stored (sum of all counts). */
    public long getUsed() {
        long sum = 0L;
        for (var e : counts.object2LongEntrySet()) {
            long v = e.getLongValue();
            if (v > 0) {
                if (v > Long.MAX_VALUE - sum) return Long.MAX_VALUE;
                sum += v;
            }
        }
        if (craftingJob != null) {
            long reserved = craftingJob.reservedSpace();
            if (reserved > Long.MAX_VALUE - sum) return Long.MAX_VALUE;
            sum += reserved;
        }
        return sum;
    }

    public long getFree() {
        long free = getCapacity() - getUsed();
        return Math.max(0L, free);
    }

    public boolean canInsert(long amount) {
        if (amount <= 0) return true;
        return amount <= getFree();
    }

    /** @return whether the depot was upgraded. */
    public boolean addUpgrade() {
        if (upgradeLevel >= MAX_UPGRADE_LEVEL || getCapacity() == Long.MAX_VALUE) return false;
        upgradeLevel++;
        setDirty();
        return true;
    }

    public @Nullable ResourceLocation getPreferredRecipe(ResourceLocation itemId) {
        return preferredRecipes.get(itemId);
    }

    public void setPreferredRecipe(ResourceLocation itemId, ResourceLocation recipeId) {
        preferredRecipes.put(itemId, recipeId);
        setDirty();
    }

    public boolean clearPreferredRecipe(ResourceLocation itemId) {
        if (preferredRecipes.remove(itemId) == null) return false;
        setDirty();
        return true;
    }

    public @Nullable ResourceLocation getPreferredMachine(ResourceLocation itemId) {
        return preferredMachines.get(itemId);
    }

    public void setPreferredMachine(ResourceLocation itemId, ResourceLocation machineId) {
        preferredMachines.put(itemId, machineId);
        setDirty();
    }

    public boolean clearPreferredMachine(ResourceLocation itemId) {
        if (preferredMachines.remove(itemId) == null) return false;
        setDirty();
        return true;
    }

    public boolean isMachineLoadBalancing() {
        return machineLoadBalancing;
    }

    public void setMachineLoadBalancing(boolean enabled) {
        if (machineLoadBalancing == enabled) return;
        machineLoadBalancing = enabled;
        setDirty();
    }

    public List<ProcessingPattern> getProcessingPatterns() {
        return processingPatterns.values().stream()
                .sorted(Comparator.comparing(pattern -> pattern.outputId().toString())).toList();
    }

    public @Nullable ProcessingPattern getProcessingPattern(ResourceLocation outputId) {
        return processingPatterns.get(outputId);
    }

    public void setProcessingPattern(ResourceLocation outputId, long outputAmount,
            Map<ResourceLocation, Long> inputs) {
        processingPatterns.put(outputId, new ProcessingPattern(outputId, outputAmount, inputs));
        setDirty();
    }

    public void setProcessingPattern(ResourceLocation outputId, long outputAmount,
            Map<ResourceLocation, Long> inputs, Map<ResourceLocation, Long> outputs) {
        processingPatterns.put(outputId, new ProcessingPattern(outputId, outputAmount, inputs, outputs, List.of()));
        setDirty();
    }

    public void setProcessingPattern(ResourceLocation outputId, long outputAmount,
            Map<ResourceLocation, Long> inputs, Map<ResourceLocation, Long> outputs,
            List<ResourceLocation> machineTypes) {
        processingPatterns.put(outputId, new ProcessingPattern(outputId, outputAmount, inputs, outputs, machineTypes));
        setDirty();
    }

    public boolean removeProcessingPattern(ResourceLocation outputId) {
        if (processingPatterns.remove(outputId) == null) return false;
        setDirty();
        return true;
    }

    public @Nullable ProcessingTask getProcessingTask() {
        return processingTask;
    }

    public @Nullable CraftingJob getCraftingJob() {
        return craftingJob;
    }

    public @Nullable CraftingJob startCraftingJob(ResourceLocation targetId, int amount, long totalWork,
            long storageReservation, Map<ResourceLocation, Long> reservedInputs,
            Map<ResourceLocation, Long> outputs, List<CraftingStep> steps) {
        if (craftingJob != null || targetId == null || amount <= 0 || totalWork <= 0
                || storageReservation <= 0 || outputs.isEmpty() || steps.isEmpty()) return null;
        if (reservedInputs.entrySet().stream().anyMatch(entry -> entry.getValue() <= 0
                || getCount(entry.getKey()) < entry.getValue())) return null;
        long reservedCount = total(reservedInputs);
        if (storageReservation > reservedCount && storageReservation - reservedCount > getFree()) return null;
        reservedInputs.forEach(this::remove);
        int id = nextCraftingJobId++;
        if (nextCraftingJobId <= 0) nextCraftingJobId = 1;
        craftingJob = new CraftingJob(id, targetId, amount, totalWork, totalWork, storageReservation,
                reservedInputs, outputs, reservedInputs, steps);
        processingTask = null;
        setDirty();
        return craftingJob;
    }

    public @Nullable CraftingJob advanceCraftingJob(int processors) {
        if (craftingJob == null || processors <= 0) return null;
        if (craftingJob.steps().isEmpty()) return advanceLegacyCraftingJob(processors);
        long completedBefore = craftingJob.totalWork() - craftingJob.remainingWork();
        long advance = processors;
        long stepStart = 0;
        for (CraftingStep step : craftingJob.steps()) {
            long stepEnd = saturatedAdd(stepStart, step.work());
            if (stepEnd > completedBefore && step.processing()) {
                advance = Math.min(advance, Math.max(0, stepStart - completedBefore));
                break;
            }
            stepStart = stepEnd;
        }
        if (advance <= 0) return null;
        long remaining = Math.max(0, craftingJob.remainingWork() - advance);
        long completedAfter = craftingJob.totalWork() - remaining;
        Map<ResourceLocation, Long> working = new ConcurrentHashMap<>(craftingJob.workingItems());
        long remainingTargetInputs = 0;
        for (CraftingStep step : craftingJob.steps()) {
            long stepTargetInput = 0;
            for (SlotEntry entry : step.inputs()) {
                if (entry.itemId().equals(craftingJob.targetId())) stepTargetInput = saturatedAdd(stepTargetInput, entry.count());
            }
            remainingTargetInputs = saturatedAdd(remainingTargetInputs, stepTargetInput);
        }
        long released = 0;
        long stepEnd = 0;
        for (CraftingStep step : craftingJob.steps()) {
            stepEnd = saturatedAdd(stepEnd, step.work());
            long targetInput = 0;
            for (SlotEntry entry : step.inputs()) {
                if (entry.itemId().equals(craftingJob.targetId())) targetInput = saturatedAdd(targetInput, entry.count());
            }
            if (completedBefore >= stepEnd) {
                remainingTargetInputs = Math.max(0, remainingTargetInputs - targetInput);
            } else if (completedAfter >= stepEnd) {
                remainingTargetInputs = Math.max(0, remainingTargetInputs - targetInput);
                if (!step.processing()) applyStep(working, step);
                long targetCount = working.getOrDefault(craftingJob.targetId(), 0L);
                released = saturatedAdd(released, releaseTarget(working, remainingTargetInputs, targetCount));
            }
        }
        long reservation = Math.max(0, craftingJob.storageReservation() - released);
        if (remaining > 0) {
            craftingJob = new CraftingJob(craftingJob.id(), craftingJob.targetId(), craftingJob.amount(),
                    craftingJob.totalWork(), remaining, reservation, craftingJob.reservedInputs(),
                    craftingJob.outputs(), working, craftingJob.steps());
            setDirty();
            return null;
        }
        CraftingJob completed = craftingJob;
        craftingJob = null;
        working.forEach(this::add);
        setDirty();
        return new CraftingJob(completed.id(), completed.targetId(), completed.amount(), completed.totalWork(), 0,
                reservation, completed.reservedInputs(), completed.outputs(), working, completed.steps());
    }

    public @Nullable CraftingJob cancelCraftingJob(int id) {
        if (craftingJob == null || craftingJob.id() != id) return null;
        CraftingJob cancelled = craftingJob;
        craftingJob = null;
        processingTask = null;
        (cancelled.steps().isEmpty() ? cancelled.reservedInputs() : cancelled.workingItems()).forEach(this::add);
        setDirty();
        return cancelled;
    }

    public @Nullable CraftingJob updateProcessingTask(ProcessingTask task,
            Map<ResourceLocation, Long> inserted, Map<ResourceLocation, Long> extracted) {
        if (craftingJob == null || task == null || craftingJob.currentStep() == null
                || !craftingJob.currentStep().processing()) return null;
        Map<ResourceLocation, Long> working = new ConcurrentHashMap<>(craftingJob.workingItems());
        for (Map.Entry<ResourceLocation, Long> entry : inserted.entrySet()) {
            long left = working.getOrDefault(entry.getKey(), 0L) - entry.getValue();
            if (left < 0) return null;
            if (left == 0) working.remove(entry.getKey());
            else working.put(entry.getKey(), left);
        }
        extracted.forEach((id, amount) -> working.merge(id, amount, DepotSavedData::saturatedAdd));
        processingTask = task;
        if (!task.remainingInputs().isEmpty() || !task.remainingOutputs().isEmpty()) {
            craftingJob = new CraftingJob(craftingJob.id(), craftingJob.targetId(), craftingJob.amount(),
                    craftingJob.totalWork(), craftingJob.remainingWork(), craftingJob.storageReservation(),
                    craftingJob.reservedInputs(), craftingJob.outputs(), working, craftingJob.steps());
            setDirty();
            return null;
        }

        CraftingJob active = craftingJob;
        int completedStep = active.currentStepIndex();
        long futureTargetInputs = 0;
        for (int i = completedStep + 1; i < active.steps().size(); i++) {
            for (SlotEntry entry : active.steps().get(i).inputs()) {
                if (entry.itemId().equals(active.targetId())) futureTargetInputs = saturatedAdd(futureTargetInputs, entry.count());
            }
        }
        long released = releaseTarget(working, futureTargetInputs,
                working.getOrDefault(active.targetId(), 0L));
        long reservation = Math.max(0, active.storageReservation() - released);
        long remaining = Math.max(0, active.remainingWork() - active.currentStep().work());
        processingTask = null;
        if (remaining > 0) {
            craftingJob = new CraftingJob(active.id(), active.targetId(), active.amount(), active.totalWork(),
                    remaining, reservation, active.reservedInputs(), active.outputs(), working, active.steps());
            setDirty();
            return null;
        }
        craftingJob = null;
        working.forEach(this::add);
        setDirty();
        return new CraftingJob(active.id(), active.targetId(), active.amount(), active.totalWork(), 0,
                reservation, active.reservedInputs(), active.outputs(), working, active.steps());
    }

    private long releaseTarget(Map<ResourceLocation, Long> working, long remainingTargetInputs, long targetCount) {
        long available = Math.max(0, targetCount - remainingTargetInputs);
        if (available <= 0) return 0;
        if (available == targetCount) working.remove(craftingJob.targetId());
        else working.put(craftingJob.targetId(), targetCount - available);
        add(craftingJob.targetId(), available);
        return available;
    }

    private @Nullable CraftingJob advanceLegacyCraftingJob(int processors) {
        long remaining = Math.max(0, craftingJob.remainingWork() - processors);
        if (remaining > 0) {
            craftingJob = new CraftingJob(craftingJob.id(), craftingJob.targetId(), craftingJob.amount(),
                    craftingJob.totalWork(), remaining, craftingJob.storageReservation(), craftingJob.reservedInputs(),
                    craftingJob.outputs(), craftingJob.workingItems(), craftingJob.steps());
            setDirty();
            return null;
        }
        CraftingJob completed = craftingJob;
        craftingJob = null;
        completed.outputs().forEach(this::add);
        setDirty();
        return completed;
    }

    private static void applyStep(Map<ResourceLocation, Long> working, CraftingStep step) {
        for (SlotEntry entry : step.inputs()) {
            long left = working.getOrDefault(entry.itemId(), 0L) - entry.count();
            if (left > 0) working.put(entry.itemId(), left);
            else working.remove(entry.itemId());
        }
        step.outputs().forEach((id, amount) -> working.merge(id, amount, DepotSavedData::saturatedAdd));
    }

    private static Map<ResourceLocation, Long> loadCounts(CompoundTag tag) {
        Map<ResourceLocation, Long> result = new ConcurrentHashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            long count = tag.getLong(key);
            if (id != null && count > 0) result.put(id, count);
        }
        return result;
    }

    private static CompoundTag saveCounts(Map<ResourceLocation, Long> counts) {
        CompoundTag tag = new CompoundTag();
        counts.forEach((id, count) -> {
            if (id != null && count > 0) tag.putLong(id.toString(), count);
        });
        return tag;
    }

    private static List<CraftingStep> loadSteps(ListTag tags) {
        List<CraftingStep> steps = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = tags.getCompound(i);
            ResourceLocation outputId = ResourceLocation.tryParse(tag.getString("output"));
            long outputAmount = tag.getLong("amount");
            long work = tag.getLong("work");
            if (outputId != null && outputAmount > 0 && work >= 0) {
                steps.add(new CraftingStep(outputId, outputAmount, work,
                        loadSlotEntries(tag.getCompound("inputs")), loadCounts(tag.getCompound("outputs")),
                        tag.getBoolean("processing"), loadIds(tag.getList("machineTypes", Tag.TAG_STRING))));
            }
        }
        return List.copyOf(steps);
    }

    private static ListTag saveSteps(List<CraftingStep> steps) {
        ListTag tags = new ListTag();
        for (CraftingStep step : steps) {
            CompoundTag tag = new CompoundTag();
            tag.putString("output", step.outputId().toString());
            tag.putLong("amount", step.outputAmount());
            tag.putLong("work", step.work());
            tag.put("inputs", saveSlotEntries(step.inputs()));
            tag.put("outputs", saveCounts(step.outputs()));
            tag.putBoolean("processing", step.processing());
            tag.put("machineTypes", saveIds(step.machineTypes()));
            tags.add(tag);
        }
        return tags;
    }

    private static List<SlotEntry> loadSlotEntries(CompoundTag tag) {
        List<SlotEntry> result = new ArrayList<>();
        ListTag entries = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            long count = entry.getLong("count");
            if (id != null && count > 0) result.add(new SlotEntry(id, count));
        }
        return List.copyOf(result);
    }

    private static CompoundTag saveSlotEntries(List<SlotEntry> entries) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SlotEntry entry : entries) {
            if (entry.itemId() == null || entry.count() <= 0) continue;
            CompoundTag saved = new CompoundTag();
            saved.putString("id", entry.itemId().toString());
            saved.putLong("count", entry.count());
            list.add(saved);
        }
        tag.put("entries", list);
        return tag;
    }

    private static List<ResourceLocation> loadIds(ListTag tags) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(tags.getString(i));
            if (id != null) ids.add(id);
        }
        return List.copyOf(ids);
    }

    private static ListTag saveIds(List<ResourceLocation> ids) {
        ListTag tags = new ListTag();
        ids.forEach(id -> tags.add(StringTag.valueOf(id.toString())));
        return tags;
    }

    private static long total(Map<ResourceLocation, Long> counts) {
        long total = 0;
        for (long count : counts.values()) total = saturatedAdd(total, count);
        return total;
    }

    public static long saturatedAdd(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }

    private static String searchKey(ResourceLocation id) {
        return SEARCH_CACHE.computeIfAbsent(id, key -> {
            var item = BuiltInRegistries.ITEM.get(key);
            if (item == null) return (key.getNamespace() + " " + key.getPath()).toLowerCase(Locale.ROOT);

            String display = new ItemStack(item).getHoverName().getString();
            return (display + " " + key.getNamespace() + " " + key.getPath()).toLowerCase(Locale.ROOT);
        });
    }

    // ===== Storage API (SAFE) =====

    public long getCount(ResourceLocation itemId) {
        return counts.getLong(itemId);
    }

    public void fillStackedContents(StackedContents contents) {
        counts.object2LongEntrySet().forEach(entry -> {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item == null || item == net.minecraft.world.item.Items.AIR || entry.getLongValue() <= 0) return;
            ItemStack stack = new ItemStack(item);
            stack.setCount((int) Math.min(Integer.MAX_VALUE, entry.getLongValue()));
            contents.accountStack(stack, Integer.MAX_VALUE);
        });
    }

    /**
     * SAFE deposit method: respects capacity.
     * @return how many were accepted (0..amount)
     */
    public long deposit(ResourceLocation itemId, long amount) {
        if (amount <= 0) return 0;
        if (!accepts(itemId)) return 0;

        long free = getFree();
        long toAdd = Math.min(free, amount);
        if (toAdd <= 0) return 0;

        counts.put(itemId, counts.getLong(itemId) + toAdd);
        setDirty();
        return toAdd;
    }

    public boolean accepts(ResourceLocation itemId) {
        return itemId != null && !itemId.equals(UPLINK_ID);
    }

    /**
     * SAFE deposit for ItemStack count.
     * Uses the registry id of the stack item.
     * @return how many items were accepted
     */
    public long depositStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return 0;
        return deposit(id, stack.getCount());
    }

    /**
     * Convenience: tries to deposit as much of the stack as possible and shrinks it by accepted amount.
     * @return accepted amount
     */
    public long tryDepositAll(ItemStack stack) {
        long accepted = depositStack(stack);
        if (accepted > 0) {
            stack.shrink((int) accepted);
        }
        return accepted;
    }

    // Backward compatibility: keep addCapped name if other code calls it
    public long addCapped(ResourceLocation itemId, long amount) {
        return deposit(itemId, amount);
    }

    // ===== Storage API (UNSAFE) =====

    /**
     * UNSAFE: ignores capacity. Only use for admin/debug/migrations.
     */
    public void add(ResourceLocation itemId, long amount) {
        if (amount <= 0) return;
        if (itemId == null) return;
        counts.put(itemId, counts.getLong(itemId) + amount);
        setDirty();
    }

    public long remove(ResourceLocation itemId, long amount) {
        if (amount <= 0) return 0;
        if (itemId == null) return 0;

        long have = counts.getLong(itemId);
        long take = Math.min(have, amount);
        if (take <= 0) return 0;

        long left = have - take;
        if (left <= 0) counts.removeLong(itemId);
        else counts.put(itemId, left);

        setDirty();
        return take;
    }

    public List<Entry> page(String search, int page, int pageSize) {
        List<Entry> all = filteredEntries(search);
        int start = Math.max(0, page);
        if (start >= all.size()) return List.of();
        return all.subList(start, Math.min(all.size(), start + pageSize));
    }

    public int countEntries(String search) {
        return filteredEntries(search).size();
    }

    public List<Entry> entries() {
        return List.copyOf(filteredEntries(""));
    }

    private List<Entry> filteredEntries(String search) {
        String raw = (search == null ? "" : search).trim().toLowerCase(Locale.ROOT);

        String modFilter = null;
        String textFilter = raw;

        if (raw.contains("@")) {
            String[] parts = raw.split("\\s+");
            StringBuilder rest = new StringBuilder();
            for (String p : parts) {
                if (p.startsWith("@") && p.length() > 1 && modFilter == null) {
                    modFilter = p.substring(1);
                } else if (!p.isBlank()) {
                    if (rest.length() > 0) rest.append(' ');
                    rest.append(p);
                }
            }
            textFilter = rest.toString();
        }

        List<Entry> all = new ArrayList<>();

        for (var e : counts.object2LongEntrySet()) {
            long count = e.getLongValue();
            if (count <= 0) continue;

            ResourceLocation id = e.getKey();

            if (modFilter != null && !id.getNamespace().toLowerCase(Locale.ROOT).contains(modFilter)) {
                continue;
            }

            if (!textFilter.isEmpty()) {
                String key = searchKey(id);
                if (!key.contains(textFilter)) continue;
            }

            all.add(new Entry(id, count));
        }

        all.sort(Comparator
                .comparingLong(DepotSavedData.Entry::count).reversed()
                .thenComparing(a -> a.itemId().toString()));
        return all;
    }
}
