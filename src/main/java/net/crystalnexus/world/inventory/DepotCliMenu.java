package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.DepotCliBlockEntity;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.cli.DepotCliParser;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DepotCliMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 14;
    private final BlockPos blockPos;
    private final ContainerData data;
    private long lastCommandTick = Long.MIN_VALUE;

    public DepotCliMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public DepotCliMenu(int id, Inventory inventory, BlockPos blockPos) {
        this(id, inventory, blockPos, inventory.player instanceof ServerPlayer player
                ? serverData(player, blockPos) : new SimpleContainerData(DATA_COUNT));
    }

    private DepotCliMenu(int id, Inventory inventory, BlockPos blockPos, ContainerData data) {
        super(CrystalnexusModMenus.DEPOT_CLI.get(), id);
        this.blockPos = blockPos.immutable();
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
        // JEI requires a real player inventory range to consider a transfer
        // handler applicable. Keep the slots off-screen because the Depot CLI is
        // command-driven, but expose all inventory stacks rather than one slot.
        for (int index = 0; index < inventory.items.size(); index++) {
            addSlot(new Slot(inventory, index, -9999, -9999));
        }
    }

    private static ContainerData serverData(ServerPlayer player, BlockPos blockPos) {
        return new ContainerData() {
            private DepotSavedData.CraftingJob job() {
                return DepotSavedData.get(player).getCraftingJob();
            }

            @Override
            public int get(int index) {
                DepotSavedData.CraftingJob job = job();
                DepotSavedData.CraftingStep step = job == null ? null : job.currentStep();
                return switch (index) {
                    case 0 -> player.serverLevel().getBlockEntity(blockPos) instanceof DepotCliBlockEntity cli
                            && cli.isConnected(player.serverLevel()) ? 1 : 0;
                    case 1 -> DepotNetwork.craftingProcessorCount(player);
                    case 2 -> job == null ? 0 : job.id();
                    case 3 -> job == null ? 0 : itemId(job.targetId());
                    case 4 -> job == null ? 0 : clamped(job.amount());
                    case 5 -> step == null ? 0 : itemId(step.outputId());
                    case 6 -> step == null ? 0 : clamped(step.outputAmount());
                    case 7 -> job == null ? 0 : job.currentStepIndex();
                    case 8 -> job == null ? 0 : job.steps().size();
                    case 9 -> job == null || job.totalWork() <= 0 ? 0 : (int) Math.min(100,
                            (job.totalWork() - job.remainingWork()) * 100 / job.totalWork());
                    case 10 -> job == null ? 0 : job.currentStepPercent();
                    case 11 -> step != null && step.processing() ? 1 : 0;
                    case 12 -> DepotSavedData.get(player).getCraftingJobs().size();
                    case 13 -> DepotNetwork.craftingJobCapacity(player);
                    default -> 0;
                };
            }

            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    private static int itemId(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? 0 : BuiltInRegistries.ITEM.getId(item);
    }

    private static int clamped(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public DepotCliBlockEntity getTerminal(ServerPlayer player) {
        return player.serverLevel().getBlockEntity(blockPos) instanceof DepotCliBlockEntity cli ? cli : null;
    }

    public boolean hasPermission(ServerPlayer player) {
        DepotCliBlockEntity cli = getTerminal(player);
        return cli != null && cli.canUse(player);
    }

    public boolean isConnected(ServerPlayer player) {
        DepotCliBlockEntity cli = getTerminal(player);
        return cli != null && cli.canUse(player) && cli.isConnected(player.serverLevel());
    }

    public boolean isConnected() { return data.get(0) != 0; }
    public int getProcessorCount() { return data.get(1); }
    public boolean hasJob() { return data.get(2) > 0; }
    public int getJobId() { return data.get(2); }
    public ItemStack getJobTarget() { return hasJob() ? new ItemStack(BuiltInRegistries.ITEM.byId(data.get(3))) : ItemStack.EMPTY; }
    public int getJobAmount() { return data.get(4); }
    public ItemStack getCurrentStep() { return hasJob() ? new ItemStack(BuiltInRegistries.ITEM.byId(data.get(5))) : ItemStack.EMPTY; }
    public int getCurrentStepAmount() { return data.get(6); }
    public int getStepIndex() { return data.get(7); }
    public int getStepCount() { return data.get(8); }
    public int getJobPercent() { return data.get(9); }
    public int getStepPercent() { return data.get(10); }
    public boolean isProcessing() { return data.get(11) != 0; }
    public int getJobCount() { return data.get(12); }
    public int getJobCapacity() { return data.get(13); }

    public boolean allowCommand(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        if (!DepotCliParser.mayExecute(lastCommandTick, now)) return false;
        lastCommandTick = now;
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return true;
        return level.getBlockEntity(blockPos) instanceof DepotCliBlockEntity cli
                && cli.canUse(player)
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
