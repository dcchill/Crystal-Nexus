package net.crystalnexus.item;

import net.crystalnexus.block.ReactorInternalComponentBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

public class ReactorBlockItem extends BlockItem {
    public ReactorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        if (Screen.hasShiftDown()) {
            if (getBlock() instanceof ReactorInternalComponentBlock reactorBlock) {
                list.add(reactorBlock.getTooltipText());
            }
        }
    }
}