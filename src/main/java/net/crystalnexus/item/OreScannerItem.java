package net.crystalnexus.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import net.crystalnexus.procedures.OreScannerRightclickedProcedure;

public class OreScannerItem extends Item {

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) { return ToolEnergy.barWidth(stack); }

    @Override
    public int getBarColor(ItemStack stack) { return 0x00FF00; }
	public OreScannerItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		OreScannerRightclickedProcedure.execute(world, entity, entity.getItemInHand(hand));
		return ar;
	}
}