package net.crystalnexus.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("crystalnexus_gui")
@PrefixGameTestTemplate(false)
public final class MachineGuiGameTests {
    private MachineGuiGameTests() {}

    @GameTest(template = "zero_point")
    public static void separatorsProduceOnlyPrimaryAndKeepOneOutput(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var recipes = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(original);
        var recipe = new net.crystalnexus.jei_recipes.DustSeperationRecipe(
            java.util.Optional.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 2)),
            java.util.Optional.empty(), net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY,
                net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STICK)), 1,
            java.util.Optional.empty(), java.util.Optional.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD)),
            java.util.Optional.empty(), 1f, 0);
        recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(ResourceLocation.parse("crystalnexus:test_single_separator_output"), recipe));
        try {
            manager.replaceRecipes(recipes);
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            for (var block : List.of(net.crystalnexus.init.CrystalnexusModBlocks.DUST_SEPARATOR.get(),
                    net.crystalnexus.init.CrystalnexusModBlocks.CHLOROPHYTE_DUST_SEPARATOR.get(),
                    net.crystalnexus.init.CrystalnexusModBlocks.INVERTIUM_DUST_SEPARATOR.get(),
                    net.crystalnexus.init.CrystalnexusModBlocks.HYPER_DUST_SEPARATOR.get())) {
                BlockPos pos = new BlockPos(1, 2, 1);
                helper.setBlock(pos, block);
                var separator = (net.crystalnexus.block.entity.DustSeparatorBlockEntity) helper.getBlockEntity(pos);
                separator.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
                while (separator.getEnergyStorage().getEnergyStored() < 4096)
                    separator.getEnergyStorage().receiveEnergy(4096, false);
                BlockPos absolute = helper.absolutePos(pos);
                for (int i = 0; i < 101; i++) net.crystalnexus.procedures.DustSeparatorOnTickUpdateProcedure.execute(
                    helper.getLevel(), absolute.getX(), absolute.getY(), absolute.getZ());
                helper.assertTrue(separator.getItem(0).isEmpty() && separator.getItem(1).is(net.minecraft.world.item.Items.DIAMOND)
                    && separator.getItem(1).getCount() == 2 && separator.getItem(3).isEmpty(), "Separator must produce only the primary result");
                FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(absolute);
                try {
                    var menu = new net.crystalnexus.world.inventory.SeparatorGuiMenu(1, player.getInventory(), data);
                    helper.assertTrue(!menu.getSlots().containsKey(3), "Retired byproduct slot must not appear in the menu");
                    int before = player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND);
                    var output = menu.getSlots().get(1);
                    menu.quickMoveStack(player, output.index);
                    helper.assertTrue(!output.hasItem() && player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == before + 2,
                        "Single output must shift-click correctly at every tier");
                } finally { data.release(); }
            }
        } finally { manager.replaceRecipes(original); }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void retiredSeparatorItemsSurviveReloadAndReturnExactlyOnce(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1), absolute = helper.absolutePos(pos);
        helper.setBlock(pos, net.crystalnexus.init.CrystalnexusModBlocks.HYPER_DUST_SEPARATOR.get());
        var separator = (net.crystalnexus.block.entity.DustSeparatorBlockEntity) helper.getBlockEntity(pos);
        separator.setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 130));
        var saved = separator.saveWithoutMetadata(helper.getLevel().registryAccess());
        separator.loadAdditional(saved, helper.getLevel().registryAccess());
        helper.assertTrue(separator.getItem(3).getCount() == 130, "Retired bulk stacks must survive loading");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (int reopen = 0; reopen < 2; reopen++) {
            FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(absolute);
            try { new net.crystalnexus.world.inventory.SeparatorGuiMenu(1, player.getInventory(), data); }
            finally { data.release(); }
        }
        helper.assertTrue(separator.getItem(3).isEmpty() && player.getInventory().countItem(net.minecraft.world.item.Items.EMERALD) == 130,
            "Opening twice must recover every retired item exactly once");
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE, 64));
        player.setPos(absolute.getCenter());
        separator.setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 130));
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(absolute);
        try { new net.crystalnexus.world.inventory.SeparatorGuiMenu(1, player.getInventory(), data); }
        finally { data.release(); }
        int dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(absolute).inflate(3)).stream()
            .filter(item -> item.getItem().is(net.minecraft.world.item.Items.EMERALD)).mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertTrue(separator.getItem(3).isEmpty() && dropped == 130, "Full inventory must drop all recovered items safely");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void onlyFerrosteelPressExposesAFluidTank(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        for (var block : List.of(net.crystalnexus.init.CrystalnexusModBlocks.CIRCUIT_PRESS.get(),
                net.crystalnexus.init.CrystalnexusModBlocks.TITANIUM_CARBIDE_CIRCUIT_PRESS.get())) {
            helper.setBlock(pos, block);
            boolean ferrosteel = block == net.crystalnexus.init.CrystalnexusModBlocks.TITANIUM_CARBIDE_CIRCUIT_PRESS.get();
            var press = (net.crystalnexus.block.entity.CircuitPressBlockEntity) helper.getBlockEntity(pos);
            press.getNitrogenTank().setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.crystalnexus.init.CrystalnexusModFluids.NITROGEN.get(), 500));
            var saved = press.saveWithoutMetadata(helper.getLevel().registryAccess());
            press.loadAdditional(saved, helper.getLevel().registryAccess());
            helper.assertTrue(press.getNitrogenTank().getFluidAmount() == 500, "Existing nitrogen must survive reload for either press");
            for (var side : net.minecraft.core.Direction.values())
                helper.assertTrue((helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(pos), side) != null) == ferrosteel, "Only ferrosteel exposes fluid automation");
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void idleCollectorDeliversItsFinalTankChange(GameTestHelper helper) throws Exception {
        BlockPos pos = new BlockPos(1,2,1);
        helper.setBlock(pos, net.crystalnexus.init.CrystalnexusModBlocks.NODE_EXTRACTOR.get());
        var collector = (net.crystalnexus.block.entity.NodeExtractorBlockEntity) helper.getBlockEntity(pos);
        var tank = collector.getFluidTank();
        tank.setFluid(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
        tank.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var pending = net.crystalnexus.util.MachineSync.class.getDeclaredField("pending");
        pending.setAccessible(true);
        helper.assertTrue(pending.getBoolean(collector.machineSync), "Final empty-tank update was not queued");
        helper.runAfterDelay(6, () -> {
            try {
                helper.assertTrue(tank.isEmpty(), "Collector should remain empty while idle");
                helper.assertTrue(!pending.getBoolean(collector.machineSync), "Idle collector did not deliver its pending tank update");
                helper.succeed();
            } catch (IllegalAccessException error) { throw new RuntimeException(error); }
        });
    }

    @GameTest(template = "zero_point")
    public static void machineSlotsRespectTheirThemeGeometry(GameTestHelper helper) throws Exception {
        Map<String,String> menus = new LinkedHashMap<>();
        try (var stream = MachineGuiGameTests.class.getResourceAsStream("/assets/crystalnexus/machine_gui_tiers.json")) {
                var manifest = JsonParser.parseReader(new InputStreamReader(java.util.Objects.requireNonNull(stream,
                    "Missing machine GUI tier assignments"), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : manifest.getAsJsonObject("screens").entrySet()) {
                var data = entry.getValue().getAsJsonObject();
                var machines = data.getAsJsonArray("machines");
                String chosen = "machine_frame";
                int highest = -1;
                for (var id : machines) {
                    var tier = manifest.getAsJsonObject("machines").getAsJsonObject(id.getAsString());
                    int rank = tier == null ? 0 : tier.get("tier").getAsInt();
                    if (rank > highest) { highest = rank; chosen = id.getAsString(); }
                }
                menus.put(data.get("menu").getAsString(), chosen);
            }
        }
        var themedMenus = java.util.Set.copyOf(menus.keySet());
        menus.putAll(Map.ofEntries(Map.entry("CelestialGearForgeMenu","celestial_gear_forge"),
                Map.entry("CometForgeMenu","comet_forge_controller"), Map.entry("GravitationalArrayMenu","gravitational_array_controller"),
                Map.entry("SolarSimulatorMenu","solar_simulator_controller"), Map.entry("SolarEngineMenu","solar_engine_controller"),
                Map.entry("HeavenshatterMenu","heavenshatter_beam_controller"), Map.entry("BioMGuiMenu","biomatic_constructor"),
                Map.entry("BioMCGuiMenu","biomatic_composter"), Map.entry("BioSIMGuiMenu","biomatic_simulator"), Map.entry("MawMenu","maw"),
                Map.entry("MasticatorMenu","gene_splicer"), Map.entry("HemolyzerMenu","hemolyzer"), Map.entry("HemochanterMenu","hemochanter")));
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "gui-grid-test"), ClientInformation.createDefault());
        BlockPos pos = new BlockPos(1,2,1);
        List<String> issues = new java.util.ArrayList<>();
        for (var entry : menus.entrySet()) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("crystalnexus", entry.getValue()));
            helper.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR, "Missing machine block: " + entry.getValue());
            helper.setBlock(pos, block);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeBlockPos(helper.absolutePos(pos));
            if (entry.getKey().equals("DepotMenu")) buffer.writeBoolean(false).writeBoolean(false);
            if (entry.getKey().equals("ReactorGUIMenu")) buffer.writeVarInt(0);
            AbstractContainerMenu menu;
            try {
                menu = (AbstractContainerMenu) Class.forName("net.crystalnexus.world.inventory." + entry.getKey())
                        .getConstructor(int.class, Inventory.class, FriendlyByteBuf.class).newInstance(1, player.getInventory(), buffer);
            } catch (ReflectiveOperationException error) { issues.add("Cannot construct " + entry.getKey() + ": " + error.getCause()); continue; }
            finally { buffer.release(); }
            var grid = MachineGuiLayout.grid(menu.slots);
            var carriedSlots = menu.slots.stream().filter(slot -> slot.isActive() && slot.container instanceof Inventory).toList();
            if (!carriedSlots.isEmpty()) {
                int inventoryTop = carriedSlots.stream().mapToInt(slot -> MachineGuiLayout.slot(slot.x, slot.y).y()).min().orElseThrow();
                for (var slot : menu.slots) {
                    if (!slot.isActive() || slot.container instanceof Inventory
                            || slot instanceof net.crystalnexus.world.inventory.MachineUpgradeSlot
                            || slot.x < 0 || slot.x >= MachineGuiLayout.BODY_WIDTH) continue;
                    if (inventoryTop - MachineGuiLayout.slot(slot.x, slot.y).bottom() < MachineGuiLayout.INVENTORY_GAP)
                        issues.add(entry.getKey() + " slot " + slot.getSlotIndex() + " is too close to the player inventory");
                }
                for (var carried : carriedSlots) {
                    int index = carried.getSlotIndex();
                    int expectedY = grid.y() + (index < 9 ? 58 : (index / 9 - 1) * 18);
                    if (carried.y != expectedY) issues.add(entry.getKey() + " separates inventory slot " + index + " from its row");
                }
            }
            for (var slot : menu.slots) {
                if (!slot.isActive() || slot.container instanceof Inventory) continue;
                // The transmutation octagon snaps as a group: its centre is on the grid, its diagonals keep their 27px offsets.
                boolean ring = entry.getKey().equals("MatterTransmutationGUIMenu") && slot.getSlotIndex() < MachineGuiLayout.TRANSMUTATION_OUTPUT;
                // Upgrade slots are centred in their own side bay instead of following the body grid.
                if (slot instanceof net.crystalnexus.world.inventory.MachineUpgradeSlot || slot.x >= MachineGuiLayout.BODY_WIDTH) ring = true;
                // Custom themes retain their original body layout; side bays use their own spacing.
                if (!ring && themedMenus.contains(entry.getKey())
                        && (Math.floorMod(slot.x - grid.x(),18) != 0 || Math.floorMod(slot.y - grid.y(),18) != 0))
                    issues.add(entry.getKey() + " slot " + slot.getSlotIndex() + " is outside the inventory grid: " + slot.x + "," + slot.y);
                for (var other : menu.slots) if (other != slot && other.isActive())
                    if (MachineGuiLayout.slot(slot.x,slot.y).intersects(MachineGuiLayout.slot(other.x,other.y)))
                        issues.add(entry.getKey() + " has overlapping slots " + slot.getSlotIndex() + " and " + other.getSlotIndex());
            }
            if (entry.getKey().equals("MatterTransmutationGUIMenu")) {
                for (int i = 0; i <= MachineGuiLayout.TRANSMUTATION_OUTPUT; i++) {
                    final int index = i;
                    var slot = menu.slots.stream().filter(candidate -> !(candidate.container instanceof Inventory)
                            && candidate.getSlotIndex() == index).findFirst().orElseThrow();
                    int[] expected = MachineGuiLayout.transmutationSlot(i);
                    if (slot.x != expected[0] || slot.y != expected[1])
                        issues.add("Matter Transmutation slot " + i + " left its octagonal position: " + slot.x + "," + slot.y);
                    for (var carried : menu.slots) if (carried.container instanceof Inventory
                            && MachineGuiLayout.slot(slot.x, slot.y).intersects(MachineGuiLayout.slot(carried.x, carried.y)))
                        issues.add("Matter Transmutation slot " + i + " overlaps the player inventory");
                }
                if (grid.y() != MachineGuiLayout.TRANSMUTATION_INVENTORY_Y) issues.add("Matter Transmutation inventory is not clear of the input ring");
            }
            if (entry.getKey().equals("CraftingFactoryGUIMenu") || entry.getKey().equals("SmartSplitterGUIMenu")) {
                int[] overlays = entry.getKey().equals("CraftingFactoryGUIMenu") ? new int[]{10} : new int[]{0,1,2,3};
                for (int index : overlays) {
                    var slot = menu.slots.stream().filter(candidate -> !(candidate.container instanceof Inventory)
                            && candidate.getSlotIndex() == index).findFirst().orElseThrow();
                    var frame = MachineGuiLayout.slot(slot.x, slot.y);
                    if (frame.width() != 18 || frame.height() != 18 || !frame.contains(slot.x, slot.y))
                        issues.add(entry.getKey() + " overlay frame misses slot " + index);
                }
            }
            int outputIndex = switch (entry.getKey()) {
                case "IronSmelterGuiMenu", "CircuitPressGUIMenu", "BioMGuiMenu", "BioSIMGuiMenu", "PistonGenGUIMenu" -> 1;
                case "MatterTransmutationGUIMenu" -> MachineGuiLayout.TRANSMUTATION_OUTPUT;
                case "CometForgeMenu", "GravitationalArrayMenu" -> 4;
                case "CelestialGearForgeMenu" -> 9;
                default -> -1;
            };
            if (outputIndex >= 0) {
                final int index = outputIndex;
                var output = menu.slots.stream().filter(slot -> !(slot.container instanceof Inventory) && slot.getSlotIndex() == index).findFirst().orElseThrow();
                output.set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 4));
                int before = player.getInventory().countItem(net.minecraft.world.item.Items.GOLD_INGOT);
                var moved = menu.quickMoveStack(player, output.index);
                if (moved.isEmpty() || output.hasItem() || player.getInventory().countItem(net.minecraft.world.item.Items.GOLD_INGOT) != before + 4)
                    issues.add(entry.getKey() + " no longer shift-clicks its output into the player inventory");
            }
        }
        helper.assertTrue(issues.isEmpty(), String.join("; ", issues));
        helper.succeed();
    }
}
