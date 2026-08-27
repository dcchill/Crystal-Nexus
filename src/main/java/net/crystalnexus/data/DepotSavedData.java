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
import net.crystalnexus.automation.DepotProgram;
import net.crystalnexus.automation.DepotProgramRuntime;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.integration.DepotStorageBridge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final Map<Integer, CraftingJob> craftingJobs = new LinkedHashMap<>();
    private final Map<Integer, ProcessingTask> processingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, DepotProgram> programs = new LinkedHashMap<>();
    private int nextCraftingJobId = 1;

    // ===== Stored items =====
    private final Object2LongMap<ResourceLocation> counts = new Object2LongOpenHashMap<>();
    private transient @Nullable DepotStorageBridge storageBridge;

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
        ListTag jobs = tag.getList("craftingJobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < jobs.size(); i++) data.loadJob(jobs.getCompound(i));
        // Migrate worlds saved before concurrent crafting jobs were supported.
        if (data.craftingJobs.isEmpty() && tag.contains("craftingJob")) data.loadJob(tag.getCompound("craftingJob"));

        ListTag tasks = tag.getList("processingTasks", Tag.TAG_COMPOUND);
        for (int i = 0; i < tasks.size(); i++) data.loadTask(tasks.getCompound(i));
        if (data.processingTasks.isEmpty() && data.getCraftingJob() != null && tag.contains("processingTask")) {
            CompoundTag legacy = tag.getCompound("processingTask").copy();
            legacy.putInt("jobId", data.getCraftingJob().id());
            data.loadTask(legacy);
        }

        ListTag savedPrograms = tag.getList("programs", Tag.TAG_COMPOUND);
        for (int i = 0; i < savedPrograms.size(); i++) {
            DepotProgram program = DepotProgram.load(savedPrograms.getCompound(i));
            if (program != null) data.programs.put(program.id(), program);
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
        ListTag jobs = new ListTag();
        craftingJobs.values().forEach(job -> jobs.add(saveJob(job)));
        tag.put("craftingJobs", jobs);
        ListTag tasks = new ListTag();
        processingTasks.forEach((jobId, task) -> tasks.add(saveTask(jobId, task)));
        tag.put("processingTasks", tasks);

        ListTag savedPrograms = new ListTag();
        programs.values().forEach(program -> savedPrograms.add(program.save()));
        tag.put("programs", savedPrograms);

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
        if (activeStorageBridge() != null) return Long.MAX_VALUE;
        return getLocalCapacity();
    }

    public long getLocalCapacity() {
        long baseCapacity = CrystalnexusConfig.MACHINES.depotBaseCapacity();
        if (upgradeLevel >= 63 || baseCapacity > (Long.MAX_VALUE >> upgradeLevel)) return Long.MAX_VALUE;
        return baseCapacity << upgradeLevel;
    }

    /** Total items stored (sum of all counts). */
    public long getUsed() {
        long sum = 0L;
        for (long v : combinedCounts().values()) {
            if (v > 0) {
                if (v > Long.MAX_VALUE - sum) return Long.MAX_VALUE;
                sum += v;
            }
        }
        for (CraftingJob job : craftingJobs.values()) {
            long reserved = job.reservedSpace();
            if (reserved > Long.MAX_VALUE - sum) return Long.MAX_VALUE;
            sum += reserved;
        }
        return sum;
    }

    public long getFree() {
        long free = getCapacity() - getUsed();
        return Math.max(0L, free);
    }

    public long getLocalFree() {
        long used = 0L;
        for (var entry : counts.object2LongEntrySet()) {
            long count = entry.getLongValue();
            if (count <= 0) continue;
            if (count > Long.MAX_VALUE - used) return 0L;
            used += count;
        }
        for (CraftingJob job : craftingJobs.values()) {
            long reserved = job.reservedSpace();
            if (reserved > Long.MAX_VALUE - used) return 0L;
            used += reserved;
        }
        return Math.max(0L, getLocalCapacity() - used);
    }

    public boolean canInsert(long amount) {
        if (amount <= 0) return true;
        return amount <= getFree();
    }

    /** @return whether the depot was upgraded. */
    public boolean addUpgrade() {
        if (upgradeLevel >= MAX_UPGRADE_LEVEL || getLocalCapacity() == Long.MAX_VALUE) return false;
        upgradeLevel++;
        setDirty();
        return true;
    }

    public @Nullable ResourceLocation getPreferredRecipe(ResourceLocation itemId) {
        return preferredRecipes.get(itemId);
    }

    public Map<ResourceLocation, ResourceLocation> preferredRecipesSnapshot() {
        return Map.copyOf(preferredRecipes);
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

    public Map<ResourceLocation, ResourceLocation> preferredMachinesSnapshot() {
        return Map.copyOf(preferredMachines);
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
        CraftingJob job = getCraftingJob();
        return job == null ? null : processingTasks.get(job.id());
    }

    public @Nullable ProcessingTask getProcessingTask(int jobId) {
        return processingTasks.get(jobId);
    }

    public boolean isProcessingMachineInUse(int exceptJobId, ResourceLocation dimension, BlockPos pos) {
        return processingTasks.entrySet().stream().anyMatch(entry -> entry.getKey() != exceptJobId
                && entry.getValue().dimension().equals(dimension) && entry.getValue().machinePos().equals(pos));
    }

    public @Nullable CraftingJob getCraftingJob() {
        return craftingJobs.values().stream().findFirst().orElse(null);
    }

    public @Nullable CraftingJob getCraftingJob(int id) {
        return craftingJobs.get(id);
    }

    public List<CraftingJob> getCraftingJobs() {
        return List.copyOf(craftingJobs.values());
    }

    public List<DepotProgram> getPrograms() { return List.copyOf(programs.values()); }

    public void putProgram(DepotProgram program) {
        if (program == null) return;
        programs.put(program.id(), program);
        setDirty();
    }

    public boolean removeProgram(UUID id) {
        if (id == null || programs.remove(id) == null) return false;
        setDirty();
        return true;
    }

    public boolean toggleProgram(UUID id) {
        DepotProgram program = id == null ? null : programs.get(id);
        if (program == null) return false;
        programs.put(id, program.withEnabled(!program.enabled()));
        setDirty();
        return true;
    }

    public @Nullable CraftingJob startCraftingJob(ResourceLocation targetId, int amount, long totalWork,
            long storageReservation, Map<ResourceLocation, Long> reservedInputs,
            Map<ResourceLocation, Long> outputs, List<CraftingStep> steps) {
        if (targetId == null || amount <= 0 || totalWork <= 0
                || storageReservation <= 0 || outputs.isEmpty() || steps.isEmpty()) return null;
        if (reservedInputs.entrySet().stream().anyMatch(entry -> entry.getValue() <= 0
                || getCount(entry.getKey()) < entry.getValue())) return null;
        long reservedCount = total(reservedInputs);
        if (storageReservation > reservedCount && storageReservation - reservedCount > getFree()) return null;
        reservedInputs.forEach(this::remove);
        int id = nextCraftingJobId++;
        if (nextCraftingJobId <= 0) nextCraftingJobId = 1;
        CraftingJob job = new CraftingJob(id, targetId, amount, totalWork, totalWork, storageReservation,
                reservedInputs, outputs, reservedInputs, steps);
        craftingJobs.put(id, job);
        setDirty();
        return job;
    }

    public @Nullable CraftingJob advanceCraftingJob(int processors) {
        CraftingJob job = getCraftingJob();
        return job == null ? null : advanceCraftingJob(job.id(), processors);
    }

    public @Nullable CraftingJob advanceCraftingJob(int jobId, int processors) {
        CraftingJob job = craftingJobs.get(jobId);
        if (job == null || processors <= 0) return null;
        if (job.steps().isEmpty()) return advanceLegacyCraftingJob(job, processors);
        long completedBefore = job.totalWork() - job.remainingWork();
        long advance = processors;
        long stepStart = 0;
        for (CraftingStep step : job.steps()) {
            long stepEnd = saturatedAdd(stepStart, step.work());
            if (stepEnd > completedBefore && step.processing()) {
                advance = Math.min(advance, Math.max(0, stepStart - completedBefore));
                break;
            }
            stepStart = stepEnd;
        }
        if (advance <= 0) return null;
        long remaining = Math.max(0, job.remainingWork() - advance);
        long completedAfter = job.totalWork() - remaining;
        Map<ResourceLocation, Long> working = new ConcurrentHashMap<>(job.workingItems());
        long remainingTargetInputs = 0;
        for (CraftingStep step : job.steps()) {
            long stepTargetInput = 0;
            for (SlotEntry entry : step.inputs()) {
                if (entry.itemId().equals(job.targetId())) stepTargetInput = saturatedAdd(stepTargetInput, entry.count());
            }
            remainingTargetInputs = saturatedAdd(remainingTargetInputs, stepTargetInput);
        }
        long released = 0;
        long stepEnd = 0;
        for (CraftingStep step : job.steps()) {
            stepEnd = saturatedAdd(stepEnd, step.work());
            long targetInput = 0;
            for (SlotEntry entry : step.inputs()) {
                if (entry.itemId().equals(job.targetId())) targetInput = saturatedAdd(targetInput, entry.count());
            }
            if (completedBefore >= stepEnd) {
                remainingTargetInputs = Math.max(0, remainingTargetInputs - targetInput);
            } else if (completedAfter >= stepEnd) {
                remainingTargetInputs = Math.max(0, remainingTargetInputs - targetInput);
                if (!step.processing()) applyStep(working, step);
                long targetCount = working.getOrDefault(job.targetId(), 0L);
                released = saturatedAdd(released, releaseTarget(job, working, remainingTargetInputs, targetCount));
            }
        }
        long reservation = Math.max(0, job.storageReservation() - released);
        if (remaining > 0) {
            craftingJobs.put(jobId, new CraftingJob(job.id(), job.targetId(), job.amount(),
                    job.totalWork(), remaining, reservation, job.reservedInputs(),
                    job.outputs(), working, job.steps()));
            setDirty();
            return null;
        }
        craftingJobs.remove(jobId);
        processingTasks.remove(jobId);
        working.forEach(this::add);
        setDirty();
        return new CraftingJob(job.id(), job.targetId(), job.amount(), job.totalWork(), 0,
                reservation, job.reservedInputs(), job.outputs(), working, job.steps());
    }

    public @Nullable CraftingJob cancelCraftingJob(int id) {
        CraftingJob cancelled = craftingJobs.remove(id);
        if (cancelled == null) return null;
        processingTasks.remove(id);
        (cancelled.steps().isEmpty() ? cancelled.reservedInputs() : cancelled.workingItems()).forEach(this::add);
        setDirty();
        return cancelled;
    }

    public @Nullable CraftingJob updateProcessingTask(ProcessingTask task,
            Map<ResourceLocation, Long> inserted, Map<ResourceLocation, Long> extracted) {
        CraftingJob job = getCraftingJob();
        return job == null ? null : updateProcessingTask(job.id(), task, inserted, extracted);
    }

    public @Nullable CraftingJob updateProcessingTask(int jobId, ProcessingTask task,
            Map<ResourceLocation, Long> inserted, Map<ResourceLocation, Long> extracted) {
        CraftingJob job = craftingJobs.get(jobId);
        if (job == null || task == null || job.currentStep() == null || !job.currentStep().processing()) return null;
        Map<ResourceLocation, Long> working = new ConcurrentHashMap<>(job.workingItems());
        for (Map.Entry<ResourceLocation, Long> entry : inserted.entrySet()) {
            long left = working.getOrDefault(entry.getKey(), 0L) - entry.getValue();
            if (left < 0) return null;
            if (left == 0) working.remove(entry.getKey());
            else working.put(entry.getKey(), left);
        }
        extracted.forEach((id, amount) -> working.merge(id, amount, DepotSavedData::saturatedAdd));
        processingTasks.put(jobId, task);
        if (!task.remainingInputs().isEmpty() || !task.remainingOutputs().isEmpty()) {
            craftingJobs.put(jobId, new CraftingJob(job.id(), job.targetId(), job.amount(),
                    job.totalWork(), job.remainingWork(), job.storageReservation(),
                    job.reservedInputs(), job.outputs(), working, job.steps()));
            setDirty();
            return null;
        }

        CraftingJob active = job;
        int completedStep = active.currentStepIndex();
        long futureTargetInputs = 0;
        for (int i = completedStep + 1; i < active.steps().size(); i++) {
            for (SlotEntry entry : active.steps().get(i).inputs()) {
                if (entry.itemId().equals(active.targetId())) futureTargetInputs = saturatedAdd(futureTargetInputs, entry.count());
            }
        }
        long released = releaseTarget(active, working, futureTargetInputs,
                working.getOrDefault(active.targetId(), 0L));
        long reservation = Math.max(0, active.storageReservation() - released);
        long remaining = Math.max(0, active.remainingWork() - active.currentStep().work());
        processingTasks.remove(jobId);
        if (remaining > 0) {
            craftingJobs.put(jobId, new CraftingJob(active.id(), active.targetId(), active.amount(), active.totalWork(),
                    remaining, reservation, active.reservedInputs(), active.outputs(), working, active.steps()));
            setDirty();
            return null;
        }
        craftingJobs.remove(jobId);
        working.forEach(this::add);
        setDirty();
        return new CraftingJob(active.id(), active.targetId(), active.amount(), active.totalWork(), 0,
                reservation, active.reservedInputs(), active.outputs(), working, active.steps());
    }

    private long releaseTarget(CraftingJob job, Map<ResourceLocation, Long> working,
            long remainingTargetInputs, long targetCount) {
        long available = Math.max(0, targetCount - remainingTargetInputs);
        if (available <= 0) return 0;
        if (available == targetCount) working.remove(job.targetId());
        else working.put(job.targetId(), targetCount - available);
        add(job.targetId(), available);
        return available;
    }

    private @Nullable CraftingJob advanceLegacyCraftingJob(CraftingJob job, int processors) {
        long remaining = Math.max(0, job.remainingWork() - processors);
        if (remaining > 0) {
            craftingJobs.put(job.id(), new CraftingJob(job.id(), job.targetId(), job.amount(),
                    job.totalWork(), remaining, job.storageReservation(), job.reservedInputs(),
                    job.outputs(), job.workingItems(), job.steps()));
            setDirty();
            return null;
        }
        craftingJobs.remove(job.id());
        processingTasks.remove(job.id());
        job.outputs().forEach(this::add);
        setDirty();
        return job;
    }

    private static void applyStep(Map<ResourceLocation, Long> working, CraftingStep step) {
        for (SlotEntry entry : step.inputs()) {
            long left = working.getOrDefault(entry.itemId(), 0L) - entry.count();
            if (left > 0) working.put(entry.itemId(), left);
            else working.remove(entry.itemId());
        }
        step.outputs().forEach((id, amount) -> working.merge(id, amount, DepotSavedData::saturatedAdd));
    }

    private void loadJob(CompoundTag tag) {
        ResourceLocation targetId = ResourceLocation.tryParse(tag.getString("target"));
        int id = tag.getInt("id");
        int amount = tag.getInt("amount");
        long totalWork = tag.getLong("totalWork");
        long remainingWork = tag.getLong("remainingWork");
        if (targetId == null || id <= 0 || amount <= 0 || totalWork <= 0 || remainingWork <= 0) return;
        Map<ResourceLocation, Long> reservedInputs = loadCounts(tag.getCompound("reservedInputs"));
        Map<ResourceLocation, Long> outputs = loadCounts(tag.getCompound("outputs"));
        List<CraftingStep> steps = loadSteps(tag.getList("steps", Tag.TAG_COMPOUND));
        Map<ResourceLocation, Long> workingItems = loadCounts(tag.getCompound("workingItems"));
        long reservation = tag.contains("storageReservation") ? tag.getLong("storageReservation")
                : Math.max(total(reservedInputs), total(outputs));
        craftingJobs.put(id, new CraftingJob(id, targetId, amount, totalWork, remainingWork,
                Math.max(0, reservation), reservedInputs, outputs, workingItems, steps));
        nextCraftingJobId = Math.max(nextCraftingJobId, id + 1);
    }

    private static CompoundTag saveJob(CraftingJob job) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", job.id());
        tag.putString("target", job.targetId().toString());
        tag.putInt("amount", job.amount());
        tag.putLong("totalWork", job.totalWork());
        tag.putLong("remainingWork", job.remainingWork());
        tag.putLong("storageReservation", job.storageReservation());
        tag.put("reservedInputs", saveCounts(job.reservedInputs()));
        tag.put("outputs", saveCounts(job.outputs()));
        tag.put("workingItems", saveCounts(job.workingItems()));
        tag.put("steps", saveSteps(job.steps()));
        return tag;
    }

    private void loadTask(CompoundTag tag) {
        int jobId = tag.getInt("jobId");
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        if (!craftingJobs.containsKey(jobId) || dimension == null || !tag.contains("machinePos")) return;
        processingTasks.put(jobId, new ProcessingTask(dimension, BlockPos.of(tag.getLong("machinePos")),
                loadSlotEntries(tag.getCompound("remainingInputs")),
                loadCounts(tag.getCompound("remainingOutputs"))));
    }

    private static CompoundTag saveTask(int jobId, ProcessingTask task) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("jobId", jobId);
        tag.putString("dimension", task.dimension().toString());
        tag.putLong("machinePos", task.machinePos().asLong());
        tag.put("remainingInputs", saveSlotEntries(task.remainingInputs()));
        tag.put("remainingOutputs", saveCounts(task.remainingOutputs()));
        return tag;
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
        long local = getLocalCount(itemId);
        if (!accepts(itemId)) return local;
        DepotStorageBridge bridge = activeStorageBridge();
        return bridge == null ? local : saturatedAdd(local, bridge.getCount(itemId));
    }

    public long getLocalCount(ResourceLocation itemId) {
        return itemId == null ? 0L : counts.getLong(itemId);
    }

    public void fillStackedContents(StackedContents contents) {
        combinedCounts().forEach((itemId, count) -> {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR || count <= 0) return;
            ItemStack stack = new ItemStack(item);
            stack.setCount((int) Math.min(Integer.MAX_VALUE, count));
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

        long inserted = 0L;
        DepotStorageBridge bridge = activeStorageBridge();
        if (bridge != null) inserted = Math.min(amount, Math.max(0L, bridge.insert(itemId, amount)));
        long remaining = amount - inserted;
        long accepted = saturatedAdd(inserted, depositLocalRaw(itemId, remaining));
        DepotProgramRuntime.itemChanged(this, itemId, accepted);
        return accepted;
    }

    public long depositLocal(ResourceLocation itemId, long amount) {
        long accepted = depositLocalRaw(itemId, amount);
        DepotProgramRuntime.itemChanged(this, itemId, accepted);
        return accepted;
    }

    private long depositLocalRaw(ResourceLocation itemId, long amount) {
        if (amount <= 0 || !accepts(itemId)) return 0;
        long free = getLocalFree();
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
        DepotStorageBridge bridge = accepts(itemId) ? activeStorageBridge() : null;
        long inserted = bridge == null ? 0L : Math.min(amount, Math.max(0L, bridge.insert(itemId, amount)));
        addLocalRaw(itemId, amount - inserted);
        DepotProgramRuntime.itemChanged(this, itemId, amount);
    }

    public void addLocal(ResourceLocation itemId, long amount) {
        if (amount <= 0 || itemId == null) return;
        addLocalRaw(itemId, amount);
        DepotProgramRuntime.itemChanged(this, itemId, amount);
    }

    private void addLocalRaw(ResourceLocation itemId, long amount) {
        counts.put(itemId, counts.getLong(itemId) + amount);
        setDirty();
    }

    public long remove(ResourceLocation itemId, long amount) {
        if (amount <= 0) return 0;
        if (itemId == null) return 0;

        DepotStorageBridge bridge = activeStorageBridge();
        long extracted = bridge == null ? 0L : Math.min(amount, Math.max(0L, bridge.extract(itemId, amount)));
        long remaining = amount - extracted;
        long removed = saturatedAdd(extracted, removeLocalRaw(itemId, remaining));
        DepotProgramRuntime.itemChanged(this, itemId, -removed);
        return removed;
    }

    public long removeLocal(ResourceLocation itemId, long amount) {
        long removed = removeLocalRaw(itemId, amount);
        DepotProgramRuntime.itemChanged(this, itemId, -removed);
        return removed;
    }

    private long removeLocalRaw(ResourceLocation itemId, long amount) {
        if (amount <= 0 || itemId == null) return 0;

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

    public Map<ResourceLocation, Long> countSnapshot() {
        return Map.copyOf(combinedCounts());
    }

    public List<Entry> localEntries() {
        List<Entry> local = new ArrayList<>();
        counts.object2LongEntrySet().forEach(entry -> {
            if (entry.getLongValue() > 0) local.add(new Entry(entry.getKey(), entry.getLongValue()));
        });
        return List.copyOf(local);
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

        for (var e : combinedCounts().entrySet()) {
            long count = e.getValue();
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

    public void setStorageBridge(@Nullable DepotStorageBridge bridge) {
        storageBridge = bridge;
    }

    public boolean hasStorageBridge(DepotStorageBridge bridge) {
        return storageBridge == bridge && bridge != null && bridge.isConnected();
    }

    private @Nullable DepotStorageBridge activeStorageBridge() {
        DepotStorageBridge bridge = storageBridge;
        if (bridge == null || !bridge.isConnected()) {
            storageBridge = null;
            return null;
        }
        return bridge;
    }

    private Map<ResourceLocation, Long> combinedCounts() {
        Map<ResourceLocation, Long> result = new ConcurrentHashMap<>();
        counts.object2LongEntrySet().forEach(entry -> {
            if (entry.getLongValue() > 0) result.put(entry.getKey(), entry.getLongValue());
        });
        DepotStorageBridge bridge = activeStorageBridge();
        if (bridge != null) {
            bridge.snapshot().forEach((id, count) -> {
                if (accepts(id) && count != null && count > 0) {
                    result.merge(id, count, DepotSavedData::saturatedAdd);
                }
            });
        }
        return result;
    }
}
