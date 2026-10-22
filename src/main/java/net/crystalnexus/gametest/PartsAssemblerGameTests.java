package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.PartsAssemblerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.PartsAssemblingRecipe;
import net.crystalnexus.processing.PartsAssemblingDefaults;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder("crystalnexus_parts")
@PrefixGameTestTemplate(false)
public final class PartsAssemblerGameTests {
    @GameTest(template = "empty")
    public static void parallelCraftsCompleteTogetherAndUseBothUpgradeSlots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        while (Math.floorMod(helper.getLevel().getGameTime() + helper.absolutePos(pos).asLong(), 5) != 0)
            pos = pos.offset(1, 0, 0);
        helper.setBlock(pos, CrystalnexusModBlocks.PARTS_ASSEMBLER.get());
        PartsAssemblerBlockEntity machine = helper.getBlockEntity(pos);
        machine.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        machine.setItem(2, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get()));
        machine.setItem(3, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get()));
        helper.assertTrue(machine.getContainerSize() == 4 && machine.canPlaceItem(3, machine.getItem(3)),
            "The second upgrade slot must accept upgrades");
        for (int i = 0; i < 74; i++) {
            machine.getEnergyStorage().receiveEnergy(2048, false);
            tick(machine);
        }
        helper.assertTrue(machine.getData().get(0) == 74 && machine.getItem(1).isEmpty(),
            "Parallelization must not advance progress faster; acceleration sets a 75-tick cycle");
        int energyBefore = machine.getEnergyStorage().getEnergyStored();
        tick(machine);
        helper.assertTrue(machine.getItem(1).getCount() == 2 && machine.getItem(0).getCount() == 6,
            "One parallel chip must craft two plates from two ingots at cycle completion");
        helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == energyBefore - 20,
            "Both crafts must pay the recipe's energy cost");
        machine.setItem(1, new ItemStack(CrystalnexusModItems.IRON_SHEET.get(), 63));
        for (int i = 0; i < 75; i++) {
            machine.getEnergyStorage().receiveEnergy(2048, false);
            tick(machine);
        }
        helper.assertTrue(machine.getItem(1).getCount() == 64 && machine.getItem(0).getCount() == 5,
            "Parallel crafts must stop when output space runs out");
        machine.setItem(0, new ItemStack(Items.IRON_INGOT));
        machine.setItem(1, ItemStack.EMPTY);
        for (int i = 0; i < 75; i++) {
            machine.getEnergyStorage().receiveEnergy(2048, false);
            tick(machine);
        }
        helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).getCount() == 1,
            "Parallel crafts must not create output without input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taggedPartsRespectPriorityModesAndReload(GameTestHelper helper) {
        var registry = BuiltInRegistries.ITEM;
        Map<TagKey<Item>, List<Holder<Item>>> originalTags = new HashMap<>();
        registry.getTags().forEach(pair -> originalTags.put(pair.getFirst(), pair.getSecond().stream().toList()));
        var manager = helper.getLevel().getRecipeManager();
        var originalRecipes = List.copyOf(manager.getRecipes());
        var tags = new HashMap<>(originalTags);
        Item[] outputs = {CrystalnexusModItems.IRON_SHEET.get(), Items.STICK, Items.IRON_NUGGET};
        var allTheOresRod = ResourceLocation.parse("alltheores:tin_rod");
        if (registry.containsKey(allTheOresRod)) outputs[1] = registry.get(allTheOresRod);
        tags.put(tag("c:ingots/parts_test"), List.of(Items.BRICK.builtInRegistryHolder()));
        // A second namespace and singular family must merge into the same material.
        tags.put(tag("forge:ingot/parts_test"), List.of(Items.FLINT.builtInRegistryHolder()));
        tags.put(tag("c:plates/parts_test"), List.of(Items.PAPER.builtInRegistryHolder(), outputs[0].builtInRegistryHolder()));
        tags.put(tag("forge:rods/parts_test"), List.of(Items.STICK.builtInRegistryHolder(), outputs[1].builtInRegistryHolder()));
        tags.put(tag("c:bolts/parts_test"), List.of(outputs[2].builtInRegistryHolder()));
        try {
            registry.bindTags(tags);
            BlockPos pos = new BlockPos(1, 1, 1);
            while (Math.floorMod(helper.getLevel().getGameTime() + helper.absolutePos(pos).asLong(), 5) != 0)
                pos = pos.offset(1, 0, 0);
            helper.setBlock(pos, CrystalnexusModBlocks.PARTS_ASSEMBLER.get());
            PartsAssemblerBlockEntity machine = helper.getBlockEntity(pos);
            machine.setItem(0, new ItemStack(Items.BRICK));
            machine.getEnergyStorage().receiveEnergy(2048, false);
            tick(machine);
            helper.assertTrue(machine.getData().get(0) == 0, "No recipe must leave the assembler idle");

            var event = new OnDatapackSyncEvent(helper.getLevel().getServer().getPlayerList(), null);
            PartsAssemblingDefaults.sync(event);
            int recipeCount = manager.getRecipes().size();
            PartsAssemblingDefaults.sync(event);
            helper.assertTrue(manager.getRecipes().size() == recipeCount, "Generation must be idempotent on player joins");
            if (registry.containsKey(allTheOresRod)) {
                var tinIngot = new ItemStack(registry.get(ResourceLocation.parse("alltheores:tin_ingot")));
                helper.assertTrue(manager.getAllRecipesFor(PartsAssemblingRecipe.Type.INSTANCE).stream().anyMatch(holder ->
                    holder.value().mode() == PartsAssemblingRecipe.Mode.ROD && holder.value().ingredient().test(tinIngot)
                        && holder.value().getResultItem(helper.getLevel().registryAccess()).is(outputs[1])),
                    "Actual AllTheOres tin ingots must gain a rod recipe");
            }
            var ironRecipe = manager.getAllRecipesFor(PartsAssemblingRecipe.Type.INSTANCE).stream()
                .filter(holder -> holder.id().equals(ResourceLocation.parse("crystalnexus:parts_assembling_iron_plate")))
                .findFirst().orElseThrow();
            helper.assertTrue(ironRecipe.value().getResultItem(helper.getLevel().registryAccess()).is(outputs[0]),
                "Explicit recipes must remain intact");

            for (int mode = 0; mode < outputs.length; mode++) {
                machine.setSelectedMode(mode);
                machine.setItem(0, new ItemStack(mode == 1 ? Items.FLINT : Items.BRICK));
                machine.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
                int energy = machine.getEnergyStorage().getEnergyStored();
                tick(machine);
                helper.assertTrue(machine.getItem(0).getCount() == 1 && machine.getEnergyStorage().getEnergyStored() == energy,
                    "Blocked output must preserve input and energy");
                machine.setItem(1, ItemStack.EMPTY);
                for (int i = 0; i < 100; i++) {
                    machine.getEnergyStorage().receiveEnergy(2048, false);
                    tick(machine);
                }
                helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).is(outputs[mode])
                        && machine.getItem(1).getCount() == (mode == 2 ? 2 : 1),
                    "Tagged mode " + mode + " must craft the preferred parts from one ingot after a cached no-match");
            }
            manager.replaceRecipes(originalRecipes);
            machine.setSelectedMode(0);
            machine.setItem(0, new ItemStack(Items.BRICK));
            machine.setItem(1, ItemStack.EMPTY);
            tick(machine);
            helper.assertTrue(machine.getData().get(0) == 0, "Removing recipes must invalidate cached matches");
        } finally {
            manager.replaceRecipes(originalRecipes);
            // Retain the temporary keys as empty sets so holders also lose their test tags.
            tags.replaceAll((key, values) -> originalTags.getOrDefault(key, List.of()));
            registry.bindTags(tags);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void latheProducesDustAndStopsWhenEitherOutputIsFull(GameTestHelper helper) {
        PartsAssemblingDefaults.sync(new OnDatapackSyncEvent(helper.getLevel().getServer().getPlayerList(), null));
        BlockPos pos = new BlockPos(1, 1, 1);
        while (Math.floorMod(helper.getLevel().getGameTime() + helper.absolutePos(pos).asLong(), 5) != 0)
            pos = pos.offset(1, 0, 0);
        helper.setBlock(pos, CrystalnexusModBlocks.AZURINE_LATHE.get());
        PartsAssemblerBlockEntity machine = helper.getBlockEntity(pos);
        machine.setItem(0, new ItemStack(CrystalnexusModItems.TITANIUM_INGOT.get(), 8));
        machine.setSelectedMode(0);
        helper.assertTrue(machine.getData().get(4) == 1, "Lathe must reject plate mode");
        for (int i = 0; i < 124; i++) {
            machine.getEnergyStorage().receiveEnergy(2048, false);
            tick(machine);
        }
        helper.assertTrue(machine.getItem(1).isEmpty() && machine.getData().get(0) == 124,
            "Lathe must take 125 ticks for a standard recipe");
        helper.getLevel().random.setSeed(2L);
        tick(machine);
        var dust = PartsAssemblingDefaults.associatedDust(new ItemStack(CrystalnexusModItems.TITANIUM_INGOT.get()));
        helper.assertTrue(!dust.isEmpty() && machine.getItem(4).is(dust.getItem())
            && machine.getItem(4).getCount() == 1 && machine.getItem(1).is(CrystalnexusModItems.TITANIUM_ROD.get()),
            "Lathe must output a rod and one matching dust");
        machine.setItem(4, dust.copyWithCount(dust.getMaxStackSize()));
        int energy = machine.getEnergyStorage().getEnergyStored();
        tick(machine);
        helper.assertTrue(machine.getItem(0).getCount() == 7 && machine.getEnergyStorage().getEnergyStored() == energy,
            "Full dust output must preserve input and energy");
        machine.setItem(4, new ItemStack(Items.IRON_NUGGET));
        tick(machine);
        helper.assertTrue(machine.getItem(0).getCount() == 7 && machine.getItem(4).is(Items.IRON_NUGGET),
            "Existing nuggets must remain collectible and block incompatible dust output");
        machine.setItem(4, ItemStack.EMPTY);
        machine.setItem(2, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get()));
        machine.setSelectedMode(2);
        machine.setItem(1, ItemStack.EMPTY);
        tick(machine);
        helper.assertTrue(machine.getData().get(0) == 0 && machine.getItem(0).getCount() == 7,
            "Lathe bolt mode must reject ingots");
        machine.setItem(0, new ItemStack(CrystalnexusModItems.TITANIUM_ROD.get(), 7));
        helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(PartsAssemblingRecipe.Type.INSTANCE).stream()
            .anyMatch(holder -> PartsAssemblingDefaults.isLatheOnly(holder.id()) && holder.value().ingredient().test(machine.getItem(0))),
            "Datapack sync must generate the tagged rod-to-bolt recipe");
        for (int i = 0; i < 125; i++) {
            machine.getEnergyStorage().receiveEnergy(2048, false);
            if (i == 124) helper.getLevel().random.setSeed(2L);
            tick(machine);
        }
        helper.assertTrue(machine.getItem(1).getCount() == 4 && machine.getItem(4).getCount() == 1
            && machine.getItem(0).getCount() == 5, "Parallel crafts must roll dust separately (seed 2 rolls 8 then 72): part=" + machine.getItem(1).getCount()
                + ", dust=" + machine.getItem(4).getCount() + ", input=" + machine.getItem(0).getCount());
        machine.setItem(4, ItemStack.EMPTY);
        machine.setItem(2, ItemStack.EMPTY);
        for (int i = 0; i < 125; i++) {
            machine.getEnergyStorage().receiveEnergy(2048, false);
            if (i == 124) helper.getLevel().random.setSeed(0L);
            tick(machine);
        }
        helper.assertTrue(machine.getItem(0).getCount() == 4 && machine.getItem(1).getCount() == 6
            && machine.getItem(4).isEmpty(), "A failed dust roll must still craft the part (seed 0 rolls 60)");
        machine.setItem(4, dust.copy());
        helper.assertTrue(machine.canTakeItemThroughFace(4, machine.getItem(4), net.minecraft.core.Direction.DOWN),
            "Automation must extract dust");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ferrosteelLatheScalesSpeedEnergyStorageAndAllUpgrades(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        while (Math.floorMod(helper.getLevel().getGameTime() + helper.absolutePos(pos).asLong(), 5) != 0)
            pos = pos.offset(1, 0, 0);
        helper.setBlock(pos, CrystalnexusModBlocks.FERROSTEEL_LATHE.get());
        PartsAssemblerBlockEntity machine = helper.getBlockEntity(pos);
        machine.setItem(0, new ItemStack(CrystalnexusModItems.TITANIUM_INGOT.get(), 512));
        machine.setSelectedMode(0);
        helper.assertTrue(machine.getData().get(4) == 1 && machine.getContainerSize() == 7,
            "Ferrosteel lathe must reject plates and expose four upgrade slots plus dust");
        for (int i = 0; i < 38; i++) {
            machine.getEnergyStorage().receiveEnergy(2048, false);
            int before = machine.getEnergyStorage().getEnergyStored();
            if (i == 37) helper.getLevel().random.setSeed(2L);
            tick(machine);
            helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == before - 320,
                "Ferrosteel must apply its 32x energy multiplier");
            if (i < 37) helper.assertTrue(machine.getItem(1).isEmpty(), "Ferrosteel cycle must last 38 ticks");
        }
        helper.assertTrue(machine.getItem(1).is(CrystalnexusModItems.TITANIUM_ROD.get())
            && machine.getItem(4).is(CrystalnexusModItems.AZURINE_DUST.get())
            && machine.getItem(0).getCount() == 511, "Ferrosteel must produce the part and matching dust");
        machine.setItem(6, new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get()));
        machine.getEnergyStorage().receiveEnergy(2048, false);
        tick(machine);
        helper.assertTrue(machine.getData().get(1) == 29,
            "The fourth upgrade slot must affect processing speed");
        machine.setItem(1, new ItemStack(CrystalnexusModItems.TITANIUM_ROD.get(), 512));
        machine.setItem(4, new ItemStack(CrystalnexusModItems.AZURINE_DUST.get(), 512));
        var registries = helper.getLevel().registryAccess();
        var restored = new PartsAssemblerBlockEntity(machine.getBlockPos(), machine.getBlockState());
        restored.loadWithComponents(machine.saveWithFullMetadata(registries), registries);
        helper.assertTrue(restored.getItem(0).getCount() == 511 && restored.getItem(1).getCount() == 512
            && restored.getItem(4).getCount() == 512 && !restored.getItem(6).isEmpty(),
            "Saving must retain oversized material stacks and appended upgrades");
        var handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
            machine.getBlockPos(), net.minecraft.core.Direction.DOWN);
        helper.assertTrue(handler != null && handler.getSlotLimit(4) == 512
            && handler.extractItem(4, 512, true).getCount() == 64 && machine.getItem(4).getCount() == 512
            && handler.extractItem(6, 1, false).isEmpty(),
            "Automation must expose dust at tier capacity, simulate safely, and protect upgrades");
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "lathe-test"),
            net.minecraft.server.level.ClientInformation.createDefault());
        var menu = new net.crystalnexus.world.inventory.PartsAssemblerMenu(1, player.getInventory(), machine, machine.getData());
        helper.assertTrue(menu.slots.size() == 43 && menu.getSlot(4).getMaxStackSize(machine.getItem(4)) == 512,
            "The menu must expose all seven machine slots and tier storage limits");
        menu.clicked(4, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 64 && machine.getItem(4).getCount() == 448,
            "Dust pickup must leave overflow in the machine and keep cursor stacks legal");
        menu.quickMoveStack(player, 1);
        helper.assertTrue(machine.getItem(1).isEmpty(), "Shift-click must move the full output into ordinary player stacks");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            helper.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "Player stacks must stay within vanilla limits");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pressesTurnOneBlockIntoTwelvePlatesAtTheirTier(GameTestHelper helper) {
        var event = new OnDatapackSyncEvent(helper.getLevel().getServer().getPlayerList(), null);
        PartsAssemblingDefaults.sync(event);
        int recipeCount = helper.getLevel().getRecipeManager().getRecipes().size();
        PartsAssemblingDefaults.sync(event);
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().size() == recipeCount,
            "Press recipe generation must be idempotent");
        var blocks = new net.minecraft.world.level.block.Block[]{
            CrystalnexusModBlocks.AZURINE_PRESS.get(), CrystalnexusModBlocks.FERROSTEEL_PRESS.get()};
        int[] ticks = {40, 30}, energyCosts = {80, 320}, capacities = {256, 512}, sizes = {5, 6};
        for (int tier = 0; tier < blocks.length; tier++) {
            BlockPos pos = new BlockPos(1, 1, 1 + tier * 2);
            while (Math.floorMod(helper.getLevel().getGameTime() + helper.absolutePos(pos).asLong(), 5) != 0)
                pos = pos.offset(1, 0, 0);
            helper.setBlock(pos, blocks[tier]);
            PartsAssemblerBlockEntity machine = helper.getBlockEntity(pos);
            helper.assertTrue(machine.isPress() && !machine.isLathe() && machine.getContainerSize() == sizes[tier],
                "Press must have two material slots and its tier's upgrade slots");
            machine.setSelectedMode(2);
            helper.assertTrue(machine.getData().get(4) == 0, "Press must remain in plate mode");
            machine.setItem(0, new ItemStack(Items.IRON_BLOCK, 3));
            for (int i = 0; i < ticks[tier]; i++) {
                machine.getEnergyStorage().receiveEnergy(2048, false);
                int energy = machine.getEnergyStorage().getEnergyStored();
                tick(machine);
                helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == energy - energyCosts[tier],
                    "Press must use its tier's energy multiplier");
                if (i < ticks[tier] - 1) helper.assertTrue(machine.getItem(1).isEmpty(), "Press must respect its tier's cycle time");
            }
            helper.assertTrue(machine.getItem(0).getCount() == 2 && machine.getItem(1).getCount() == 12
                && machine.getItem(1).is(CrystalnexusModItems.IRON_SHEET.get()),
                "One iron block must produce exactly twelve preferred iron plates");
            machine.setItem(1, new ItemStack(CrystalnexusModItems.IRON_SHEET.get(), capacities[tier] - 11));
            int energy = machine.getEnergyStorage().getEnergyStored();
            tick(machine);
            helper.assertTrue(machine.getItem(0).getCount() == 2 && machine.getEnergyStorage().getEnergyStored() == energy,
                "Insufficient room for all twelve plates must preserve input and energy");
            machine.setItem(1, ItemStack.EMPTY);
            int lastUpgrade = machine.getContainerSize() - 1;
            machine.setItem(lastUpgrade, new ItemStack(CrystalnexusModItems.PARALLELIZATION_CHIP.get()));
            helper.assertTrue(machine.canPlaceItem(lastUpgrade, machine.getItem(lastUpgrade)), "Last upgrade slot must be usable");
            for (int i = 0; i < ticks[tier]; i++) {
                machine.getEnergyStorage().receiveEnergy(2048, false);
                tick(machine);
            }
            helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).getCount() == 24,
                "The last upgrade slot must parallelize two blocks into twenty-four plates");
            machine.setItem(0, new ItemStack(Items.IRON_INGOT));
            tick(machine);
            helper.assertTrue(machine.getData().get(0) == 0 && machine.getItem(0).getCount() == 1,
                "Press must reject ingots and ordinary assembler plate recipes");
            var handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                machine.getBlockPos(), net.minecraft.core.Direction.DOWN);
            helper.assertTrue(handler != null && handler.getSlotLimit(1) == capacities[tier]
                && handler.extractItem(1, 64, true).getCount() == 24 && machine.getItem(1).getCount() == 24
                && handler.extractItem(lastUpgrade, 1, false).isEmpty(),
                "Automation must expose plate output and protect upgrades");
            var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "press-test"),
                net.minecraft.server.level.ClientInformation.createDefault());
            var menu = new net.crystalnexus.world.inventory.PartsAssemblerMenu(1, player.getInventory(), machine, machine.getData());
            helper.assertTrue(menu.slots.size() == sizes[tier] + 36 && !menu.clickMenuButton(player, 1)
                && menu.getSlot(4).mayPlace(new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get())),
                "Press GUI must expose all upgrade slots, including slot four, and reject mode changes");
            var pressRecipe = helper.getLevel().getRecipeManager().getAllRecipesFor(PartsAssemblingRecipe.Type.INSTANCE).stream()
                .filter(holder -> PartsAssemblingDefaults.isPressOnly(holder.id()) && holder.value().ingredient().test(new ItemStack(Items.IRON_BLOCK)))
                .findFirst().orElseThrow();
            var assembler = new PartsAssemblerBlockEntity(machine.getBlockPos(), CrystalnexusModBlocks.PARTS_ASSEMBLER.get().defaultBlockState());
            helper.assertTrue(machine.supportsRecipe(pressRecipe.id(), pressRecipe.value())
                && !assembler.supportsRecipe(pressRecipe.id(), pressRecipe.value()), "Press recipes must stay exclusive to presses");
        }
        helper.succeed();
    }

    private static TagKey<Item> tag(String id) { return ItemTags.create(ResourceLocation.parse(id)); }

    private static void tick(PartsAssemblerBlockEntity machine) {
        PartsAssemblerBlockEntity.serverTick(machine.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
}
