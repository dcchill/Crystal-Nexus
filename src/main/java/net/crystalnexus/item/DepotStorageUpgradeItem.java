package net.crystalnexus.item;

import net.crystalnexus.data.DepotSavedData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DepotStorageUpgradeItem extends Item {

    public DepotStorageUpgradeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Client: succeed instantly for responsiveness
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!DepotSavedData.requirePoweredController(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        DepotSavedData depot = DepotSavedData.get(serverPlayer);

        if (!depot.addUpgrade()) {
            player.displayClientMessage(
                    Component.literal("Dimensional Depot is already at the maximum upgrade level.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return InteractionResultHolder.consume(stack);
        }

        long cap = depot.getCapacity();

        // Consume item
        stack.shrink(1);

        // Feedback
        player.displayClientMessage(
                Component.literal("Dimensional Depot upgraded! New capacity: " + cap)
                        .withStyle(ChatFormatting.AQUA),
                true
        );

        return InteractionResultHolder.consume(stack);
    }
}
