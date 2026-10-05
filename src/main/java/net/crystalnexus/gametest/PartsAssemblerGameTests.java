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

    private static TagKey<Item> tag(String id) { return ItemTags.create(ResourceLocation.parse(id)); }

    private static void tick(PartsAssemblerBlockEntity machine) {
        PartsAssemblerBlockEntity.serverTick(machine.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
}
