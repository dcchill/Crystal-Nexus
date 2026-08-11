package net.crystalnexus.util;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class WrenchLinePlacementGameTests {
    private WrenchLinePlacementGameTests() {
    }

    @GameTest(template = "zero_point")
    public static void plannerUsesSelectedAxisFirst(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 2, 1);
        BlockPos end = new BlockPos(3, 4, 5);
        List<BlockPos> eastFirst = WrenchLinePlacement.planPath(start, end, Direction.EAST);
        List<BlockPos> upFirst = WrenchLinePlacement.planPath(start, end, Direction.UP);
        helper.assertTrue(eastFirst.size() == 9 && eastFirst.get(1).equals(start.east()),
            "An east/west selection must route along X first");
        helper.assertTrue(upFirst.size() == 9 && upFirst.get(1).equals(start.above()),
            "An up/down selection must route vertically first");
        helper.assertTrue(eastFirst.get(eastFirst.size() - 1).equals(end)
                && upFirst.get(upFirst.size() - 1).equals(end),
            "Every route must include its endpoint");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void placesLineAndConsumesOffhandThenInventory(GameTestHelper helper) {
        ServerPlayer player = equippedPlayer(helper, "wrench-line-success", 2);
        player.getInventory().setItem(1, new ItemStack(CrystalnexusModItems.BASIC_ENERGY_CABLE.get(), 4));
        BlockPos startAnchor = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos endAnchor = helper.absolutePos(new BlockPos(5, 2, 4));
        List<BlockPos> path = WrenchLinePlacement.planPath(startAnchor.east(), endAnchor.west(), Direction.EAST);
        for (BlockPos pos : path) helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(startAnchor, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(endAnchor, Blocks.STONE.defaultBlockState());

        use(player, startAnchor, Direction.EAST);
        helper.assertTrue(helper.getLevel().isEmptyBlock(startAnchor.east()),
            "The first use must only select a start point");
        helper.assertTrue(player.getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().contains(WrenchLinePlacement.PREVIEW_KEY),
            "The server must mirror the active selection to the wrench for client preview rendering");
        use(player, endAnchor, Direction.WEST);

        StringBuilder states = new StringBuilder();
        boolean complete = true;
        for (BlockPos pos : path) {
            boolean cableAtPosition = helper.getLevel().getBlockState(pos)
                .is(CrystalnexusModBlocks.BASIC_ENERGY_CABLE.get());
            complete &= cableAtPosition;
            states.append(pos.toShortString()).append('=').append(helper.getLevel().getBlockState(pos).getBlock()).append(' ');
        }
        helper.assertTrue(complete, "Expected a complete route: " + states
            + " active=" + player.getPersistentData().contains(WrenchLinePlacement.STATE_KEY)
            + " offhand=" + player.getOffhandItem().getCount());
        helper.assertTrue(player.getOffhandItem().isEmpty()
                && countInventory(player, new ItemStack(CrystalnexusModItems.BASIC_ENERGY_CABLE.get())) == 0,
            "Survival placement must consume offhand blocks before inventory blocks");
        helper.assertTrue(!player.getPersistentData().contains(WrenchLinePlacement.STATE_KEY),
            "Successful placement must clear the selection");
        helper.assertTrue(!player.getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().contains(WrenchLinePlacement.PREVIEW_KEY),
            "Successful placement must clear the client preview state");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void blockedPathIsAtomicAndSelectionsArePerPlayer(GameTestHelper helper) {
        ServerPlayer first = equippedPlayer(helper, "wrench-line-first", 16);
        ServerPlayer second = equippedPlayer(helper, "wrench-line-second", 16);
        BlockPos firstAnchor = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos secondAnchor = helper.absolutePos(new BlockPos(1, 3, 4));
        BlockPos endAnchor = helper.absolutePos(new BlockPos(5, 2, 4));
        helper.getLevel().setBlockAndUpdate(firstAnchor, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(secondAnchor, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(endAnchor, Blocks.STONE.defaultBlockState());

        use(first, firstAnchor, Direction.EAST);
        use(second, secondAnchor, Direction.EAST);
        long firstStart = first.getPersistentData().getCompound(WrenchLinePlacement.STATE_KEY).getLong("start");
        long secondStart = second.getPersistentData().getCompound(WrenchLinePlacement.STATE_KEY).getLong("start");
        helper.assertTrue(firstStart != secondStart, "Selections must be independent per player");

        BlockPos obstruction = firstAnchor.east(2);
        helper.getLevel().setBlockAndUpdate(obstruction, Blocks.OBSIDIAN.defaultBlockState());
        int before = first.getOffhandItem().getCount();
        use(first, endAnchor, Direction.WEST);
        helper.assertTrue(helper.getLevel().isEmptyBlock(firstAnchor.east()),
            "A blocked route must not leave a partial first segment");
        helper.assertTrue(helper.getLevel().getBlockState(obstruction).is(Blocks.OBSIDIAN),
            "A blocked route must retain its obstruction");
        helper.assertTrue(first.getOffhandItem().getCount() == before,
            "A blocked route must not consume blocks");
        helper.assertTrue(first.getPersistentData().contains(WrenchLinePlacement.STATE_KEY),
            "A blocked endpoint must keep the start for retrying");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void ordinaryBlockItemsAreEligible(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new GameProfile(UUID.randomUUID(), "wrench-line-glass"), ClientInformation.createDefault());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Blocks.GLASS, 2));
        BlockPos startAnchor = helper.absolutePos(new BlockPos(1, 4, 1));
        BlockPos endAnchor = helper.absolutePos(new BlockPos(4, 4, 1));
        BlockPos first = startAnchor.east();
        BlockPos second = endAnchor.west();
        helper.getLevel().setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(startAnchor, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(endAnchor, Blocks.STONE.defaultBlockState());

        use(player, startAnchor, Direction.EAST);
        use(player, endAnchor, Direction.WEST);

        helper.assertTrue(helper.getLevel().getBlockState(first).is(Blocks.GLASS)
                && helper.getLevel().getBlockState(second).is(Blocks.GLASS),
            "The wrench line placer must accept ordinary BlockItems, not only cables");
        helper.succeed();
    }

    private static ServerPlayer equippedPlayer(GameTestHelper helper, String name, int cableCount) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        player.setItemInHand(InteractionHand.OFF_HAND,
            new ItemStack(CrystalnexusModItems.BASIC_ENERGY_CABLE.get(), cableCount));
        return player;
    }

    private static void use(ServerPlayer player, BlockPos clicked, Direction face) {
        Vec3 location = Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5,
            face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(location, face, clicked, false);
        ItemStack wrench = player.getMainHandItem();
        wrench.getItem().onItemUseFirst(wrench,
            new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND, wrench, hit));
    }

    private static int countInventory(ServerPlayer player, ItemStack template) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, template)) count += stack.getCount();
        }
        return count;
    }
}
