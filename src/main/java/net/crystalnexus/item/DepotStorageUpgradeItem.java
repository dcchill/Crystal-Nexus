package net.crystalnexus.item;

import net.crystalnexus.data.DepotSavedData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        DepotSavedData depot = DepotSavedData.get(serverLevel);

        // Apply upgrade
        depot.addUpgrade();

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
