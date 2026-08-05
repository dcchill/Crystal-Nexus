package net.crystalnexus.world.inventory;

import net.crystalnexus.block.CraftingUpgradeBlock;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class CraftingProcessorMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 10;
    private final BlockPos blockPos;
    private final ContainerData data;

    public CraftingProcessorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public CraftingProcessorMenu(int id, Inventory inventory, BlockPos blockPos) {
        this(id, inventory, blockPos, inventory.player.level() instanceof ServerLevel level
                ? serverData(level, blockPos) : new SimpleContainerData(DATA_COUNT));
    }

    private CraftingProcessorMenu(int id, Inventory inventory, BlockPos blockPos, ContainerData data) {
        super(CrystalnexusModMenus.CRAFTING_PROCESSOR.get(), id);
        this.blockPos = blockPos.immutable();
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    private static ContainerData serverData(ServerLevel level, BlockPos pos) {
        UUID owner = DepotNetwork.craftingProcessorOwner(level, pos);
        return new ContainerData() {
            private DepotSavedData.CraftingJob job() {
                return owner == null ? null : DepotSavedData.get(level, owner).getCraftingJob();
            }

            @Override
            public int get(int index) {
                DepotSavedData.CraftingJob job = job();
                if (job == null) return 0;
                DepotSavedData.CraftingStep step = job.currentStep();
                return switch (index) {
                    case 0 -> job.id();
                    case 1 -> itemId(job.targetId());
                    case 2 -> job.amount();
                    case 3 -> itemId(step == null ? job.targetId() : step.outputId());
                    case 4 -> clamped(step == null ? job.amount() : step.outputAmount());
                    case 5 -> job.currentStepIndex();
                    case 6 -> job.steps().size();
                    case 7 -> (int) Math.min(100, 100.0 * (job.totalWork() - job.remainingWork())
                            / Math.max(1, job.totalWork()));
                    case 8 -> job.currentStepPercent();
                    case 9 -> step != null && step.processing() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static int itemId(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? 0 : BuiltInRegistries.ITEM.getId(item);
    }

    private static int clamped(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    public boolean hasJob() {
        return data.get(0) > 0;
    }

    public int getJobId() {
        return data.get(0);
    }

    public ItemStack getTargetStack() {
        return hasJob() ? new ItemStack(BuiltInRegistries.ITEM.byId(data.get(1))) : ItemStack.EMPTY;
    }

    public int getTargetAmount() {
        return data.get(2);
    }

    public ItemStack getStepStack() {
        return hasJob() ? new ItemStack(BuiltInRegistries.ITEM.byId(data.get(3))) : ItemStack.EMPTY;
    }

    public int getStepAmount() {
        return data.get(4);
    }

    public int getStepIndex() {
        return data.get(5);
    }

    public int getStepCount() {
        return data.get(6);
    }

    public int getOverallPercent() {
        return data.get(7);
    }

    public int getStepPercent() {
        return data.get(8);
    }

    public boolean isProcessing() {
        return data.get(9) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return true;
        UUID owner = DepotNetwork.craftingProcessorOwner(level, blockPos);
        return level.getBlockState(blockPos).getBlock() instanceof CraftingUpgradeBlock
                && player.getUUID().equals(owner)
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
