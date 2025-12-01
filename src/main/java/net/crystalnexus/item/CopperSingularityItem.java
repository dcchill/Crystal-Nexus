package net.crystalnexus.item;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

import net.crystalnexus.procedures.CopperSingularityRightclickedOnBlockProcedure;

import java.util.List;

public class CopperSingularityItem extends Item {
	public CopperSingularityItem() {
		super(new Item.Properties().rarity(Rarity.RARE));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("item.crystalnexus.copper_singularity.description_0"));
		list.add(Component.translatable("item.crystalnexus.copper_singularity.description_1"));
	}

	@Override
public InteractionResult useOn(UseOnContext context) {
    LevelAccessor world = context.getLevel();
    BlockPos pos = context.getClickedPos();

    boolean reverse = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
    CopperSingularityRightclickedOnBlockProcedure.execute(world, pos, reverse);

    return InteractionResult.SUCCESS;
}
}