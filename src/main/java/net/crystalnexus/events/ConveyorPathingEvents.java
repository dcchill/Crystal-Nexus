package net.crystalnexus.events;

import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.util.ConveyorSplinePlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public class ConveyorPathingEvents {
    private static final String PATH_TAG = "crystalnexusConveyorPath";
    private static final String SOURCE_TAG = "source";
    private static final String MODE_TAG = "mode";

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !event.getItemStack().is(CrystalnexusModItems.CONVEYER_BELT.get())) {
            return;
        }

        ConveyorSplinePlanner.PathMode nextMode = getPathMode(player) == ConveyorSplinePlanner.PathMode.SPLINE
                ? ConveyorSplinePlanner.PathMode.ANGLED
                : ConveyorSplinePlanner.PathMode.SPLINE;
        setPathMode(player, nextMode);
        player.displayClientMessage(
                Component.literal("Conveyor path mode: " + (nextMode == ConveyorSplinePlanner.PathMode.SPLINE ? "Spline" : "Angled"))
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }

        ItemStack heldStack = event.getItemStack();
        if (!heldStack.is(CrystalnexusModItems.CONVEYER_BELT.get())) {
            return;
        }

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!ConveyorSplinePlanner.isConveyorBlock(clickedState)) {
            return;
        }

        BlockPos sourcePos = getSource(player);
        if (sourcePos == null) {
            setSource(player, clickedPos);
            player.displayClientMessage(
                    Component.literal("Conveyor source set to " + formatPos(clickedPos) + ". Shift-right-click another conveyor to connect.")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
            event.setCanceled(true);
            return;
        }

        if (sourcePos.equals(clickedPos)) {
            clearSource(player);
            player.displayClientMessage(
                    Component.literal("Conveyor connection cleared.").withStyle(ChatFormatting.YELLOW),
                    true
            );
            event.setCanceled(true);
            return;
        }

        if (!level.isLoaded(sourcePos) || !ConveyorSplinePlanner.isConveyorBlock(level.getBlockState(sourcePos))) {
            clearSource(player);
            player.displayClientMessage(
                    Component.literal("Stored conveyor source is no longer valid.").withStyle(ChatFormatting.RED),
                    true
            );
            event.setCanceled(true);
            return;
        }

        ConveyorSplinePlanner.ConnectionPlan plan = ConveyorSplinePlanner.plan(level, sourcePos, clickedPos, getPathMode(player));
        if (plan == null) {
            player.displayClientMessage(
                    Component.literal("No valid " + (getPathMode(player) == ConveyorSplinePlanner.PathMode.SPLINE ? "spline" : "angled") + " path found between those conveyors.").withStyle(ChatFormatting.RED),
                    true
            );
            event.setCanceled(true);
            return;
        }

        int beltsNeeded = 0;
        for (ConveyorSplinePlanner.PlacedBelt belt : plan.placedBelts()) {
            if (!ConveyorSplinePlanner.isConveyorBlock(level.getBlockState(belt.pos()))) {
                beltsNeeded++;
            }
        }

        if (!player.getAbilities().instabuild && heldStack.getCount() < beltsNeeded) {
            player.displayClientMessage(
                    Component.literal("Need " + beltsNeeded + " conveyor belts, but only have " + heldStack.getCount() + ".")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            event.setCanceled(true);
            return;
        }

        for (ConveyorSplinePlanner.PlacedBelt belt : plan.placedBelts()) {
            BlockState existing = level.getBlockState(belt.pos());
            if (!existing.canBeReplaced() && !ConveyorSplinePlanner.isConveyorBlock(existing)) {
                player.displayClientMessage(
                Component.literal("Spline path was blocked before placement finished.").withStyle(ChatFormatting.RED),
                        true
                );
                event.setCanceled(true);
                return;
            }
        }

        for (ConveyorSplinePlanner.PlacedBelt belt : plan.placedBelts()) {
            level.setBlock(belt.pos(), belt.state(), Block.UPDATE_ALL);
        }
        applySplineConnections(level, plan);

        if (!player.getAbilities().instabuild && beltsNeeded > 0) {
            heldStack.shrink(beltsNeeded);
        }

        clearSource(player);
        player.displayClientMessage(
                Component.literal("Connected conveyors with a " + (getPathMode(player) == ConveyorSplinePlanner.PathMode.SPLINE ? "spline" : "angled") + " path using " + beltsNeeded + " belt" + (beltsNeeded == 1 ? "" : "s") + ".")
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        event.setCanceled(true);
    }

    private static BlockPos getSource(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(PATH_TAG);
        if (!root.contains(SOURCE_TAG)) {
            return null;
        }
        return BlockPos.of(root.getLong(SOURCE_TAG));
    }

    private static void setSource(Player player, BlockPos sourcePos) {
        CompoundTag root = player.getPersistentData().getCompound(PATH_TAG);
        root.putLong(SOURCE_TAG, sourcePos.asLong());
        player.getPersistentData().put(PATH_TAG, root);
    }

    private static ConveyorSplinePlanner.PathMode getPathMode(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(PATH_TAG);
        if (!root.contains(MODE_TAG)) {
            return ConveyorSplinePlanner.PathMode.SPLINE;
        }
        try {
            return ConveyorSplinePlanner.PathMode.valueOf(root.getString(MODE_TAG));
        } catch (IllegalArgumentException ignored) {
            return ConveyorSplinePlanner.PathMode.SPLINE;
        }
    }

    private static void setPathMode(Player player, ConveyorSplinePlanner.PathMode mode) {
        CompoundTag root = player.getPersistentData().getCompound(PATH_TAG);
        root.putString(MODE_TAG, mode.name());
        player.getPersistentData().put(PATH_TAG, root);
    }

    private static void clearSource(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(PATH_TAG);
        root.remove(SOURCE_TAG);
        if (root.isEmpty()) {
            player.getPersistentData().remove(PATH_TAG);
        } else {
            player.getPersistentData().put(PATH_TAG, root);
        }
    }

    private static void applySplineConnections(Level level, ConveyorSplinePlanner.ConnectionPlan plan) {
        for (int i = 0; i < plan.cells().size(); i++) {
            BlockPos pos = plan.cells().get(i);
            if (!(level.getBlockEntity(pos) instanceof ConveyerBeltBaseBlockEntity beltEntity)) {
                continue;
            }

            BlockPos prevPos = i > 0 ? plan.cells().get(i - 1) : null;
            BlockPos nextPos = i + 1 < plan.cells().size() ? plan.cells().get(i + 1) : null;
            beltEntity.setSplineConnections(prevPos, nextPos);
        }
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
