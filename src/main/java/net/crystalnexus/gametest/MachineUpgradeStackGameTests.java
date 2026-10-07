package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.world.inventory.CrusherGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.UUID;

@GameTestHolder("crystalnexus_upgrades")
@PrefixGameTestTemplate(false)
public final class MachineUpgradeStackGameTests {
    private MachineUpgradeStackGameTests() {}

    @GameTest(template = "zero_point")
    public static void stackSizeAndMachineSlotInsertion(GameTestHelper helper) {
        for (Item item : new Item[] {
                CrystalnexusModItems.ACCELERATION_UPGRADE.get(), CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get(),
                CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get(),
                CrystalnexusModItems.RANGE_UPGRADE.get(), CrystalnexusModItems.CARBON_RANGE_UPGRADE.get() })
            helper.assertTrue(new ItemStack(item).getMaxStackSize() == 64, "Machine upgrade inventory stack must hold 64: " + item);
        helper.assertTrue(new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get()).getMaxStackSize() == 64,
                "Parallelization chip inventory stacks must hold 64");
        helper.assertTrue(MachineUpgradeHelper.parallelCraftCount(new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get())) == 2
                && MachineUpgradeHelper.parallelCraftCount(new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 4)) == 8,
                "Each parallelization chip must add two crafts");

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        CrystalCrusherBlockEntity crusher = helper.getBlockEntity(pos);
        InvWrapper automation = new InvWrapper(crusher);
        helper.assertTrue(automation.insertItem(2, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 16), false).isEmpty()
                && crusher.getItem(2).getCount() == 16, "Automation must insert all 16 upgrades");
        crusher.setItem(2, ItemStack.EMPTY);
        helper.assertTrue(automation.insertItem(2, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 4), false).isEmpty()
                && crusher.getItem(2).getCount() == 4, "Recipe machines must accept parallelization chips");

        crusher.setItem(2, ItemStack.EMPTY);

        BlockPos smelterPos = new BlockPos(3, 1, 1);
        helper.setBlock(smelterPos, CrystalnexusModBlocks.CRYSTAL_SMELTER.get());
        var smelter = new net.neoforged.neoforge.items.wrapper.InvWrapper(helper.getBlockEntity(smelterPos));
        helper.assertTrue(smelter.insertItem(2, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 4), false).isEmpty()
                && smelter.getStackInSlot(2).getCount() == 4, "Compatible smelters must accept all 4 parallelization chips");

        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "upgrade-stack-test"), ClientInformation.createDefault());
        player.getInventory().setItem(9, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 16));
        CrusherGuiMenu menu = new CrusherGuiMenu(1, player.getInventory(),
                new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(helper.absolutePos(pos)));
        menu.quickMoveStack(player, 3);
        helper.assertTrue(crusher.getItem(2).getCount() == 16 && player.getInventory().getItem(9).isEmpty(),
                "Shift-click must put the full stack in the upgrade slot");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void tierSlotsStackAndSurviveSaving(GameTestHelper helper) {
        int[] slotCounts = {0, 1, 2, 3, 3, 4, 4, 5, 5};
        for (int tier = 0; tier < slotCounts.length; tier++)
            helper.assertTrue(net.crystalnexus.processing.MachineTier.forLevel(tier).upgradeSlots() == slotCounts[tier],
                    "Incorrect upgrade slot count for tier " + tier);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.HYPER_CRUSHER.get());
        CrystalCrusherBlockEntity crusher = helper.getBlockEntity(pos);
        InvWrapper automation = new InvWrapper(crusher);
        var speed = CrystalnexusModItems.ACCELERATION_UPGRADE.get();
        for (int slot : new int[]{2, 3, 4, 5, 6})
            helper.assertTrue(automation.insertItem(slot, new ItemStack(speed, 16), false).isEmpty(),
                    "Hyper crusher must accept all five upgrade stacks");
        var upgrades = MachineUpgradeHelper.upgrades(crusher, 2, 3);
        double oneSpeed = 100 / MachineUpgradeHelper.processingTime(new ItemStack(speed, 16), 100, 75, 50);
        helper.assertTrue(Math.abs(100 / MachineUpgradeHelper.processingTime(upgrades, 100, 75, 50)
                - (1 + 5 * (oneSpeed - 1))) < 0.000001, "Acceleration bonuses must add across all slots");
        net.crystalnexus.procedures.CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(),
                crusher.getBlockPos().getX(), crusher.getBlockPos().getY(), crusher.getBlockPos().getZ());
        helper.assertTrue(crusher.getPersistentData().getDouble("maxProgress")
                == net.crystalnexus.processing.MachineTier.HYPER.processingTime(
                    MachineUpgradeHelper.processingTime(upgrades, 100, 75, 50)),
                "Machine processing must use all acceleration slots");
        crusher.setItem(6, new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 16));
        helper.assertTrue(MachineUpgradeHelper.energyCost(MachineUpgradeHelper.upgrades(crusher, 2, 3), 4096) < 4096,
                "Efficiency must work alongside acceleration");
        var saved = crusher.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = new CrystalCrusherBlockEntity(crusher.getBlockPos(), crusher.getBlockState());
        restored.loadAdditional(saved, helper.getLevel().registryAccess());
        for (int slot : new int[]{2, 3, 4, 5, 6})
            helper.assertTrue(ItemStack.matches(crusher.getItem(slot), restored.getItem(slot)), "Upgrade stack must survive saving");

        crusher.setItem(6, ItemStack.EMPTY);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "tier-slots-test"), ClientInformation.createDefault());
        player.getInventory().setItem(9, new ItemStack(speed, 16));
        var menu = new CrusherGuiMenu(1, player.getInventory(),
                new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(helper.absolutePos(pos)));
        menu.quickMoveStack(player, menu.slots.size() - 36);
        helper.assertTrue(crusher.getItem(6).getCount() == 16 && crusher.getItem(0).isEmpty(),
                "Shift-click must reach the last upgrade slot without entering the input");
        player.getInventory().setItem(9, new ItemStack(speed));
        menu.quickMoveStack(player, menu.slots.size() - 36);
        helper.assertTrue(crusher.getItem(0).isEmpty() && player.getInventory().getItem(9).getCount() == 1,
                "Full upgrade slots must not redirect upgrades to the input");

        BlockPos low = new BlockPos(3, 1, 1);
        helper.setBlock(low, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        CrystalCrusherBlockEntity crystal = helper.getBlockEntity(low);
        helper.assertTrue(!crystal.canPlaceItem(3, new ItemStack(speed)), "Tier 1 must reject a second slot");
        BlockPos ironPos = new BlockPos(5, 1, 1);
        helper.setBlock(ironPos, CrystalnexusModBlocks.IRON_SMELTER.get());
        net.crystalnexus.block.entity.IronSmelterBlockEntity iron = helper.getBlockEntity(ironPos);
        helper.assertTrue(!iron.canPlaceItem(2, new ItemStack(speed))
                && MachineUpgradeHelper.upgrades(iron, 2, 3).isEmpty(), "Tier 0 must have no active upgrades");
        var basic = new ItemStack(speed);
        var carbon = new ItemStack(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get());
        helper.assertTrue(Math.abs(MachineUpgradeHelper.processingTime(java.util.List.of(basic, carbon), 100, 75, 50)
                - 100 / (1 + (100.0 / 75 - 1) + (100.0 / 50 - 1))) < 0.000001,
                "Basic and carbon acceleration must combine");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void crushingLookupReusesCandidatesAndReloads(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = java.util.List.copyOf(manager.getRecipes());
        var classifications = new java.util.concurrent.atomic.AtomicInteger();
        var ingredientReads = new java.util.concurrent.atomic.AtomicInteger();
        var unrelated = new net.minecraft.world.item.crafting.SmeltingRecipe("", net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.CARROT),
                new ItemStack(net.minecraft.world.item.Items.POTATO), 0, 200) {
            @Override public net.minecraft.world.item.crafting.RecipeType<?> getType() {
                classifications.incrementAndGet();
                return super.getType();
            }
        };
        var id = net.minecraft.resources.ResourceLocation.parse("crystalnexus:lookup_reload_test");
        var marker = new net.minecraft.world.item.crafting.RecipeHolder<>(
                net.minecraft.resources.ResourceLocation.parse("crystalnexus:lookup_scan_marker"), unrelated);
        var input = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
        try {
            var recipes = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>();
            recipes.add(marker);
            manager.replaceRecipes(recipes);
            helper.assertTrue(net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input).isEmpty(),
                    "Unknown inputs must remain empty");
            int scans = classifications.get();
            for (int i = 0; i < 100; i++)
                net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input);
            helper.assertTrue(classifications.get() == scans, "Repeated lookup must not rescan unrelated recipes");

            var ingredients = net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.DIAMOND));
            var recipe = new net.crystalnexus.jei_recipes.OreCrushingJeiRecipe(
                    new ItemStack(net.minecraft.world.item.Items.EMERALD), ingredients) {
                @Override public net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() {
                    ingredientReads.incrementAndGet();
                    return super.getIngredients();
                }
            };
            recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(id, recipe));
            manager.replaceRecipes(recipes);
            helper.assertTrue(net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input)
                    .is(net.minecraft.world.item.Items.EMERALD), "Recipe reload must invalidate cached misses");
            scans = classifications.get();
            int reads = ingredientReads.get();
            long started = System.nanoTime();
            for (int i = 0; i < 1000; i++)
                helper.assertTrue(net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input)
                        .is(net.minecraft.world.item.Items.EMERALD), "Warm lookup must preserve outputs");
            net.crystalnexus.CrystalnexusMod.LOGGER.info("Warm crushing lookup: {} ns/op", (System.nanoTime() - started) / 1000);
            helper.assertTrue(classifications.get() == scans, "Parallel crafts must reuse the candidate list");
            helper.assertTrue(ingredientReads.get() == reads, "Warm lookup must reuse parsed recipe ingredients");
            recipes.set(1, new net.minecraft.world.item.crafting.RecipeHolder<>(id,
                    new net.crystalnexus.jei_recipes.OreCrushingJeiRecipe(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT), ingredients, 8)));
            manager.replaceRecipes(recipes);
            helper.assertTrue(net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input).isEmpty()
                    && net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input,
                        net.crystalnexus.processing.MachineTier.HYPER).is(net.minecraft.world.item.Items.GOLD_INGOT),
                    "Same-count replacement must preserve tier restrictions");
            recipes.remove(1);
            manager.replaceRecipes(recipes);
            helper.assertTrue(net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input).isEmpty(),
                    "Recipe removal must invalidate cached matches");
        } finally {
            manager.replaceRecipes(original);
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void parallelCrusherRespectsInputsSpaceAndEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get().defaultBlockState()
                .setValue(net.crystalnexus.block.CrystalCrusherBlock.BLOCKSTATE, 2));
        CrystalCrusherBlockEntity crusher = helper.getBlockEntity(pos);
        crusher.setItem(2, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 2));
        var input = new ItemStack(net.minecraft.world.item.Items.IRON_ORE, 4);
        var result = net.crystalnexus.util.CrushingRecipeSupport.findResult(helper.getLevel(), input,
                net.crystalnexus.processing.MachineTier.CRYSTAL);
        helper.assertTrue(!result.isEmpty(), "Crusher test requires an iron ore recipe");
        for (int mode = 0; mode < 3; mode++) {
            crusher.setItem(0, input.copy());
            crusher.setItem(1, mode == 1 ? result.copyWithCount(result.getMaxStackSize() - result.getCount()) : ItemStack.EMPTY);
            var energy = crusher.getEnergyStorage();
            while (energy.extractEnergy(Integer.MAX_VALUE, false) > 0) {}
            int perCraft = Math.min(4096, net.crystalnexus.config.CrystalnexusConfig.MACHINES.CRYSTAL_CRUSHER.maxExtract());
            int budget = mode == 2 ? perCraft : 4 * perCraft;
            while (energy.getEnergyStored() < budget && energy.receiveEnergy(budget - energy.getEnergyStored(), false) > 0) {}
            helper.assertTrue(energy.getEnergyStored() == budget, "Test energy budget must fit the machine capacity");
            crusher.getPersistentData().putDouble("progress", 100);
            net.crystalnexus.procedures.CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(),
                    crusher.getBlockPos().getX(), crusher.getBlockPos().getY(), crusher.getBlockPos().getZ());
            int crafts = mode == 0 ? 4 : 1;
            helper.assertTrue(crusher.getItem(0).getCount() == 4 - crafts, "Parallel crusher must respect input/output/energy limits");
            helper.assertTrue(crusher.getItem(1).getCount() == (mode == 1 ? result.getMaxStackSize() : result.getCount() * crafts),
                    "Parallel crusher output count is incorrect");
            helper.assertTrue(energy.getEnergyStored() == budget - crafts * perCraft, "Each parallel craft must pay its energy cost");
            helper.assertTrue(crusher.getPersistentData().getDouble("progress") == 0, "Batch must finish with cleared progress");
        }
        helper.setBlock(pos, CrystalnexusModBlocks.CRYSTAL_PURIFIER.get().defaultBlockState()
                .setValue(net.crystalnexus.block.CrystalPurifierBlock.BLOCKSTATE, 2));
        net.crystalnexus.block.entity.CrystalPurifierBlockEntity purifier = helper.getBlockEntity(pos);
        purifier.setItem(0, new ItemStack(CrystalnexusModItems.ANCIENT_CRYSTAL.get(), 8));
        purifier.setItem(2, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 8));
        purifier.setItem(3, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 4));
        var energy = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                purifier.getBlockPos(), null);
        while (energy.getEnergyStored() < 8192 && energy.receiveEnergy(8192 - energy.getEnergyStored(), false) > 0) {}
        net.crystalnexus.procedures.CrystalPurifierOnTickUpdateProcedure.execute(helper.getLevel(),
                purifier.getBlockPos().getX(), purifier.getBlockPos().getY(), purifier.getBlockPos().getZ());
        helper.assertTrue(energy.getEnergyStored() == 4096 && purifier.getPersistentData().getDouble("progress") == 8,
                "Parallel work steps must pay for every tick: energy=" + energy.getEnergyStored()
                        + ", progress=" + purifier.getPersistentData().getDouble("progress"));
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void specializedRecipeMachinesAcceptSaveAndShiftChips(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "specialized-chips-test"), ClientInformation.createDefault());
        for (var block : new net.minecraft.world.level.block.Block[]{CrystalnexusModBlocks.HEMOLYZER.get(),
                CrystalnexusModBlocks.CRYOGENIC_FLASH_FREEZER_HATCH.get(), CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get(),
                CrystalnexusModBlocks.GRAVITATIONAL_ARRAY_CONTROLLER.get(), CrystalnexusModBlocks.COMET_FORGE_CONTROLLER.get(),
                CrystalnexusModBlocks.MATTER_TRANSMUTATION_TABLE.get(), CrystalnexusModBlocks.PARTICLE_ACCELERATOR_CONTROLLER.get(), CrystalnexusModBlocks.SINGULARITY_COMPRESSOR.get()}) {
            BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, block);
            net.minecraft.world.level.block.entity.BaseContainerBlockEntity machine = helper.getBlockEntity(pos);
            int upgradeSlot = machine.getContainerSize() - 1;
            ItemStack chips = new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 4);
            helper.assertTrue(new InvWrapper(machine).insertItem(upgradeSlot, chips, false).isEmpty(),
                    "Specialized recipe machine must accept parallel chips: " + block);
            var tag = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
            var restored = (net.minecraft.world.Container) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    machine.getBlockPos(), machine.getBlockState(), tag, helper.getLevel().registryAccess());
            helper.assertTrue(restored != null && restored.getItem(upgradeSlot).getCount() == 4,
                    "Specialized chip slot must survive saving: " + block);
            machine.setItem(upgradeSlot, ItemStack.EMPTY);
            player.getInventory().setItem(9, chips.copy());
            var menu = machine.createMenu(1, player.getInventory(), player);
            helper.assertTrue(menu != null, "Machine must provide a menu");
            menu.quickMoveStack(player, menu.slots.size() - 36);
            int installed = 0;
            for (int i = machine.getContainerSize() - MachineUpgradeHelper.upgradeSlots(machine.getBlockState()); i < machine.getContainerSize(); i++)
                installed += machine.getItem(i).getCount();
            helper.assertTrue(installed == 4 && player.getInventory().getItem(9).isEmpty(),
                    "Shift-click must insert chips into the available upgrade slots: " + block);
        }
        var compressor = (net.minecraft.world.Container) helper.getBlockEntity(new BlockPos(1, 1, 1));
        compressor.setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 32));
        BlockPos compressorPos = helper.absolutePos(new BlockPos(1, 1, 1));
        net.crystalnexus.procedures.SingularityCompressorOnTickUpdateProcedure.execute(helper.getLevel(),
                compressorPos.getX(), compressorPos.getY(), compressorPos.getZ());
        helper.assertTrue(compressor.getItem(0).getCount() == 24,
                "Parallel compression must consume all eight material work steps");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void upgradeEffectsAndSsdRemainDistinct(GameTestHelper helper) {
        ItemStack empty = ItemStack.EMPTY;
        ItemStack one = new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get());
        ItemStack two = new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 2);
        ItemStack speed = new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(), 16);
        ItemStack efficiency = new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 16);
        ItemStack range = new ItemStack(CrystalnexusModItems.RANGE_UPGRADE.get(), 16);
        helper.assertTrue(MachineUpgradeHelper.processingTime(empty, 100, 75, 50) == 100
                && MachineUpgradeHelper.processingTime(one, 100, 75, 50) == 75
                && MachineUpgradeHelper.processingTime(two, 100, 75, 50) < 75
                && MachineUpgradeHelper.processingTime(speed, 100, 75, 50) < MachineUpgradeHelper.processingTime(two, 100, 75, 50),
                "Processing speed must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.energyCost(empty, 4096) == 4096
                && MachineUpgradeHelper.energyCost(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()), 4096) == 2731
                && MachineUpgradeHelper.energyCost(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 2), 4096) == 2048
                && MachineUpgradeHelper.energyCost(efficiency, 4096) < 2048,
                "FE efficiency must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.generatorEfficiency(empty, 1.25, 1.5) == 1
                && MachineUpgradeHelper.generatorEfficiency(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()), 1.25, 1.5) == 1.25
                && MachineUpgradeHelper.generatorEfficiency(new ItemStack(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get(), 2), 1.25, 1.5) == 1.5
                && MachineUpgradeHelper.generatorEfficiency(efficiency, 1.25, 1.5) > 1.5,
                "Generator FE output must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.scaledEffect(empty, 12, 25) == 12
                && MachineUpgradeHelper.scaledEffect(new ItemStack(CrystalnexusModItems.RANGE_UPGRADE.get()), 12, 25) == 25
                && MachineUpgradeHelper.scaledEffect(new ItemStack(CrystalnexusModItems.RANGE_UPGRADE.get(), 2), 12, 25) == 38
                && MachineUpgradeHelper.scaledEffect(range, 12, 25) > 38,
                "Range must improve at counts 0, 1, 2, and 16");
        helper.assertTrue(MachineUpgradeHelper.generatorCycleTime(empty, 250, 325, 500) == 250
                && MachineUpgradeHelper.generatorCycleTime(one, 250, 325, 500) == 325
                && MachineUpgradeHelper.generatorCycleTime(two, 250, 325, 500) == 400
                && MachineUpgradeHelper.generatorCycleTime(speed, 250, 325, 500) > 400,
                "Generator cycle effect must improve at counts 0, 1, 2, and 16");

        ItemStack ssd = new ItemStack(CrystalnexusModItems.SSD.get());
        CustomData.update(DataComponents.CUSTOM_DATA, ssd, tag -> {
            tag.putDouble("cook_mult", 0.5);
            tag.putDouble("fe_efficiency", 2.0);
        });
        helper.assertTrue(ssd.getMaxStackSize() == 64 && MachineUpgradeHelper.cookTime(ssd, 100) == 50
                && MachineUpgradeHelper.energyCost(ssd, 4096) == 2048,
                "SSD stack size and custom effects must stay unchanged");
        helper.succeed();
    }
}
