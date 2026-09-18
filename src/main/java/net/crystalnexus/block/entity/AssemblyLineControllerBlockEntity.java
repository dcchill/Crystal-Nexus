package net.crystalnexus.block.entity;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.assembly.*;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class AssemblyLineControllerBlockEntity extends BlockEntity implements MenuProvider, net.crystalnexus.multiblock.MultiblockPortTarget {
    /** Player-authored node program. Legacy queued jobs remain below for migration/recovery. */
    private AssemblyGraph graph = new AssemblyGraph();
    public final ItemStackHandler inventory = new ItemStackHandler(36) {
        @Override protected void onContentsChanged(int slot) { planDirty = true; setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot < 27; }
    };
    // Reserved raw materials and intermediates are physically held here, never exposed to automation.
    private final ItemStackHandler reserved = new ItemStackHandler(108);
    private final FluidTank fluidBuffer = new FluidTank(16_000) { @Override protected void onContentsChanged() { setChanged(); } };
    private final IFluidHandler fluidPorts = new IFluidHandler() {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return fluidBuffer.getFluid(); }
        public int getTankCapacity(int tank) { return fluidBuffer.getCapacity(); }
        public boolean isFluidValid(int tank, FluidStack stack) { return true; }
        public int fill(FluidStack stack, FluidAction action) { return fluidBuffer.fill(stack, action); }
        public FluidStack drain(FluidStack stack, FluidAction action) { return fluidBuffer.drain(stack, action); }
        public FluidStack drain(int amount, FluidAction action) { return fluidBuffer.drain(amount, action); }
    };
    public final EnergyStorage energy = new EnergyStorage(16_000_000, 1_000_000, 1_000_000) {
        @Override public int receiveEnergy(int amount, boolean simulate) { int n = super.receiveEnergy(amount, simulate); if (!simulate && n > 0) setChanged(); return n; }
        @Override public int extractEnergy(int amount, boolean simulate) { int n = super.extractEnergy(amount, simulate); if (!simulate && n > 0) setChanged(); return n; }
    };
    public final IItemHandler ports = new IItemHandler() {
        public int getSlots() { return inventory.getSlots(); }
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot < 27 ? inventory.insertItem(slot, stack, simulate) : stack; }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot >= 27 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) { return slot < 27; }
    };
    private AssemblyLineStructure.Bounds bounds;
    private List<BlockPos> machines = List.of();
    private final Map<Long, AssemblyLineMachine.SideProfile> sideProfiles = new HashMap<>();
    private final Map<BlockPos, Block> blocks = new HashMap<>();
    private final Map<Integer, RecipeHolder<?>> taskRecipes = new HashMap<>();
    private final ArrayDeque<ItemStack> queue = new ArrayDeque<>();
    private final ArrayDeque<String> completedJobs = new ArrayDeque<>();
    private ProductionPlan active;
    private boolean formed, dirty = true;
    private boolean planDirty = true;
    private String status = "Build enclosure, then Rescan";
    private ItemStack configured = ItemStack.EMPTY;
    private String clientJobs = "", clientMachines = "";
    private long clientSystemEnergy;
    private final Set<ResourceLocation> learnedRecipes = new LinkedHashSet<>();
    public AssemblyLineControllerBlockEntity(BlockPos pos, BlockState state) { super(CrystalnexusModBlockEntities.ASSEMBLY_LINE_CONTROLLER.get(), pos, state); }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            dirty = true; formed = false;
            AssemblyLineEvents.register(this);
            rescan(); // Immediately validate structure on world load
        }
    }
    @Override public void setRemoved() { AssemblyLineEvents.unregister(this); super.setRemoved(); }
    public boolean formed() { return formed && !dirty; }
    public String status() { return status; }
    public String jobsText() { return clientJobs; }
    public String machinesText() { return clientMachines; }
    public long systemEnergy() {
        if (level == null || level.isClientSide) return clientSystemEnergy;
        long total = energy.getEnergyStored();
        for (AssemblyLineMachine worker : workers()) {
            if (worker.energy() != null) total += worker.energy().getEnergyStored();
        }
        return total;
    }
    public ItemStack configured() { return configured; }
    public AssemblyLineStructure.Bounds bounds() { return bounds; }
    public List<BlockPos> machinePositions() { return machines; }
    public ProductionPlan activePlan() { return active; }
    public AssemblyGraph graph() { return graph; }
    public boolean graphEnabled() { return graph.enabled && graph.validate().isEmpty(); }
    public void selectGraphRecipe(int nodeId, ResourceLocation recipe) {
        var node = graph.node(nodeId);
        if (node == null || recipe == null) return;
        node.recipe = recipe.toString();
        var holder = level.getRecipeManager().byKey(recipe);
        if (holder.isPresent()) {
            node.inputSockets = holder.get().value().getIngredients().size();
            node.inputItems = holder.get().value().getIngredients().stream().map(ingredient -> {
                ItemStack[] choices = ingredient.getItems();
                return choices.length == 0 ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(choices[0].getItem()).toString();
            }).toArray(String[]::new);
            node.inputFluids = new String[node.inputSockets];
            var machine = machineAt(BlockPos.of(node.machine));
            int[] physicalInputs = machine == null ? new int[0] : discoverInputSlots(machine, node.inputSockets);
            node.inputSlots = Arrays.copyOf(physicalInputs, node.inputSockets);
            node.outputSlots = discoverOutputSlots(machineAt(BlockPos.of(node.machine)));
            node.outputSockets = node.outputSlots.length;
            ItemStack result = holder.get().value().getResultItem(level.registryAccess());
            node.outputItems = new String[node.outputSockets];
            Arrays.fill(node.outputItems, result.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
            node.outputFluids = new String[0];
            node.outputExport = new boolean[node.outputSockets];
            Arrays.fill(node.outputExport, true);
        }
        node.status = "Recipe selected";
        graph.syncRevision(); setChanged(); sync();
    }
    public void selectGraphJeiRecipe(int nodeId, net.crystalnexus.cli.DepotJeiRecipeCache.Recipe recipe) {
        AssemblyGraph.Node node=graph.node(nodeId);
        if(node==null || recipe==null) return;
        node.recipe=recipe.id().toString();
        node.inputSockets=recipe.inputs().size();
        node.inputSlots=new int[node.inputSockets];
        for(int i=0;i<node.inputSlots.length;i++) node.inputSlots[i]=i;
        node.inputItems=recipe.inputs().stream().map(slot->slot.alternatives().isEmpty()?"":slot.alternatives().getFirst().itemId().toString()).toArray(String[]::new);
        node.inputFluids = new String[node.inputSockets];
        node.outputSockets=recipe.outputs().size();
        node.outputSlots=new int[node.outputSockets];
        for(int i=0;i<node.outputSlots.length;i++) node.outputSlots[i]=i;
        node.outputItems=recipe.outputs().stream().map(output->output.itemId().toString()).toArray(String[]::new);
        node.outputExport=new boolean[node.outputSockets]; Arrays.fill(node.outputExport,true);
        node.status="JEI: "+recipe.categoryName();
        graph.syncRevision(); setChanged(); sync();
    }
    public void selectNextGraphJeiRecipe(int nodeId, List<net.crystalnexus.cli.DepotJeiRecipeCache.Recipe> recipes) {
        AssemblyGraph.Node node=graph.node(nodeId);
        if(node==null || recipes.isEmpty()) return;
        int current=-1; for(int i=0;i<recipes.size();i++) if(recipes.get(i).id().toString().equals(node.recipe)) current=i;
        selectGraphJeiRecipe(nodeId, recipes.get(Math.floorMod(current+1, recipes.size())));
    }
    private int[] discoverOutputSlots(AssemblyLineMachine machine) {
        if (machine == null) return new int[] {1};
        var handler = machine.itemHandler();
        if (handler != null && machine.kind() == AssemblyLineMachine.Kind.GENERIC) {
            // For generic modded machines, just use all non-empty slots that aren't input slots.
            // Don't require canTakeItemThroughFace since modded machines may not implement it correctly.
            List<Integer> available = new ArrayList<>();
            int[] inputs = machine.inputs();
            for (int i = 0; i < handler.getSlots(); i++) {
                boolean isInput = false;
                for (int inputSlot : inputs) if (inputSlot == i) isInput = true;
                if (isInput) continue;
                available.add(i);
            }
            int[] slots = available.stream().mapToInt(Integer::intValue).toArray();
            return slots.length == 0 ? new int[] {0} : slots;
        }
        List<Integer> slots = new ArrayList<>();
        int[] inputs = machine.inputs();
        for (int slot = 0; slot < machine.inventory().getContainerSize(); slot++) {
            boolean input = false; for (int candidate : inputs) if (candidate == slot) input = true;
            if (input) continue;
            if (slot == 0 || slot == 2 || slot == 3 && machine.kind() != AssemblyLineMachine.Kind.CIRCUIT_PRESS) {
                // Slot 0 is conventionally an input and slots 2/3 are commonly upgrades.
                if (machine.kind() != AssemblyLineMachine.Kind.GENERIC) continue;
            }
            slots.add(slot);
        }
        return slots.isEmpty() ? new int[] {1} : slots.stream().mapToInt(Integer::intValue).toArray();
    }
    private int[] discoverInputSlots(AssemblyLineMachine machine, int count) {
        if (machine == null || count <= 0) return new int[0];
        if (machine.kind() != AssemblyLineMachine.Kind.GENERIC) return machine.inputs();
        var handler = machine.itemHandler();
        if (handler == null) return new int[0];
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots() && slots.size() < count; slot++) {
            ItemStack probe = new ItemStack(net.minecraft.world.item.Items.STONE);
            if (handler.insertItem(slot, probe, true).getCount() < probe.getCount()) slots.add(slot);
        }
        for (int slot = 0; slot < handler.getSlots() && slots.size() < count; slot++)
            if (!slots.contains(slot)) slots.add(slot);
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override public boolean acceptsMultiblockPort(BlockPos pos) {
        // Ports are valid anywhere on the shell, including edges and corners.
        // Requiring exactly one face made visually valid energy hatches silently reject FE.
        return formed && bounds != null && bounds.contains(pos) && bounds.faces(pos) >= 1;
    }
    @Override public net.neoforged.neoforge.energy.IEnergyStorage multiblockEnergyInput() { return energy; }
    @Override public IFluidHandler multiblockFluidInput() { return fluidPorts; }
    @Override public IFluidHandler multiblockFluidOutput() { return fluidPorts; }

    /** Fairly fills all discovered worker batteries, independent of graph run state. */
    private void distributeMachineEnergy() {
        distributeEnergyFrom(energy);
    }

    /**
     * Sends one tick's available FE directly and evenly to every receptive machine.
     * Full machines are removed and their unused share is redistributed in the same tick.
     */
    public int distributeEnergyFrom(net.neoforged.neoforge.energy.IEnergyStorage source) {
        if (!formed() || source == null || !source.canExtract() || source.getEnergyStored() <= 0) return 0;
        List<AssemblyLineMachine> allWorkers = workers();
        // Use canReceive() instead of receiveEnergy(1, true) because some mods (Mekanism)
        // have ForgeEnergyIntegration that returns 0 in simulate mode even when canReceive=true
        List<AssemblyLineMachine> targets = new ArrayList<>(allWorkers.stream()
            .filter(worker -> worker.energy() != null && worker.energy().canReceive()).toList());
        int budget = Math.min(1_000_000, source.getEnergyStored());
        int moved = 0;
        while (budget > 0 && !targets.isEmpty()) {
            int share = Math.max(1, (budget + targets.size() - 1) / targets.size());
            boolean progress = false;
            for (var iterator = targets.iterator(); iterator.hasNext() && budget > 0;) {
                AssemblyLineMachine worker = iterator.next();
                // Don't use simulate mode to check acceptance - Mekanism returns 0 in simulate
                // even when canReceive() is true. Just attempt the transfer directly.
                int extracted = source.extractEnergy(Math.min(share, budget), false);
                if (extracted <= 0) break;
                int received = worker.receiveEnergy(extracted, false);
                if (received < extracted) source.receiveEnergy(extracted - received, false);
                moved += received; budget -= received; progress |= received > 0;
                // Only remove from distribution list if truly can't receive (not just temporarily full)
                if (worker.energy() != null && !worker.energy().canReceive()) iterator.remove();
            }
            if (!progress) break;
        }
        return moved;
    }

    public void changedAt(BlockPos pos, boolean force) {
        boolean nearby = bounds == null || !formed ? Math.abs(pos.getX()-worldPosition.getX()) < 32 && Math.abs(pos.getY()-worldPosition.getY()) < 32 && Math.abs(pos.getZ()-worldPosition.getZ()) < 32 : bounds.contains(pos);
        if (nearby && (force || blocks.get(pos) != level.getBlockState(pos).getBlock())) invalidate();
    }
    public void invalidate() {
        dirty = true; formed = false; planDirty = true; setStatus("Structure changed; validating");
    }
    public void rescan() {
        var result = AssemblyLineStructure.scan(level, worldPosition);
        if (result.bounds() != null) bounds = result.bounds();
        formed = result.valid(); dirty = false; machines = result.machines(); blocks.clear();
        for (BlockPos pos : machines) learnSideProfile(pos);
        if (bounds != null) for (BlockPos p : BlockPos.betweenClosed(bounds.min(), bounds.max()))
            if (level.hasChunkAt(p)) blocks.put(p.immutable(), level.getBlockState(p).getBlock());
        setStatus(formed ? "Ready" : result.error());
        if (formed) { /* Assembly Line formed at {} */ }
        if (formed) discoverGraphMachines();
        if (formed) bindShellPorts();
        taskRecipes.clear();
        if (active != null) for (int i = 0; i < active.tasks.size(); i++) {
            var r = level.getRecipeManager().byKey(active.tasks.get(i).recipe); if (r.isPresent()) taskRecipes.put(i, r.get());
        }
        sync();
    }
    private void bindShellPorts() {
        if (bounds == null) return;
        for (BlockPos p : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            var be = level.getBlockEntity(p);
            if (be instanceof MachineFluidInputBlockEntity input) input.bindController(worldPosition);
            else if (be instanceof MultiblockFluidOutputBlockEntity output) output.bindController(worldPosition);
            else if (be instanceof MachineEnergyInputBlockEntity input) input.bindController(worldPosition);
        }
    }
    private void discoverGraphMachines() {
        Set<Long> present = new HashSet<>();
        for (BlockPos pos : machines) {
            present.add(pos.asLong());
            AssemblyGraph.Node existing = graph.nodes.stream().filter(n -> n.machine == pos.asLong()).findFirst().orElse(null);
            if (existing == null) {
                var state = level.getBlockState(pos);
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                int index = graph.nodes.size();
                graph.nodes.add(new AssemblyGraph.Node(graph.nextId(), pos.asLong(), id, 12 + (index % 4) * 92, 12 + (index / 4) * 52));
            } else if (existing.outputSockets > 0) {
                AssemblyLineMachine machine = machineAt(pos);
                if (machine != null && machine.kind() == AssemblyLineMachine.Kind.GENERIC
                    && existing.outputSlots.length != existing.outputSockets) {
                    existing.outputSlots = Arrays.copyOf(discoverOutputSlots(machine), existing.outputSockets);
                }
            }
        }
        if (bounds != null) for (BlockPos pos : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            var be = level.getBlockEntity(pos);
            if (!(be instanceof MultiblockItemInputBlockEntity) && !(be instanceof MultiblockItemOutputBlockEntity)
                && !(be instanceof MachineFluidInputBlockEntity) && !(be instanceof MultiblockFluidOutputBlockEntity)) continue;
            present.add(pos.asLong());
            if (graph.nodes.stream().anyMatch(n -> n.machine == pos.asLong())) continue;
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            int index = graph.nodes.size();
            AssemblyGraph.Node node = new AssemblyGraph.Node(graph.nextId(), pos.asLong(), id, 12 + (index % 4) * 210, 12 + (index / 4) * 120);
            if (be instanceof MultiblockItemInputBlockEntity) {
                node.inputSockets = 0; node.inputSlots = new int[0]; node.inputItems = new String[0]; node.inputFluids = new String[0];
                node.outputSockets = 4; node.outputSlots = new int[]{0,1,2,3}; node.outputItems = new String[]{"","","",""};
                node.outputExport = new boolean[]{false,false,false,false}; node.status = "Supplies graph inputs";
            } else if (be instanceof MultiblockFluidOutputBlockEntity) {
                node.inputSockets = 4; node.inputSlots = new int[]{0,1,2,3}; node.inputItems = new String[]{"","","",""}; node.inputFluids = new String[]{"","","",""};
                node.outputSockets = 0; node.outputSlots = new int[0]; node.outputItems = new String[0];
                node.outputFluids = new String[0]; node.outputExport = new boolean[0];
                node.status = "Receives graph fluids";
            } else if (be instanceof MachineFluidInputBlockEntity) {
                node.inputSockets = 0; node.inputSlots = new int[0]; node.inputItems = new String[0]; node.inputFluids = new String[0];
                node.outputSockets = 4; node.outputSlots = new int[]{0,1,2,3}; node.outputItems = new String[]{"","","",""};
                node.outputFluids = new String[]{"","","",""}; node.outputExport = new boolean[]{false,false,false,false};
                node.status = "Supplies graph fluids";
            } else {
                node.inputSockets = 4; node.inputSlots = new int[]{0,1,2,3}; node.inputItems = new String[]{"","","",""}; node.inputFluids = new String[]{"","","",""};
                node.outputSockets = 0; node.outputSlots = new int[0]; node.outputItems = new String[0];
                node.outputExport = new boolean[0]; node.status = "Receives graph outputs";
            }
            graph.nodes.add(node);
        }
        for (AssemblyGraph.Node node : graph.nodes) {
            var port = level.getBlockEntity(BlockPos.of(node.machine));
            if (port instanceof MachineFluidInputBlockEntity) {
                node.inputSockets = 0; node.inputSlots = new int[0]; node.inputItems = new String[0]; node.inputFluids = new String[0];
                node.outputSockets = 4; node.outputSlots = new int[]{0, 1, 2, 3}; node.outputItems = new String[]{"", "", "", ""};
                node.outputFluids = new String[]{"", "", "", ""}; node.outputExport = new boolean[]{false, false, false, false};
                node.status = "Supplies graph fluids";
            } else if (port instanceof MultiblockFluidOutputBlockEntity) {
                node.inputSockets = 4; node.inputSlots = new int[]{0, 1, 2, 3}; node.inputItems = new String[]{"", "", "", ""}; node.inputFluids = new String[]{"", "", "", ""};
                node.outputSockets = 0; node.outputSlots = new int[0]; node.outputItems = new String[0];
                node.outputFluids = new String[0]; node.outputExport = new boolean[0];
                node.status = "Receives graph fluids";
            }
        }
        graph.nodes.removeIf(n -> n.machine != 0 && !present.contains(n.machine));
        graph.edges.removeIf(e -> graph.node(e.from()) == null || graph.node(e.to()) == null);
        graph.syncRevision(); setChanged();
    }
    public void setGraphEnabled(boolean enabled) {
        if (enabled && !graph.validate().isEmpty()) { setStatus(graph.validate()); return; }
        graph.enabled = enabled; graph.syncRevision(); setChanged(); sync();
    }
    public boolean editGraph(String action, int a, int b, int c, int d, float x, float y) {
        if (action.equals("toggle")) { setGraphEnabled(!graph.enabled); return true; }
        if (action.equals("edge")) {
            if (graph.node(a) == null || graph.node(b) == null || a == b || graph.edges.size() >= AssemblyGraph.MAX_EDGES) return false;
            graph.edges.removeIf(e -> e.from() == a && e.output() == c && e.to() == b);
            if (d < 0) return false;
            var from = graph.node(a); var to = graph.node(b);
            if (from.outputSockets > 0 && c >= from.outputSockets) return false;
            if (to.inputSockets > 0 && d >= to.inputSockets) return false;
            graph.edges.add(new AssemblyGraph.Edge(a, c, b, d));
        } else if (action.equals("remove_edge")) graph.edges.removeIf(e ->
            b < 0 && c >= 0 ? e.from() == a && e.output() == c
                : b < 0 ? e.from() == a : e.from() == a && e.to() == b);
        else if (action.equals("remove_slot")) {
            var node = graph.node(a);
            if (node == null || c < 0) return false;
            if (b == 0) {
                if (c >= node.inputSockets) return false;
                node.inputSockets--;
                node.inputItems = remove(node.inputItems, c);
                node.inputFluids = remove(node.inputFluids, c);
                node.inputSlots = remove(node.inputSlots, c);
                graph.edges.removeIf(e -> e.to() == a && e.input() == c);
                List<AssemblyGraph.Edge> shifted = graph.edges.stream().map(e -> e.to() == a && e.input() > c
                    ? new AssemblyGraph.Edge(e.from(), e.output(), e.to(), e.input() - 1) : e).toList();
                graph.edges.clear(); graph.edges.addAll(shifted);
            } else {
                if (c >= node.outputSockets) return false;
                node.outputSockets--;
                node.outputItems = remove(node.outputItems, c);
                node.outputSlots = remove(node.outputSlots, c);
                node.outputExport = remove(node.outputExport, c);
                graph.edges.removeIf(e -> e.from() == a && e.output() == c);
                List<AssemblyGraph.Edge> shifted = graph.edges.stream().map(e -> e.from() == a && e.output() > c
                    ? new AssemblyGraph.Edge(e.from(), e.output() - 1, e.to(), e.input()) : e).toList();
                graph.edges.clear(); graph.edges.addAll(shifted);
            }
        }
        else if (action.equals("export")) { var n=graph.node(a); if(n==null || c<0 || c>=n.outputSockets) return false; if(n.outputExport.length!=n.outputSockets) n.outputExport=Arrays.copyOf(n.outputExport,n.outputSockets); n.outputExport[c]=b!=0; }
        else if (action.equals("move")) { var n = graph.node(a); if (n == null || !Float.isFinite(x) || !Float.isFinite(y)) return false; n.x=x; n.y=y; }
        else return false;
        String error = graph.validate(); if (!error.isEmpty()) { setStatus(error); return false; }
        graph.syncRevision(); setChanged(); sync(); return true;
    }
    public void enqueue(ItemStack target) {
        if (target.isEmpty() || target.getCount() < 1 || target.getCount() > ProductionPlan.MAX_OPERATIONS || queue.size() >= 16) { setStatus("Invalid quantity or queue full"); return; }
        configured = target.copy(); queue.add(target.copy()); planDirty = true; setChanged(); sync();
    }
    public void retry() { invalidate(); }
    public static void tick(Level level, BlockPos pos, BlockState state, AssemblyLineControllerBlockEntity be) {
        if (level.isClientSide) return;
        if (be.dirty) be.rescan();
        if (!be.formed()) return;
        be.distributeMachineEnergy();
        if (be.graph.enabled) net.crystalnexus.assembly.AssemblyGraphRuntime.tick(be);
        if (!be.graph.enabled) {
            if (be.active == null && !be.queue.isEmpty() && be.planDirty) { be.planDirty = false; be.startNext(); }
            if (be.active != null) be.schedule();
        }
        if (level.getGameTime() % 20 == 0) be.sync();
    }
    /** Graph execution gate and status refresh. */
    public void runGraphTick() {
        String error = graph.validate();
        if (!error.isEmpty()) { setStatus(error); graph.enabled = false; return; }
        for (AssemblyGraph.Node node : graph.nodes) {
            BlockPos machine = BlockPos.of(node.machine);
            if (isGraphPort(node)) continue;
            if (machineAt(machine) == null) { node.status = "Machine missing"; continue; }
            node.status = node.recipe.isEmpty() ? "Select recipe" : "Ready";
        }
        if (!graph.nodes.isEmpty() && graph.nodes.stream().allMatch(n -> !n.recipe.isEmpty())) setStatus("Graph active");
    }
    private static boolean isGraphPort(AssemblyGraph.Node node) {
        return node.block.endsWith(":multiblock_item_input")
            || node.block.endsWith(":multiblock_item_output")
            || node.block.endsWith(":machine_fluid_input")
            || node.block.endsWith(":multiblock_fluid_output");
    }
    public AssemblyLineMachine machineAt(BlockPos pos) {
        return level == null || !level.hasChunkAt(pos) ? null : AssemblyLineMachine.at(level, pos, sideProfiles.get(pos.asLong()));
    }

    private void learnSideProfile(BlockPos pos) {
        if (level == null) return;
        int insert = 0, extract = 0, fill = 0, drain = 0, receive = 0, energyExtract = 0;
        for (Direction side : Direction.values()) {
            var items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
            if (items != null) {
                boolean canInsert = false, canExtract = false;
                ItemStack probe = new ItemStack(net.minecraft.world.item.Items.STONE);
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    if (!items.insertItem(slot, probe, true).equals(probe)) canInsert = true;
                    if (!items.getStackInSlot(slot).isEmpty()
                        && !items.extractItem(slot, 1, true).isEmpty()) canExtract = true;
                }
                if (canInsert) insert |= 1 << side.ordinal();
                if (canExtract) extract |= 1 << side.ordinal();
            }
            var fluids = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
            if (fluids != null) {
                boolean canDrain = false, canFill = false;
                for (int tank = 0; tank < fluids.getTanks(); tank++) {
                    FluidStack contents = fluids.getFluidInTank(tank);
                    if (!contents.isEmpty() && !fluids.drain(contents.copyWithAmount(Math.min(1000, contents.getAmount())), IFluidHandler.FluidAction.SIMULATE).isEmpty()) canDrain = true;
                    if (!contents.isEmpty() && fluids.fill(contents.copyWithAmount(Math.min(1000, contents.getAmount())), IFluidHandler.FluidAction.SIMULATE) > 0) canFill = true;
                }
                if (canFill) fill |= 1 << side.ordinal();
                if (canDrain) drain |= 1 << side.ordinal();
            }
            var storage = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, side);
            if (storage != null) {
                if (storage.canReceive()) receive |= 1 << side.ordinal();
                if (storage.canExtract()) energyExtract |= 1 << side.ordinal();
            }
        }
        sideProfiles.put(pos.asLong(), new AssemblyLineMachine.SideProfile(insert, extract, fill, drain, receive, energyExtract));
        setChanged();
    }
    public ItemStack takeGraphItem(int nodeId, int output, int amount) {
        AssemblyGraph.Node node = graph.node(nodeId);
        if (node == null || node.machine == 0 || amount <= 0) return ItemStack.EMPTY;
        var sourceEntity = level.getBlockEntity(BlockPos.of(node.machine));
        if (sourceEntity instanceof MultiblockItemInputBlockEntity input) {
            if (output < 0 || output >= input.getContainerSize()) return ItemStack.EMPTY;
            ItemStack available = input.getItem(output);
            if (available.isEmpty()) return ItemStack.EMPTY;
            return input.removeItem(output, Math.min(amount, available.getCount()));
        }
        AssemblyLineMachine worker = machineAt(BlockPos.of(node.machine));
        if (worker == null || output < 0 || output >= node.outputSlots.length) return ItemStack.EMPTY;
        int slot = outputSlot(worker, node, output);
        var handler = worker.itemHandlerForExtract();
        if (worker.kind() == AssemblyLineMachine.Kind.GENERIC && handler != null) {
            return handler.extractItem(slot, amount, false);
        }
        ItemStack available = worker.inventory().getItem(slot);
        if (available.isEmpty()) return ItemStack.EMPTY;
        ItemStack moved = available.copyWithCount(Math.min(amount, available.getCount()));
        available.shrink(moved.getCount()); worker.inventory().setItem(slot, available);
        worker.entity().setChanged(); return moved;
    }
    public int outputSlot(AssemblyLineMachine worker, AssemblyGraph.Node node, int output) {
        if (worker == null || node == null || output < 0 || output >= node.outputSlots.length) return -1;
        IItemHandler handler = worker.itemHandlerForExtract();
        if (handler == null) return -1;
        String expected = node.outputItems != null && output < node.outputItems.length ? node.outputItems[output] : "";
        if (expected != null && !expected.isEmpty()) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(expected))
                    return slot;
            }
        }
        int configured = node.outputSlots[output];
        return configured >= 0 && configured < handler.getSlots() ? configured : -1;
    }
    public void returnGraphItem(int nodeId, int output, ItemStack stack) {
        if (stack.isEmpty()) return;
        AssemblyGraph.Node node = graph.node(nodeId);
        if (node != null && level.getBlockEntity(BlockPos.of(node.machine)) instanceof MultiblockItemInputBlockEntity input
            && output >= 0 && output < input.getContainerSize()) {
            ItemStack current = input.getItem(output);
            if (current.isEmpty()) input.setItem(output, stack.copy());
            else if (ItemStack.isSameItemSameComponents(current, stack)) current.grow(stack.getCount());
            input.setChanged(); return;
        }
        AssemblyLineMachine worker = node == null ? null : machineAt(BlockPos.of(node.machine));
        if (worker != null && output >= 0 && output < node.outputSlots.length) {
            int slot = node.outputSlots[output];
            var handler = worker.itemHandler();
            if (worker.kind() == AssemblyLineMachine.Kind.GENERIC && handler != null) {
                ItemStack leftover = handler.insertItem(slot, stack, false);
                if (!leftover.isEmpty()) put(reserved, leftover, 0, reserved.getSlots());
                return;
            }
            ItemStack existing = worker.inventory().getItem(slot);
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack)) {
                worker.inventory().setItem(slot, existing.isEmpty() ? stack.copy() : existing.copyWithCount(existing.getCount() + stack.getCount()));
                worker.entity().setChanged(); return;
            }
        }
        put(reserved, stack, 0, reserved.getSlots());
    }
    public FluidStack takeGraphFluid(int nodeId, int output, int amount) {
        if (amount <= 0 || level == null) return FluidStack.EMPTY;
        AssemblyGraph.Node source = graph.node(nodeId);
        if (source != null && output >= 0 && source.block.endsWith(":machine_fluid_input")) {
            var port = level.getBlockEntity(BlockPos.of(source.machine));
            if (port instanceof MachineFluidInputBlockEntity input)
                return input.getFluidInput().drain(amount, IFluidHandler.FluidAction.EXECUTE);
        }
        if (source != null && output >= 0 && !source.block.endsWith(":multiblock_fluid_output")
            && !source.block.endsWith(":machine_fluid_input")) {
            AssemblyLineMachine machine = machineAt(BlockPos.of(source.machine));
            IFluidHandler handler = machine == null ? null : machine.fluidHandler();
            if (handler != null && output < handler.getTanks())
                return handler.drain(Math.min(amount, handler.getFluidInTank(output).getAmount()),
                    IFluidHandler.FluidAction.EXECUTE);
        }
        for (BlockPos pos : shellPorts(net.crystalnexus.block.entity.MultiblockFluidOutputBlockEntity.class)) {
            var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler == null) continue;
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack fluid = handler.getFluidInTank(tank);
                if (!fluid.isEmpty()) return handler.drain(Math.min(amount, fluid.getAmount()), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return FluidStack.EMPTY;
    }
    public void returnGraphFluid(int nodeId, int output, FluidStack stack) {
        if (stack.isEmpty() || level == null) return;
        AssemblyGraph.Node source = graph.node(nodeId);
        if (source != null && source.block.endsWith(":machine_fluid_input")) {
            var port = level.getBlockEntity(BlockPos.of(source.machine));
            if (port instanceof MachineFluidInputBlockEntity input
                && input.getFluidInput().fill(stack, IFluidHandler.FluidAction.EXECUTE) > 0) return;
        }
        if (source != null && !source.block.endsWith(":multiblock_fluid_output")
            && !source.block.endsWith(":machine_fluid_input")) {
            AssemblyLineMachine machine = machineAt(BlockPos.of(source.machine));
            IFluidHandler handler = machine == null ? null : machine.fluidHandler();
            if (handler != null && handler.fill(stack, IFluidHandler.FluidAction.EXECUTE) > 0) return;
        }
        for (BlockPos pos : shellPorts(net.crystalnexus.block.entity.MachineFluidInputBlockEntity.class)) {
            var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler != null && handler.fill(stack, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) > 0) return;
        }
    }
    public boolean exportGraphFluid(FluidStack stack) {
        if (stack.isEmpty()) return false;
        int accepted = fluidBuffer.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        return accepted == stack.getAmount();
    }
    public boolean exportGraphItem(int nodeId, int output, ItemStack stack) {
        if (stack.isEmpty() || bounds == null || level == null) return false;
        ItemStack remaining = stack.copy();
        for (BlockPos pos : shellPorts(net.crystalnexus.block.entity.MultiblockItemOutputBlockEntity.class)) {
            var out = level.getBlockEntity(pos);
            if (out instanceof MultiblockItemOutputBlockEntity port) {
                if (port.insert(remaining, false)) return true;
                return false;
            }
        }
        return false;
    }
    public boolean insertGraphSink(int nodeId, int input, ItemStack stack) {
        AssemblyGraph.Node node = graph.node(nodeId);
        if (node == null || stack.isEmpty() || input < 0) return false;
        if (!(level.getBlockEntity(BlockPos.of(node.machine)) instanceof MultiblockItemOutputBlockEntity port)) return false;
        return port.insert(stack, false);
    }
    public void configureGraphItem(int nodeId, int socket, String itemId) {
        AssemblyGraph.Node node = graph.node(nodeId);
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (node == null || id == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) return;
        if (level.getBlockEntity(BlockPos.of(node.machine)) instanceof MultiblockItemInputBlockEntity
            && socket >= 0 && socket < node.outputSockets) {
            node.outputItems = Arrays.copyOf(node.outputItems, node.outputSockets); node.outputItems[socket] = id.toString();
            node.status = "Input filter configured"; graph.syncRevision(); setChanged(); sync();
        }
    }
    public void setGraphSlots(int nodeId, List<String> inputs, List<String> outputs,
                              List<String> inputFluids, List<String> outputFluids) {
        AssemblyGraph.Node node = graph.node(nodeId);
        if (node == null || inputFluids.size() > 32 || outputFluids.size() > 32
            || inputs.size() > 32 || outputs.size() > 32) return;
        node.recipe = "";
        node.inputSockets = Math.max(inputs.size(), inputFluids.size());
        node.inputItems = Arrays.copyOf(inputs.toArray(String[]::new), node.inputSockets);
        node.inputFluids = Arrays.copyOf(inputFluids.toArray(String[]::new), node.inputSockets);
        node.inputSlots = new int[node.inputSockets];
        for (int i = 0; i < node.inputSlots.length; i++) node.inputSlots[i] = i;
        node.outputSockets = Math.max(outputs.size(), outputFluids.size());
        node.outputItems = Arrays.copyOf(outputs.toArray(String[]::new), node.outputSockets);
        node.outputFluids = Arrays.copyOf(outputFluids.toArray(String[]::new), node.outputSockets);
        node.outputSlots = Arrays.copyOf(discoverOutputSlots(machineAt(BlockPos.of(node.machine))), node.outputSockets);
        if (node.outputSlots.length < node.outputSockets) node.outputSlots = Arrays.copyOf(node.outputSlots, node.outputSockets);
        node.outputExport = new boolean[node.outputSockets];
        Arrays.fill(node.outputExport, true);
        graph.edges.removeIf(edge ->
            (edge.to() == nodeId && edge.input() >= node.inputSockets)
                || (edge.from() == nodeId && edge.output() >= node.outputSockets));
        node.status = "Slots configured";
        graph.syncRevision(); setChanged(); sync();
    }
    public void configureGraphSlot(int nodeId, int socket, boolean output, String itemId) {
        AssemblyGraph.Node node = graph.node(nodeId);
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (node == null || id == null || !BuiltInRegistries.ITEM.containsKey(id) || socket < 0) return;
        String[] values = output ? node.outputItems : node.inputItems;
        if (socket >= values.length) return;
        values[socket] = id.toString();
        node.recipe = "";
        node.status = "Slot configured";
        graph.syncRevision(); setChanged(); sync();
    }
    public void configureGraphFluid(int nodeId, int socket, boolean output, String fluidId) {
        AssemblyGraph.Node node = graph.node(nodeId);
        ResourceLocation id = ResourceLocation.tryParse(fluidId);
        if (node == null || id == null || !BuiltInRegistries.FLUID.containsKey(id) || socket < 0) return;
        String[] values = output ? node.outputFluids : node.inputFluids;
        if (socket >= values.length) return;
        values[socket] = id.toString();
        node.recipe = "";
        node.status = "Fluid slot configured";
        graph.syncRevision(); setChanged(); sync();
    }
    private <T> List<BlockPos> shellPorts(Class<T> type) {
        if (bounds == null || level == null) return List.of();
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(bounds.min(), bounds.max())) if (type.isInstance(level.getBlockEntity(p))) result.add(p.immutable());
        return result;
    }
    private List<AssemblyLineMachine> workers() {
        return machines.stream().filter(level::hasChunkAt)
            .map(p -> AssemblyLineMachine.at(level, p, sideProfiles.get(p.asLong())))
            .filter(Objects::nonNull).toList();
    }
    private void startNext() {
        // A failed request is retried only when inventory, structure or the user changes it.
        List<ItemStack> stock = new ArrayList<>(); for (int i = 0; i < 27; i++) stock.add(inventory.getStackInSlot(i));
        ProductionPlan plan = ProductionPlan.build(level, queue.peek(), stock, workers());

        if (!plan.error.isEmpty()) { setStatus(plan.error); return; }
        ItemStackHandler test = copy(reserved);
        for (ItemStack input : plan.rawInputs) if (!put(test, input, 0, test.getSlots()).isEmpty()) { setStatus("Reserved storage full"); return; }
        for (ItemStack input : plan.rawInputs) { take(inventory, input, 0, 27); put(reserved, input, 0, reserved.getSlots()); }
        active = plan; queue.remove(); taskRecipes.clear();
        for (int i = 0; i < active.tasks.size(); i++) taskRecipes.put(i, level.getRecipeManager().byKey(active.tasks.get(i).recipe).orElseThrow());
        setStatus("Running"); setChanged();
    }
    private void schedule() {
        boolean running = false;
        String problem = null;
        for (int id = 0; id < active.tasks.size(); id++) {
            ProductionPlan.Task task = active.tasks.get(id);
            if (task.complete) continue;
            RecipeHolder<?> recipe = taskRecipes.get(id);
            if (recipe == null || !recipeMatches(recipe, task)) { problem = "Invalid or changed recipe: " + task.recipe; continue; }
            if (task.machine != null) {
                BlockPos pos = BlockPos.of(task.machine);
                AssemblyLineMachine worker = level.hasChunkAt(pos) ? AssemblyLineMachine.at(level, pos) : null;
                if (worker == null || !owns(worker, id)) { problem = "Assigned machine missing at " + pos.toShortString() + "; job paused"; continue; }
                boolean consumed = Arrays.stream(worker.inputs()).allMatch(slot -> worker.inventory().getItem(slot).isEmpty());
                ItemStack output = worker.inventory().getItem(1);
                if (consumed && ItemStack.matches(output, task.output)) {
                    if (!put(copy(reserved), output, 0, reserved.getSlots()).isEmpty()) { problem = "Intermediate storage full"; continue; }
                    put(reserved, output.copy(), 0, reserved.getSlots()); worker.inventory().setItem(1, ItemStack.EMPTY);
                    task.complete = true; worker.release(); worker.entity().setChanged(); setChanged();

                } else if (inputsMatch(worker, task)) {
                    feedEnergy(worker); running = true;
                    if (worker.energy().getEnergyStored() < worker.startingEnergy(recipe)) problem = "Missing Energy";
                } else problem = "Assigned machine inventory changed at " + pos.toShortString() + "; job paused";
                continue;
            }
            if (!task.dependencies.stream().allMatch(dep -> active.tasks.get(dep).complete)) continue;
            ItemStackHandler trial = copy(reserved);
            if (!task.inputs.stream().allMatch(input -> take(trial, input, 0, trial.getSlots()))) { problem = "Required reserved material unavailable"; continue; }
            for (AssemblyLineMachine worker : workers()) {
                if (!worker.supports(recipe) || !worker.empty() || worker.entity().getPersistentData().contains(AssemblyLineMachine.OWNER)) continue;
                if (worker.energy() == null) continue;
                feedEnergy(worker);
                if (worker.energy().getMaxEnergyStored() < worker.startingEnergy(recipe)) { problem = "Machine energy capacity too small for recipe"; continue; }
                if (worker.energy().getEnergyStored() < worker.startingEnergy(recipe)) { problem = "Missing Energy"; continue; }
                if (worker.energy().extractEnergy(1, true) == 0) { problem = "Machine energy extraction disabled"; continue; }
                task.machine = worker.entity().getBlockPos().asLong(); worker.assign(worldPosition, id, recipe);
                int[] slots = worker.inputs();
                for (int i = 0; i < slots.length; i++) { take(reserved, task.inputs.get(i), 0, reserved.getSlots()); worker.inventory().setItem(slots[i], task.inputs.get(i).copy()); }
                worker.entity().setChanged(); setChanged(); running = true;

                break;
            }
        }
        if (active.complete()) finish();
        else setStatus(problem != null ? problem : running ? "Running " + active.completed() + "/" + active.tasks.size() : "Waiting for compatible idle machine");
    }
    private boolean owns(AssemblyLineMachine worker, int id) {
        var tag = worker.entity().getPersistentData();
        return tag.contains(AssemblyLineMachine.OWNER) && tag.getLong(AssemblyLineMachine.OWNER) == worldPosition.asLong() && tag.getInt(AssemblyLineMachine.TASK) == id;
    }
    private boolean inputsMatch(AssemblyLineMachine worker, ProductionPlan.Task task) {
        if (!worker.inventory().getItem(1).isEmpty()) return false;
        int[] slots = worker.inputs(); if (slots.length != task.inputs.size()) return false;
        for (int i = 0; i < slots.length; i++) if (!ItemStack.matches(worker.inventory().getItem(slots[i]), task.inputs.get(i))) return false;
        return true;
    }
    private static String[] remove(String[] values, int index) {
        String[] result = new String[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, result.length - index);
        return result;
    }
    private static int[] remove(int[] values, int index) {
        int[] result = new int[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, result.length - index);
        return result;
    }
    private static boolean[] remove(boolean[] values, int index) {
        boolean[] result = new boolean[values.length - 1];
        System.arraycopy(values, 0, result, 0, index);
        System.arraycopy(values, index + 1, result, index, result.length - index);
        return result;
    }
    public boolean mayRun(BlockPos machine, int id) {
        if (!formed() || active == null || id < 0 || id >= active.tasks.size()) return false;
        var task = active.tasks.get(id); var worker = AssemblyLineMachine.at(level, machine);
        return !task.complete && task.machine != null && task.machine == machine.asLong() && worker != null
            && taskRecipes.containsKey(id) && recipeMatches(taskRecipes.get(id), task) && owns(worker, id) && inputsMatch(worker, task)
            && worker.energy().extractEnergy(1, true) > 0
            && worker.energy().getEnergyStored() >= worker.startingEnergy(taskRecipes.get(id));
    }
    private boolean recipeMatches(RecipeHolder<?> recipe, ProductionPlan.Task task) {
        var ingredients = recipe.value().getIngredients();
        if (ingredients.size() != task.inputs.size()) return false;
        for (int i = 0; i < ingredients.size(); i++) if (!ingredients.get(i).test(task.inputs.get(i))) return false;
        ItemStack result = recipe.value().getResultItem(level.registryAccess());
        if (AssemblyLineMachine.kind(recipe) == AssemblyLineMachine.Kind.CRUSHER) result.setCount(Math.min(16, result.getCount()));
        return ItemStack.matches(result, task.output);
    }
    private void feedEnergy(AssemblyLineMachine worker) {
        int offered = Math.min(energy.getEnergyStored(), 1_000_000);
        int received = worker.receiveEnergy(offered, false); energy.extractEnergy(received, false);
    }
    private void finish() {
        ItemStackHandler available = copy(reserved), destination = copy(inventory);
        if (!take(available, active.requested, 0, available.getSlots())) { setStatus("Final product unavailable; job paused"); return; }
        if (!put(destination, active.requested, 27, 36).isEmpty()) { setStatus("Output storage full"); return; }
        for (int i = 0; i < available.getSlots(); i++) if (!put(destination, available.getStackInSlot(i), 0, 27).isEmpty()) { setStatus("Input storage full for surplus; job paused"); return; }
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, destination.getStackInSlot(i));
        for (int i = 0; i < reserved.getSlots(); i++) reserved.setStackInSlot(i, ItemStack.EMPTY);
        completedJobs.addFirst(active.requested.getHoverName().getString() + " x" + active.requested.getCount());
        while (completedJobs.size() > 16) completedJobs.removeLast();

        active = null; taskRecipes.clear(); planDirty = true; setStatus("Production complete"); setChanged(); sync();
    }
    public static ItemStackHandler copy(ItemStackHandler from) {
        ItemStackHandler copy = new ItemStackHandler(from.getSlots());
        for (int i = 0; i < from.getSlots(); i++) copy.setStackInSlot(i, from.getStackInSlot(i).copy()); return copy;
    }
    public static ItemStack put(ItemStackHandler handler, ItemStack stack, int start, int end) {
        ItemStack remaining = stack.copy();
        for (int i = start; i < end && !remaining.isEmpty(); i++) remaining = handler.insertItem(i, remaining, false);
        return remaining;
    }
    public static boolean take(ItemStackHandler handler, ItemStack wanted, int start, int end) {
        int count = wanted.getCount();
        for (int i = start; i < end && count > 0; i++) if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), wanted))
            count -= handler.extractItem(i, count, false).getCount();
        return count == 0;
    }
    public void dropContents() {
        for (ItemStackHandler store : List.of(inventory, reserved)) for (int i = 0; i < store.getSlots(); i++) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, store.getStackInSlot(i));
            store.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
    private void setStatus(String value) { if (!status.equals(value)) { status = value; setChanged(); } }
    private void sync() {
        clientSystemEnergy = systemEnergy();
        clientJobs = (active == null ? "No active job" : "Active: " + active.requested.getHoverName().getString() + " " + active.completed() + "/" + active.tasks.size())
            + "\nQueued: " + queue.size() + "\n" + String.join("\n", queue.stream().map(i -> i.getHoverName().getString() + " x" + i.getCount()).toList())
            + "\nCompleted:\n" + String.join("\n", completedJobs);
        Map<String, int[]> counts = new TreeMap<>();
        for (AssemblyLineMachine w : workers()) { int[] c = counts.computeIfAbsent(w.kind().name(), k -> new int[2]); c[w.entity().getPersistentData().contains(AssemblyLineMachine.OWNER) ? 1 : 0]++; }
        clientMachines = String.join("\n", counts.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()[0] + " idle / " + e.getValue()[1] + " busy").toList());
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.crystalnexus.assembly_line_controller"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new AssemblyLineMenu(id, inv, this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("inventory", inventory.serializeNBT(registries)); tag.put("reserved", reserved.serializeNBT(registries));
        tag.put("fluidBuffer", fluidBuffer.writeToNBT(registries, new CompoundTag()));
        tag.putInt("energy", energy.getEnergyStored()); tag.putBoolean("formed", formed()); tag.putString("status", status);
        if (bounds != null) { tag.putLong("min", bounds.min().asLong()); tag.putLong("max", bounds.max().asLong()); }
        if (active != null) tag.put("active", active.save(registries));
        tag.put("assemblyGraph", graph.save(registries));
        ListTag requests = new ListTag(); queue.forEach(i -> requests.add(ProductionPlan.saveRequest(i, registries))); tag.put("queue", requests);
        ListTag history = new ListTag(); completedJobs.forEach(s -> history.add(StringTag.valueOf(s))); tag.put("history", history);
        if (!configured.isEmpty()) tag.put("configured", ProductionPlan.saveRequest(configured, registries));
        ListTag learned = new ListTag(); learnedRecipes.forEach(id -> learned.add(StringTag.valueOf(id.toString()))); tag.put("learnedRecipes", learned);
        tag.putString("jobsText", clientJobs); tag.putString("machinesText", clientMachines);
        ListTag profiles = new ListTag();
        sideProfiles.forEach((pos, profile) -> {
            CompoundTag p = new CompoundTag(); p.putLong("pos", pos); p.putInt("insert", profile.insertMask());
            p.putInt("extract", profile.extractMask()); p.putInt("fill", profile.fillMask()); p.putInt("drain", profile.drainMask());
            p.putInt("receive", profile.receiveMask()); p.putInt("energyExtract", profile.extractEnergyMask()); profiles.add(p);
        });
        tag.put("sideProfiles", profiles);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); inventory.deserializeNBT(registries, tag.getCompound("inventory")); reserved.deserializeNBT(registries, tag.getCompound("reserved"));
        if (tag.contains("fluidBuffer")) fluidBuffer.readFromNBT(registries, tag.getCompound("fluidBuffer"));
        energy.deserializeNBT(registries, IntTag.valueOf(tag.getInt("energy"))); status = tag.getString("status");
        bounds = tag.contains("min") ? new AssemblyLineStructure.Bounds(BlockPos.of(tag.getLong("min")), BlockPos.of(tag.getLong("max"))) : null;
        active = tag.contains("active") ? ProductionPlan.load(tag.getCompound("active"), registries) : null;
        graph = tag.contains("assemblyGraph") ? AssemblyGraph.load(tag.getCompound("assemblyGraph"), registries) : new AssemblyGraph();
        queue.clear(); for (Tag request : tag.getList("queue", Tag.TAG_COMPOUND)) queue.add(ProductionPlan.loadRequest((CompoundTag) request, registries));
        completedJobs.clear(); for (Tag s : tag.getList("history", Tag.TAG_STRING)) completedJobs.add(s.getAsString());
        configured = ProductionPlan.loadRequest(tag.getCompound("configured"), registries);
        learnedRecipes.clear(); for (Tag id : tag.getList("learnedRecipes", Tag.TAG_STRING)) { ResourceLocation parsed = ResourceLocation.tryParse(id.getAsString()); if (parsed != null) learnedRecipes.add(parsed); }
        clientJobs = tag.getString("jobsText"); clientMachines = tag.getString("machinesText"); formed = false; dirty = true;
        sideProfiles.clear();
        for (Tag raw : tag.getList("sideProfiles", Tag.TAG_COMPOUND)) {
            CompoundTag p = (CompoundTag) raw;
            sideProfiles.put(p.getLong("pos"), new AssemblyLineMachine.SideProfile(p.getInt("insert"), p.getInt("extract"),
                p.getInt("fill"), p.getInt("drain"), p.getInt("receive"), p.getInt("energyExtract")));
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); tag.putString("status", status); tag.putString("jobsText", clientJobs); tag.putString("machinesText", clientMachines);
        tag.putInt("energy", energy.getEnergyStored()); tag.putLong("systemEnergy", systemEnergy()); tag.putBoolean("formed", formed()); tag.put("assemblyGraph", graph.save(registries)); if (!configured.isEmpty()) tag.put("configured", ProductionPlan.saveRequest(configured, registries)); return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        status = tag.getString("status"); clientJobs = tag.getString("jobsText"); clientMachines = tag.getString("machinesText");
        energy.deserializeNBT(registries, IntTag.valueOf(tag.getInt("energy"))); clientSystemEnergy = tag.getLong("systemEnergy"); formed = tag.getBoolean("formed"); dirty = false;
        if (tag.contains("assemblyGraph")) graph = AssemblyGraph.load(tag.getCompound("assemblyGraph"), registries);
        configured = ProductionPlan.loadRequest(tag.getCompound("configured"), registries);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
