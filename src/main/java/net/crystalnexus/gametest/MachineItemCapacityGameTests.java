package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.procedures.CrystalCrusherOnTickUpdateProcedure;
import net.crystalnexus.util.MachineItemStorage;
import net.crystalnexus.world.inventory.CrusherGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

@GameTestHolder("crystalnexus_upgrades")
@PrefixGameTestTemplate(false)
public final class MachineItemCapacityGameTests {
    private MachineItemCapacityGameTests() {}

    @GameTest(template = "zero_point")
    public static void insertionSimulationAndSpecialLimits(GameTestHelper helper) {
        int[] capacities = {64, 64, 128, 256, 256, 512, 512, 1024, 1024};
        for (MachineTier tier : MachineTier.values())
            helper.assertTrue(tier.itemSlotCapacity() == capacities[tier.level()], "Incorrect capacity for " + tier);
        var crusher = crusher(helper);
        IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, crusher.getBlockPos(), Direction.DOWN);
        helper.assertTrue(items != null && items.getSlotLimit(0) == 1024, "Hyper material limit must be 1024");
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 1020));
        ItemStack offered = new ItemStack(Items.COBBLESTONE, 8);
        helper.assertTrue(items.insertItem(0, offered, true).getCount() == 4 && crusher.getItem(0).getCount() == 1020,
            "Simulation must report overflow without modifying storage");
        helper.assertTrue(items.insertItem(0, offered, false).getCount() == 4 && crusher.getItem(0).getCount() == 1024
                && offered.getCount() == 8, "Insertion must fill precisely to capacity and preserve offered items");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1,
            "A full slot must reject overflow");
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 100));
        ItemStack named = new ItemStack(Items.COBBLESTONE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Different components"));
        helper.assertTrue(items.insertItem(0, named, false).getCount() == 1
                && items.insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1,
            "Different items and components must not merge");
        helper.assertTrue(items.extractItem(0, 64, false).isEmpty(), "Automation must preserve input extraction restrictions");
        crusher.setItem(1, new ItemStack(Items.COBBLESTONE, 1024));
        helper.assertTrue(items.extractItem(1, 1000, true).getCount() == 64 && crusher.getItem(1).getCount() == 1024,
            "Simulated extraction must return a legal stack without changes");
        helper.assertTrue(items.extractItem(1, 1000, false).getCount() == 64 && crusher.getItem(1).getCount() == 960,
            "Automation must extract ordinary stacks");
        crusher.setItem(0, ItemStack.EMPTY);
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_PICKAXE, 2), false).getCount() == 1
                && crusher.getItem(0).getCount() == 1, "Unstackable items must remain singletons");
        crusher.setItem(2, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 64));
        helper.assertTrue(crusher.getItem(2).getCount() == 16, "Upgrade stack caps must remain unchanged");
        crusher.setItem(2, ItemStack.EMPTY);
        helper.assertTrue(items.insertItem(2, new ItemStack(CrystalnexusModItems.ZERO_CHIP.get(), 2), false).getCount() == 1
                && crusher.getItem(2).getCount() == 1, "Zero chips must remain singletons");
        crusher.getPersistentData().putString(net.crystalnexus.assembly.AssemblyLineMachine.OWNER, "capacity-test");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_PICKAXE), false).getCount() == 1
                && items.extractItem(1, 64, false).isEmpty(), "Assembly ownership must still lock external access");
        crusher.getPersistentData().remove(net.crystalnexus.assembly.AssemblyLineMachine.OWNER);
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void everyTieredInventorySavesFullCounts(GameTestHelper helper) {
        Block[] blocks = {CrystalnexusModBlocks.IRON_SMELTER.get(), CrystalnexusModBlocks.CRYSTAL_SMELTER.get(),
            CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get(), CrystalnexusModBlocks.INVERTIUM_SMELTER.get(),
            CrystalnexusModBlocks.HYPER_CRUSHER.get(), CrystalnexusModBlocks.HYPER_DUST_SEPARATOR.get(),
            CrystalnexusModBlocks.CIRCUIT_PRESS.get(), CrystalnexusModBlocks.HYPER_CRAFTING_FACTORY.get(),
            CrystalnexusModBlocks.ARC_FURNACE.get(), CrystalnexusModBlocks.TITANIUM_EXTRACTINATOR.get(),
            CrystalnexusModBlocks.MASTICATOR.get(), CrystalnexusModBlocks.HYPER_REFINERY.get()};
        for (Block block : blocks) {
            BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, block);
            var machine = helper.getBlockEntity(pos);
            Container inventory = (Container) machine;
            int capacity = MachineTier.from(machine.getBlockState()).itemSlotCapacity();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++)
                if (MachineItemStorage.isMaterialSlot(inventory, slot)) inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, capacity));
            var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            var restored = machine.getType().create(machine.getBlockPos(), machine.getBlockState());
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            for (int slot = 0; slot < inventory.getContainerSize(); slot++)
                helper.assertTrue(ItemStack.matches(inventory.getItem(slot), ((Container) restored).getItem(slot)),
                    "Full count must survive save/load: " + block + " slot " + slot);
            inventory.clearContent();
        }
        var crusher = crusher(helper);
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        var legacy = crusher.saveWithFullMetadata(helper.getLevel().registryAccess());
        legacy.remove("MachineItemCounts");
        crusher.loadAdditional(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(crusher.getItem(0).getCount() == 64, "Legacy saves must load without count metadata");
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 1024));
        var packet = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(packet, crusher.getItem(0));
            helper.assertTrue(ItemStack.OPTIONAL_STREAM_CODEC.decode(packet).getCount() == 1024,
                "Existing menu packet codec must retain full counts");
        } finally { packet.release(); }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void menuTransfersKeepPlayerStacksLegal(GameTestHelper helper) {
        var crusher = crusher(helper);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new GameProfile(UUID.randomUUID(), "capacity-test"), ClientInformation.createDefault());
        var menu = new CrusherGuiMenu(1, player.getInventory(),
            new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(crusher.getBlockPos()));
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 1024));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 64 && crusher.getItem(0).getCount() == 960, "Pickup must leave overflow in storage");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && crusher.getItem(0).getCount() == 1024, "Click insertion must merge beyond 64");
        menu.clicked(0, 1, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 64 && crusher.getItem(0).getCount() == 960, "Right pickup must remain legal");
        menu.setCarried(new ItemStack(Items.STONE));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.STONE) && crusher.getItem(0).getCount() == 960, "Different-item cursor swaps must retain overflow");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 64 && crusher.getItem(0).getCount() == 896, "Empty hotbar swap must extract one stack");
        player.getInventory().setItem(0, new ItemStack(Items.STONE));
        menu.clicked(0, 0, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(0).is(Items.STONE) && crusher.getItem(0).getCount() == 896, "Occupied hotbar swap must retain overflow");
        menu.clicked(0, 40, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(40).getCount() == 64, "Offhand extraction must remain legal");
        player.getInventory().clearContent();
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 1024));
        menu.quickMoveStack(player, 0);
        int total = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            helper.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "Shift-click must respect player limits");
            total += stack.getCount();
        }
        helper.assertTrue(total == 1024 && crusher.getItem(0).isEmpty(), "Shift-click must distribute every item");
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 1024));
        menu.quickMoveStack(player, 0);
        helper.assertTrue(crusher.getItem(0).getCount() == 1024, "Full player inventory must leave machine unchanged");
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 60));
        menu.quickMoveStack(player, 0);
        helper.assertTrue(crusher.getItem(0).getCount() == 1020 && player.getInventory().getItem(0).getCount() == 64,
            "Partial transfers must retain every remaining item");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void processingAndBreakingPreserveOverflow(GameTestHelper helper) {
        var crusher = crusher(helper);
        ItemStack input = new ItemStack(Items.IRON_ORE, 16);
        ItemStack result = net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input, MachineTier.HYPER);
        helper.assertTrue(!result.isEmpty(), "Test requires an iron crushing recipe");
        crusher.setItem(0, input);
        crusher.setItem(1, result.copyWithCount(1024 - result.getCount()));
        crusher.setItem(2, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 2));
        while (crusher.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        crusher.getPersistentData().putDouble("progress", 100);
        CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), crusher.getBlockPos().getX(), crusher.getBlockPos().getY(), crusher.getBlockPos().getZ());
        helper.assertTrue(crusher.getItem(1).getCount() == 1024 && crusher.getItem(0).getCount() == 15,
            "Parallel processing must stop at capacity: output=" + crusher.getItem(1) + ", input=" + crusher.getItem(0)
                + ", recipe=" + result + ", progress=" + crusher.getPersistentData().getDouble("progress"));
        int energy = crusher.getEnergyStorage().getEnergyStored();
        CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), crusher.getBlockPos().getX(), crusher.getBlockPos().getY(), crusher.getBlockPos().getZ());
        helper.assertTrue(crusher.getItem(0).getCount() == 15 && crusher.getEnergyStorage().getEnergyStored() == energy,
            "Full output must consume neither input nor energy");
        crusher.clearContent();
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 1024));
        AABB area = new AABB(crusher.getBlockPos()).inflate(2);
        helper.getLevel().destroyBlock(crusher.getBlockPos(), false);
        int dropped = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = entity.getItem();
            if (!stack.is(Items.COBBLESTONE)) continue;
            helper.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "Block drops must have legal counts");
            dropped += stack.getCount();
        }
        helper.assertTrue(dropped == 1024, "Breaking must drop all stored items");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void smelterAndFactoryProduceBeyond64(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get().defaultBlockState()
            .setValue(net.crystalnexus.block.ChlorophyteSmelterBlock.BLOCKSTATE, 2));
        net.crystalnexus.block.entity.ChlorophyteSmelterBlockEntity smelter = helper.getBlockEntity(relative);
        smelter.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        smelter.setItem(1, new ItemStack(Items.IRON_INGOT, 127));
        while (smelter.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        smelter.getPersistentData().putDouble("progress", 100);
        net.crystalnexus.procedures.ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(),
            smelter.getBlockPos().getX(), smelter.getBlockPos().getY(), smelter.getBlockPos().getZ());
        helper.assertTrue(smelter.getItem(1).getCount() == 128 && smelter.getItem(0).getCount() == 1,
            "Smelting must reach the tier capacity: output=" + smelter.getItem(1) + ", input=" + smelter.getItem(0)
                + ", energy=" + smelter.getEnergyStorage().getEnergyStored() + ", progress=" + smelter.getPersistentData().getDouble("progress"));
        int energy = smelter.getEnergyStorage().getEnergyStored();
        net.crystalnexus.procedures.ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(),
            smelter.getBlockPos().getX(), smelter.getBlockPos().getY(), smelter.getBlockPos().getZ());
        helper.assertTrue(smelter.getItem(0).getCount() == 1 && smelter.getEnergyStorage().getEnergyStored() == energy,
            "Full smelter output must preserve input and energy");
        smelter.clearContent();
        helper.setBlock(relative, CrystalnexusModBlocks.HYPER_CRAFTING_FACTORY.get());
        net.crystalnexus.block.entity.CraftingFactoryBlockEntity factory = helper.getBlockEntity(relative);
        factory.setItem(0, new ItemStack(Items.OAK_LOG));
        factory.setItem(9, new ItemStack(Items.OAK_PLANKS, 64));
        factory.setItem(10, new ItemStack(Items.OAK_PLANKS));
        while (factory.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        factory.getPersistentData().putInt("progress", 100);
        net.crystalnexus.procedures.AutoCrafterOnTickProcedure.execute(helper.getLevel(),
            factory.getBlockPos().getX(), factory.getBlockPos().getY(), factory.getBlockPos().getZ());
        helper.assertTrue(factory.getItem(9).getCount() == 68 && factory.getItem(0).isEmpty(),
            "Hyper crafting factory output must grow beyond 64");
        factory.setItem(10, new ItemStack(Items.OAK_PLANKS, 1024));
        helper.assertTrue(factory.getItem(10).getCount() == 64, "Filter slots must retain ordinary limits");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void smelterBuffersSupportFullParallelBatches(GameTestHelper helper) {
        Block[] blocks = {CrystalnexusModBlocks.CRYSTAL_SMELTER.get(),
            CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get(), CrystalnexusModBlocks.INVERTIUM_SMELTER.get()};
        for (int variant = 0; variant < blocks.length; variant++) {
            BlockPos relative = new BlockPos(1 + variant * 2, 1, 1);
            var state = blocks[variant].defaultBlockState();
            state = state.setValue((net.minecraft.world.level.block.state.properties.IntegerProperty)
                state.getBlock().getStateDefinition().getProperty("blockstate"), 2);
            helper.setBlock(relative, state);
            var machine = helper.getBlockEntity(relative);
            Container inventory = (Container) machine;
            var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, machine.getBlockPos(), null);
            int crafts = 8 << variant;
            inventory.setItem(2, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 4));
            for (int slot = 3; slot < 3 + variant; slot++)
                inventory.setItem(slot, new ItemStack(CrystalnexusModItems.ZERO_CHIP.get()));
            inventory.setItem(0, new ItemStack(Items.RAW_IRON, crafts));
            while (energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
            int before = energy.getEnergyStored();
            machine.getPersistentData().putDouble("progress", 100);
            tickSmelter(helper, machine, variant);
            helper.assertTrue(inventory.getItem(1).getCount() == crafts && inventory.getItem(0).isEmpty()
                && energy.getEnergyStored() == before - 2048 * crafts,
                "Smelter must support its full " + crafts + "-craft batch and charge each craft");
            while (energy.extractEnergy(Integer.MAX_VALUE, false) > 0) {}
            energy.receiveEnergy(2048, false);
            inventory.setItem(0, new ItemStack(Items.RAW_IRON, crafts));
            inventory.setItem(1, ItemStack.EMPTY);
            machine.getPersistentData().putDouble("progress", 100);
            tickSmelter(helper, machine, variant);
            helper.assertTrue(inventory.getItem(1).getCount() == 1 && inventory.getItem(0).getCount() == crafts - 1
                && energy.getEnergyStored() == 0, "Low energy must still limit the batch");
        }
        helper.succeed();
    }

    private static void tickSmelter(GameTestHelper helper, net.minecraft.world.level.block.entity.BlockEntity machine, int variant) {
        BlockPos pos = machine.getBlockPos();
        switch (variant) {
            case 0 -> net.crystalnexus.procedures.CrystalSmelterOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
            case 1 -> net.crystalnexus.procedures.ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
            default -> net.crystalnexus.procedures.InvertiumSmelterOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @GameTest(template = "zero_point")
    public static void machineInputsReceiveTenTimesWithSafeSimulation(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.INVERTIUM_SMELTER.get());
        var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(relative), null);
        helper.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, true) == 20480 && energy.getEnergyStored() == 0,
            "Simulation must offer 10 times the old smelter input without changing storage");
        helper.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, false) == 20480 && energy.getEnergyStored() == 20480,
            "Smelter must accept 20480 FE per input operation");
        while (energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        helper.assertTrue(energy.getEnergyStored() == energy.getMaxEnergyStored()
            && energy.receiveEnergy(Integer.MAX_VALUE, false) == 0, "Input must respect buffer capacity");
        helper.setBlock(relative, CrystalnexusModBlocks.PARTS_ASSEMBLER.get());
        energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(relative), null);
        helper.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, false) == 10000,
            "Hardcoded machine input must also scale, limited by its 10000 FE buffer");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void hyperMachineInputsDoubleCurrentLimits(GameTestHelper helper) {
        Block[] blocks = {CrystalnexusModBlocks.HYPER_CRUSHER.get(), CrystalnexusModBlocks.HYPER_DUST_SEPARATOR.get(),
            CrystalnexusModBlocks.HYPER_CRAFTING_FACTORY.get(), CrystalnexusModBlocks.HYPER_REFINERY.get(),
            CrystalnexusModBlocks.HYPER_LASER_QUARRY.get()};
        int[] rates = {40960, 40960, 40960, 20480, 20480000};
        for (int index = 0; index < blocks.length; index++) {
            BlockPos relative = new BlockPos(1, 1, 1);
            helper.setBlock(relative, blocks[index]);
            var energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(relative), null);
            int expected = Math.min(rates[index], energy.getMaxEnergyStored());
            helper.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, true) == expected && energy.getEnergyStored() == 0,
                "Hyper input simulation must use twice the current rate for " + blocks[index]);
            helper.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, false) == expected && energy.getEnergyStored() == expected,
                "Hyper machines must receive twice the current rate, bounded by free capacity");
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void hoppersFillBeyond64(GameTestHelper helper) {
        var crusher = crusher(helper);
        crusher.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        BlockPos hopperPos = crusher.getBlockPos().above();
        helper.getLevel().setBlockAndUpdate(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(hopperPos);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        HopperBlockEntity.pushItemsTick(helper.getLevel(), hopperPos, hopper.getBlockState(), hopper);
        helper.assertTrue(crusher.getItem(0).getCount() == 65 && hopper.getItem(0).getCount() == 63,
            "Hoppers must insert past the normal 64-item cap");
        helper.succeed();
    }

    private static CrystalCrusherBlockEntity crusher(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.HYPER_CRUSHER.get().defaultBlockState()
            .setValue(net.crystalnexus.block.CrystalCrusherBlock.BLOCKSTATE, 2));
        return helper.getBlockEntity(pos);
    }
}
