package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.crystalnexus.block.entity.*;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.crystalnexus.procedures.*;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.world.inventory.CrusherGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_zero")
@PrefixGameTestTemplate(false)
public final class ZeroUpgradeGameTests {
    private static ItemStack chip(int count) { return new ItemStack(CrystalnexusModItems.ZERO_CHIP.get(), count); }

    @GameTest(template = "zero_point")
    public static void inventoryStacksAndInstalledUpgradeLimits(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        CrystalCrusherBlockEntity machine = helper.getBlockEntity(pos);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "upgrade-limits"), ClientInformation.createDefault());
        var menu = new CrusherGuiMenu(1, player.getInventory(), new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(machine.getBlockPos()));
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item, 64);
            if (!MachineUpgradeHelper.isMachineUpgrade(stack)) continue;
            helper.assertTrue(stack.getMaxStackSize() == 64, "All upgrade inventory stacks must hold 64: " + item);
            int limit = MachineUpgradeHelper.upgradeStackLimit(stack);
            helper.assertTrue(menu.slots.get(2).getMaxStackSize(stack) == limit, "GUI must advertise the installed limit: " + item);
            player.getInventory().setItem(9, stack.copy());
            menu.quickMoveStack(player, menu.slots.size() - 36);
            helper.assertTrue(machine.getItem(2).getCount() == limit && player.getInventory().getItem(9).getCount() == 64 - limit, "Shift-click must leave excess upgrades in the inventory: " + item);
            machine.setItem(2, ItemStack.EMPTY);
            for (Direction side : new Direction[]{null, Direction.UP}) {
                var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), side);
                helper.assertTrue(items.insertItem(2, stack, true).getCount() == 64 - limit && machine.getItem(2).isEmpty(), "Simulation must enforce the installed limit without changing inventory");
                helper.assertTrue(items.insertItem(2, stack, false).getCount() == 64 - limit && machine.getItem(2).getCount() == limit, "Automation must enforce the installed limit");
                helper.assertTrue(items.getSlotLimit(2) == limit && items.insertItem(2, stack, false).getCount() == 64, "Full upgrade slots must reject excess items");
                if (limit > 1) {
                    machine.setItem(2, stack.copyWithCount(limit - 1));
                    helper.assertTrue(items.insertItem(2, stack, true).getCount() == 63 && machine.getItem(2).getCount() == limit - 1, "Simulated merges must respect the remaining space");
                    helper.assertTrue(items.insertItem(2, stack, false).getCount() == 63 && machine.getItem(2).getCount() == limit, "Merges must stop at the installed limit");
                }
                machine.setItem(2, ItemStack.EMPTY);
            }
        }
        // Upgrade items used as recipe ingredients must keep their ordinary 64-item input stacks.
        var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), Direction.UP);
        helper.assertTrue(items.insertItem(0, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 64), false).isEmpty() && machine.getItem(0).getCount() == 64, "Recipe input slots must not inherit upgrade limits");
        BlockPos legacyPos = new BlockPos(3, 1, 1);
        helper.setBlock(legacyPos, CrystalnexusModBlocks.CRYSTAL_PURIFIER.get());
        CrystalPurifierBlockEntity legacy = helper.getBlockEntity(legacyPos);
        var legacyMenu = new net.crystalnexus.world.inventory.CrystalPurifierGUIMenu(1, player.getInventory(), new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(legacy.getBlockPos()));
        player.getInventory().setItem(9, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 64));
        legacyMenu.quickMoveStack(player, legacyMenu.slots.size() - 36);
        helper.assertTrue(legacy.getItem(3).getCount() == 16 && player.getInventory().getItem(9).getCount() == 48, "Legacy upgrade GUI slots must preserve their 16-item limit");
        BlockPos reactorPos = new BlockPos(5, 1, 1);
        helper.setBlock(reactorPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
        var reactor = helper.getLevel().getBlockEntity(helper.absolutePos(reactorPos));
        var reactorItems = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, reactor.getBlockPos(), Direction.UP);
        var reactorMenu = new net.crystalnexus.world.inventory.ReactorGUIMenu(1, player.getInventory(), new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(reactor.getBlockPos()).writeVarInt(0));
        for (var item : new net.minecraft.world.item.Item[]{CrystalnexusModItems.REACTOR_UPGRADE.get(), CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get()}) {
            ItemStack stack = new ItemStack(item, 64);
            helper.assertTrue(stack.getMaxStackSize() == 64 && reactorMenu.slots.get(0).getMaxStackSize(stack) == 1, "Reactor upgrades must stack to 64 in inventory and one in the reactor slot");
            helper.assertTrue(reactorItems.insertItem(1, stack, false).getCount() == 63, "Automation must install one reactor upgrade");
            ((net.minecraft.world.Container) reactor).setItem(1, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void specialMachineUpgradeLayouts(GameTestHelper helper) {
        net.minecraft.world.level.block.Block[] blocks = {
            CrystalnexusModBlocks.MEGA_CHEMICAL_REACTION_CHAMBER.get(),
            CrystalnexusModBlocks.CRYOGENIC_FLASH_FREEZER_HATCH.get(),
            CrystalnexusModBlocks.MATTER_TRANSMUTATION_TABLE.get(),
            CrystalnexusModBlocks.PARTICLE_ACCELERATOR_CONTROLLER.get(),
            CrystalnexusModBlocks.FLUID_PACKAGER.get()
        };
        int[] first = {3, 2, 9, 8, 2}, count = {3, 3, 4, 4, 1};
        for (int m = 0; m < blocks.length; m++) {
            BlockPos pos = new BlockPos(1 + m * 2, 1, 1);
            helper.setBlock(pos, blocks[m]);
            var machine = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            var inventory = (net.minecraft.world.Container) machine;
            helper.assertTrue(inventory.getContainerSize() == first[m] + count[m], "Inventory must contain every upgrade slot: " + blocks[m]);
            ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "upgrade-layout"), ClientInformation.createDefault());
            var data = new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(machine.getBlockPos());
            net.minecraft.world.inventory.AbstractContainerMenu menu = switch (m) {
                case 0 -> new net.crystalnexus.world.inventory.FluidChemicalReactionChamberGUIMenu(1, player.getInventory(), data);
                case 1 -> new net.crystalnexus.world.inventory.CryogenicFlashFreezerMenu(1, player.getInventory(), data);
                case 2 -> new net.crystalnexus.world.inventory.MatterTransmutationGUIMenu(1, player.getInventory(), data);
                case 3 -> new net.crystalnexus.world.inventory.AcceleratorGuiMenu(1, player.getInventory(), data);
                default -> new net.crystalnexus.world.inventory.FluidPackagerGUIMenu(1, player.getInventory(), data);
            };
            helper.assertTrue(menu.slots.size() == inventory.getContainerSize() + 36, "GUI must expose the expanded inventory");
            boolean rejectsZero = m == 2 || m == 4;
            for (int i = 0; i < count[m]; i++) {
                helper.assertTrue(menu.slots.get(first[m] + i).isActive(), "All upgrade slots must be active");
                helper.assertTrue(menu.slots.get(first[m] + i).mayPlace(chip(1)) != rejectsZero, "GUI must enforce Zero Chip restrictions");
                helper.assertTrue(inventory.canPlaceItem(first[m] + i, chip(1)) != rejectsZero, "Inventory must enforce Zero Chip restrictions");
            }
            player.getInventory().setItem(9, chip(16));
            menu.quickMoveStack(player, inventory.getContainerSize());
            helper.assertTrue(player.getInventory().getItem(9).getCount() == (rejectsZero ? 16 : 16 - count[m]), "Shift-click must spread chips or reject them");
            if (!rejectsZero) {
                helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(MachineUpgradeHelper.upgrades(machine, first[m], first[m] + 1)) == 1 << count[m], "Every new slot must multiply throughput");
                var restored = ((net.minecraft.world.level.block.EntityBlock) blocks[m]).newBlockEntity(machine.getBlockPos(), machine.getBlockState());
                restored.loadWithComponents(machine.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
                helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(MachineUpgradeHelper.upgrades(restored, first[m], first[m] + 1)) == 1 << count[m], "New slots must survive saving");
                inventory.clearContent();
            }
            for (Direction side : new Direction[]{null, Direction.UP}) {
                var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), side);
                for (int i = items.getSlots() - count[m]; i < items.getSlots(); i++) {
                    helper.assertTrue(items.insertItem(i, chip(16), false).getCount() == (rejectsZero ? 16 : 15), "Automation must enforce chip limits in all slots");
                }
                inventory.clearContent();
            }
            if (m == 2) {
                for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                    ItemStack upgrade = new ItemStack(item);
                    if (!MachineUpgradeHelper.isMachineUpgrade(upgrade)) continue;
                    for (int i = 9; i < 13; i++)
                        helper.assertTrue(menu.slots.get(i).mayPlace(upgrade) == !MachineUpgradeHelper.isZeroChip(upgrade), "Table must accept every other upgrade");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void oneChipPerSlotAndMultiplicativeThroughput(GameTestHelper helper) {
        helper.assertTrue(chip(1).getMaxStackSize() == 64, "Inventory stacks must hold 64 chips");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.HYPER_CRUSHER.get());
        CrystalCrusherBlockEntity machine = helper.getBlockEntity(pos);
        for (Direction side : new Direction[]{null, Direction.UP}) {
            var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), side);
            helper.assertTrue(items.insertItem(2, chip(16), true).getCount() == 15 && machine.getItem(2).isEmpty(), "Simulation must reserve only one chip");
            helper.assertTrue(items.insertItem(2, chip(16), false).getCount() == 15 && machine.getItem(2).getCount() == 1, "Automation must install one chip");
            helper.assertTrue(items.insertItem(2, chip(16), false).getCount() == 16 && !machine.canPlaceItem(2, chip(1)), "Occupied upgrade slots must reject more chips");
            machine.setItem(2, ItemStack.EMPTY);
        }
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "zero-chip-test"), ClientInformation.createDefault());
        player.getInventory().setItem(9, chip(16));
        CrusherGuiMenu menu = new CrusherGuiMenu(1, player.getInventory(), new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(machine.getBlockPos()));
        helper.assertTrue(menu.slots.get(2).getMaxStackSize(chip(16)) == 1, "GUI must cap chips at one");
        menu.quickMoveStack(player, menu.slots.size() - 36);
        for (int slot = 2; slot < 7; slot++) helper.assertTrue(machine.getItem(slot).getCount() == 1, "Shift-click must spread chips across slots");
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 11, "Uninstalled chips must remain in the inventory");
        var upgrades = MachineUpgradeHelper.upgrades(machine, 2, 3);
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(upgrades) == 32, "Five occupied chip slots must allow 32 normal crafts");
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(List.of(chip(16))) == 2, "Chip counts within one slot must not multiply throughput");
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(List.of(ItemStack.EMPTY)) == 1, "No chip must preserve the normal batch");
        var parallel = new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 3);
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(List.of(parallel, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get()))) == 8, "Parallel chips must add across slots");
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(List.of(parallel, chip(1), chip(1))) == 24, "Zero chips must multiply the additive parallel batch");
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(java.util.Collections.nCopies(32, chip(1))) == Integer.MAX_VALUE, "Throughput must saturate without overflow");
        var restored = new CrystalCrusherBlockEntity(machine.getBlockPos(), machine.getBlockState());
        restored.loadWithComponents(machine.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(MachineUpgradeHelper.upgrades(restored, 2, 3)) == 32, "Chip slots must survive saving");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void smeltingBatchesConsumeInputsEnergyAndRespectSpace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get().defaultBlockState().setValue(net.crystalnexus.block.ChlorophyteSmelterBlock.BLOCKSTATE, 2));
        ChlorophyteSmelterBlockEntity machine = helper.getBlockEntity(pos);
        machine.setItem(0, new ItemStack(Items.IRON_ORE, 8));
        machine.setItem(1, new ItemStack(Items.IRON_INGOT, 64));
        machine.setItem(2, chip(1)); machine.setItem(3, chip(1));
        while (machine.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        int energy = machine.getEnergyStorage().getEnergyStored();
        machine.getPersistentData().putDouble("progress", 1000);
        ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
        helper.assertTrue(machine.getItem(0).getCount() == 8 && machine.getItem(1).getCount() == 64 && machine.getEnergyStorage().getEnergyStored() == energy, "Full outputs must preserve inputs and energy");
        machine.setItem(1, new ItemStack(Items.IRON_INGOT, 60));
        ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
        int perCraft = Math.min(MachineUpgradeHelper.energyCost(machine.getBlockState(), MachineUpgradeHelper.upgrades(machine, 2, 3), 2048), net.crystalnexus.config.CrystalnexusConfig.MACHINES.CHLOROPHYTE_SMELTER.maxExtract());
        helper.assertTrue(machine.getItem(0).getCount() == 4 && machine.getItem(1).getCount() == 64 && energy - machine.getEnergyStorage().getEnergyStored() == 4 * perCraft, "Four crafts must consume four inputs and four normal energy costs");
        machine.setItem(1, new ItemStack(Items.IRON_INGOT, 63));
        machine.getPersistentData().putDouble("progress", 1000);
        ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
        helper.assertTrue(machine.getItem(0).getCount() == 3 && machine.getItem(1).getCount() == 64, "Limited output room must allow only one craft");
        machine.setItem(1, ItemStack.EMPTY);
        while (machine.getEnergyStorage().extractEnergy(Integer.MAX_VALUE, false) > 0) {}
        machine.getEnergyStorage().receiveEnergy(perCraft, false);
        machine.getPersistentData().putDouble("progress", 1000);
        ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
        helper.assertTrue(machine.getItem(0).getCount() == 2 && machine.getItem(1).getCount() == 1 && machine.getEnergyStorage().getEnergyStored() == 0, "One craft's energy must never fund the whole batch");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void fluidBatchesConsumeEveryInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get());
        FluidChemicalReactionChamberBlockEntity machine = helper.getBlockEntity(pos);
        var manager = helper.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var recipe = new FluidChemicalReactionRecipe(Optional.of(new FluidChemicalReactionRecipe.FluidAmount(ResourceLocation.parse("minecraft:water"), 100, Optional.empty(), false)), Optional.empty(), Optional.of(Ingredient.of(Items.COBBLESTONE)), Optional.empty(), Optional.of(new FluidChemicalReactionRecipe.FluidAmount(ResourceLocation.parse("minecraft:lava"), 250, Optional.empty(), false)), Optional.of(new ItemStack(Items.STONE)), Optional.empty());
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.parse("crystalnexus:zero_test"), recipe)));
            machine.setItem(0, new ItemStack(Items.COBBLESTONE, 2)); machine.setItem(3, chip(1));
            machine.getTank(0).fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE);
            machine.getTank(2).setFluid(new FluidStack(Fluids.LAVA, machine.getTank(2).getCapacity() - 249));
            while (machine.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
            int energy = machine.getEnergyStorage().getEnergyStored();
            machine.getPersistentData().putDouble("progress", 1000);
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos());
            helper.assertTrue(machine.getItem(0).getCount() == 2 && machine.getTank(0).getFluidAmount() == 200 && machine.getEnergyStorage().getEnergyStored() == energy, "Blocked fluid output must consume nothing");
            machine.getTank(2).setFluid(FluidStack.EMPTY);
            helper.getLevel().setBlock(machine.getBlockPos(), machine.getBlockState().setValue(net.crystalnexus.block.ChemicalReactionChamberBlock.BLOCKSTATE, 2), 3);
            machine.getPersistentData().putDouble("progress", 1000);
            FluidChemicalReactionChamberOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos());
            helper.assertTrue(machine.getItem(0).isEmpty() && machine.getTank(0).isEmpty() && machine.getItem(2).getCount() == 2 && machine.getTank(2).getFluidAmount() == 500 && energy - machine.getEnergyStorage().getEnergyStored() == 8192, "Two reactions must consume twice the item, fluid, and energy inputs");
        } finally { manager.replaceRecipes(original); }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void extractinatorBatchesNormalLoot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.EXTRACTINATOR.get().defaultBlockState().setValue(net.crystalnexus.block.ExtractinatorBlock.BLOCKSTATE, 2));
        ExtractinatorBlockEntity machine = helper.getBlockEntity(pos);
        machine.setItem(0, new ItemStack(Items.COBBLESTONE, 2)); machine.setItem(7, chip(1));
        for (int slot = 1; slot <= 6; slot++) machine.setItem(slot, new ItemStack(Items.IRON_NUGGET, 64));
        while (machine.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        machine.getPersistentData().putDouble("progress", 1000);
        ExtractinatorOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
        helper.assertTrue(machine.getItem(0).getCount() == 2 && machine.getItem(1).getCount() == 64, "Blocked loot batch must preserve inputs");
        for (int slot = 1; slot <= 6; slot++) machine.setItem(slot, ItemStack.EMPTY);
        ExtractinatorOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
        int nuggets = 0;
        for (int slot = 1; slot <= 6; slot++) if (machine.getItem(slot).is(Items.IRON_NUGGET)) nuggets += machine.getItem(slot).getCount();
        helper.assertTrue(machine.getItem(0).isEmpty() && nuggets == 2, "Two consumed inputs must yield two normal loot rolls");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void steamOutputKeepsNormalYield(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.STEAM_CHAMBER.get());
        helper.setBlock(pos.above(), CrystalnexusModBlocks.STEAM_COLLECTOR.get());
        SteamChamberBlockEntity chamber = helper.getBlockEntity(pos);
        chamber.setItem(3, chip(1)); chamber.getPersistentData().putBoolean("running", true);
        SteamCollectorBlockEntity collector = helper.getBlockEntity(pos.above());
        BlockPos absolute = collector.getBlockPos();
        SteamCollectionProcedure.execute(helper.getLevel(), absolute.getX(), absolute.getY(), absolute.getZ());
        helper.assertTrue(collector.getFluidTank().getFluidAmount() == 25, "Zero Chips must not increase steam yield");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void craftingFactoryRejectsAndIgnoresZeroChips(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CRYSTAL_CRAFTING_FACTORY.get());
        CraftingFactoryBlockEntity factory = helper.getBlockEntity(pos);
        var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, factory.getBlockPos(), null);
        helper.assertTrue(!factory.canPlaceItem(11, chip(1)) && items.insertItem(11, chip(16), false).getCount() == 16,
            "Crafting Factory upgrade slots must reject Zero Chips");
        var slot = new net.crystalnexus.world.inventory.MachineUpgradeSlot(new net.neoforged.neoforge.items.wrapper.InvWrapper(factory), 11, factory.getBlockState(), 0);
        helper.assertTrue(!slot.mayPlace(chip(1)), "GUI upgrade slots must reject Zero Chips");
        // Simulate a chip installed before this restriction, preserving its removal path.
        factory.setItem(11, chip(1));
        factory.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        factory.setItem(10, new ItemStack(Items.STICK));
        while (factory.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        for (int tick = 0; tick < 50; tick++)
            AutoCrafterOnTickProcedure.execute(helper.getLevel(), factory.getBlockPos().getX(), factory.getBlockPos().getY(), factory.getBlockPos().getZ());
        helper.assertTrue(factory.getItem(9).is(Items.STICK) && factory.getItem(9).getCount() == 4 && factory.getItem(0).isEmpty()
                && factory.getItem(11).getCount() == 1, "Previously installed Zero Chips must not multiply factory output");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void energyExtractionConsumesEachInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.ENERGY_EXTRACTOR.get().defaultBlockState().setValue(net.crystalnexus.block.EnergyExtractorBlock.BLOCKSTATE, 2));
        EnergyExtractorBlockEntity machine = helper.getBlockEntity(pos);
        var manager = helper.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var recipe = new net.crystalnexus.jei_recipes.EnergyExtractionRecipe(new ItemStack(Items.DIAMOND), net.minecraft.core.NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.COBBLESTONE)));
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.parse("crystalnexus:zero_energy_test"), recipe)));
            machine.setItem(0, new ItemStack(Items.COBBLESTONE, 2)); machine.setItem(1, chip(1));
            machine.getPersistentData().putDouble("progress", 1000);
            EnergyExtractorOnTickUpdateProcedure.execute(helper.getLevel(), machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
            helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(2).getCount() == 2 && machine.getEnergyStorage().getEnergyStored() == 2 * net.crystalnexus.util.EeMatterEconomy.EXTRACTION_FE_PER_ITEM, "Two actual inputs must yield two normal outputs and normal FE per input");
        } finally { manager.replaceRecipes(original); }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void crushingSmeltingLoopCannotMultiplyResources(GameTestHelper helper) {
        BlockPos crusherPos = new BlockPos(1, 1, 1), smelterPos = new BlockPos(3, 1, 1);
        helper.setBlock(crusherPos, CrystalnexusModBlocks.HYPER_CRUSHER.get().defaultBlockState().setValue(net.crystalnexus.block.CrystalCrusherBlock.BLOCKSTATE, 2));
        helper.setBlock(smelterPos, CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get().defaultBlockState().setValue(net.crystalnexus.block.ChlorophyteSmelterBlock.BLOCKSTATE, 2));
        CrystalCrusherBlockEntity crusher = helper.getBlockEntity(crusherPos);
        ChlorophyteSmelterBlockEntity smelter = helper.getBlockEntity(smelterPos);
        crusher.setItem(2, chip(1)); crusher.setItem(3, chip(1));
        smelter.setItem(2, chip(1)); smelter.setItem(3, chip(1));
        while (crusher.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        while (smelter.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        for (int iteration = 0; iteration < 5; iteration++) {
            crusher.setItem(0, new ItemStack(CrystalnexusModItems.INVERTIUM_INGOT.get()));
            crusher.getPersistentData().putDouble("progress", 1000);
            CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), crusher.getBlockPos().getX(), crusher.getBlockPos().getY(), crusher.getBlockPos().getZ());
            helper.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(1).is(CrystalnexusModItems.INVERTIUM_DUST.get()) && crusher.getItem(1).getCount() == 1, "One ingot must produce only one dust regardless of throughput");
            smelter.setItem(0, crusher.removeItemNoUpdate(1));
            smelter.getPersistentData().putDouble("progress", 1000);
            ChlorophyteSmelterOnTickUpdateProcedure.execute(helper.getLevel(), smelter.getBlockPos().getX(), smelter.getBlockPos().getY(), smelter.getBlockPos().getZ());
            helper.assertTrue(smelter.getItem(0).isEmpty() && smelter.getItem(1).is(CrystalnexusModItems.INVERTIUM_INGOT.get()) && smelter.getItem(1).getCount() == 1, "Repeated loops must never grow the resource count");
            smelter.removeItemNoUpdate(1);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void miningThroughputPaysForEveryOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), CrystalnexusModBlocks.IRON_NODE.get());
        helper.setBlock(pos, CrystalnexusModBlocks.NODE_MINER.get());
        NodeMinerBlockEntity miner = helper.getBlockEntity(pos);
        miner.setItem(3, chip(1));
        while (miner.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        int energy = miner.getEnergyStorage().getEnergyStored();
        miner.getPersistentData().putDouble("progress", 248);
        NodeMinerOnTickUpdateProcedure.execute(helper.getLevel(), miner.getBlockPos().getX(), miner.getBlockPos().getY(), miner.getBlockPos().getZ());
        int outputs = 0;
        for (int i = 0; i < 3; i++) outputs += miner.getItem(i).getCount();
        helper.assertTrue(outputs == 1 && energy - miner.getEnergyStorage().getEnergyStored() == 1024, "Two work steps must complete one normal mining cycle with its normal energy cost");
        for (int i = 0; i < 3; i++) miner.setItem(i, new ItemStack(Items.RAW_IRON, 64));
        miner.getPersistentData().putDouble("progress", 1000);
        energy = miner.getEnergyStorage().getEnergyStored();
        NodeMinerOnTickUpdateProcedure.execute(helper.getLevel(), miner.getBlockPos().getX(), miner.getBlockPos().getY(), miner.getBlockPos().getZ());
        helper.assertTrue(miner.getEnergyStorage().getEnergyStored() == energy, "Blocked mining outputs must not consume batch energy");
        BlockPos quantumPos = new BlockPos(3, 2, 1);
        helper.setBlock(quantumPos, CrystalnexusModBlocks.QUANTUM_MINER.get().defaultBlockState().setValue(net.crystalnexus.block.QuantumMinerBlock.BLOCKSTATE, 2));
        QuantumMinerBlockEntity quantum = helper.getBlockEntity(quantumPos);
        quantum.setItem(9, chip(1));
        while (quantum.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        energy = quantum.getEnergyStorage().getEnergyStored();
        quantum.getPersistentData().putDouble("progress", 1000);
        QuantumMinerOnTickUpdateProcedure.execute(helper.getLevel(), quantum.getBlockPos().getX(), quantum.getBlockPos().getY(), quantum.getBlockPos().getZ());
        outputs = 0;
        for (int i = 0; i < 9; i++) outputs += quantum.getItem(i).getCount();
        helper.assertTrue(outputs == 1 && energy - quantum.getEnergyStorage().getEnergyStored() == 40960, "Quantum mining must pay its full per-cycle energy cost even above the extraction limit");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void zeroCableSharesAboveHyperLimit(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 4, 2);
        helper.setBlock(pos, CrystalnexusModBlocks.ZERO_CABLE.get());
        helper.setBlock(pos.east(), CrystalnexusModBlocks.ZERO_CABLE.get());
        helper.setBlock(pos.north(), CrystalnexusModBlocks.TESSERACT.get());
        helper.setBlock(pos.east().east(), CrystalnexusModBlocks.TESSERACT.get());
        var level = helper.getLevel();
        var cable = level.getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.WEST);
        var near = level.getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos.north()), null);
        var far = level.getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos.east().east()), null);
        helper.assertTrue(cable.receiveEnergy(4_000_000, true) == 4_000_000 && near.getEnergyStored() == 0, "Simulation must preserve unlimited cable throughput");
        helper.assertTrue(cable.receiveEnergy(4_000_000, false) == 4_000_000 && near.getEnergyStored() == 2_000_000 && far.getEnergyStored() == 2_000_000 && cable.getEnergyStored() == 0, "Zero cables must divide transfers above the Hyper limit evenly");
        helper.setBlock(pos.east(), CrystalnexusModBlocks.BASIC_ENERGY_CABLE.get());
        helper.assertTrue(cable.receiveEnergy(Integer.MAX_VALUE, true) == 2048, "Mixed networks must preserve slower cable limits");
        helper.succeed();
    }
}
