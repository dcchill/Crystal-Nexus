package net.crystalnexus.client;

import net.crystalnexus.client.preview.ConveyorPreviewState;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.util.ConveyorSplinePlanner;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT)
public class ConveyorPreviewClientEvents {
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) {
            return;
        }

        if (!event.getEntity().isShiftKeyDown() || !event.getItemStack().is(CrystalnexusModItems.CONVEYER_BELT.get())) {
            return;
        }

        ConveyorPreviewState.togglePathMode();
        event.getEntity().displayClientMessage(
                Component.literal("Conveyor path mode: " + (ConveyorPreviewState.getPathMode() == ConveyorSplinePlanner.PathMode.SPLINE ? "Spline" : "Angled")),
                true
        );
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) {
            return;
        }

        if (!event.getItemStack().is(CrystalnexusModItems.CONVEYER_BELT.get())) {
            return;
        }

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!ConveyorSplinePlanner.isConveyorBlock(clickedState)) {
            return;
        }

        if (!event.getEntity().isShiftKeyDown()) {
            return;
        }

        BlockPos sourcePos = ConveyorPreviewState.getSource();
        if (sourcePos == null) {
            ConveyorPreviewState.setSource(clickedPos);
            return;
        }

        if (sourcePos.equals(clickedPos)) {
            ConveyorPreviewState.clear();
            return;
        }

        if (ConveyorSplinePlanner.plan(level, sourcePos, clickedPos, ConveyorPreviewState.getPathMode()) != null) {
            ConveyorPreviewState.clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (!mainHand.is(CrystalnexusModItems.CONVEYER_BELT.get()) && !minecraft.player.getOffhandItem().is(CrystalnexusModItems.CONVEYER_BELT.get())) {
            return;
        }

        BlockPos sourcePos = ConveyorPreviewState.getSource();
        if (sourcePos == null) {
            return;
        }

        if (!minecraft.level.isLoaded(sourcePos) || !ConveyorSplinePlanner.isConveyorBlock(minecraft.level.getBlockState(sourcePos))) {
            ConveyorPreviewState.clear();
        }
    }
}
