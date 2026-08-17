package net.crystalnexus.item;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ChlorophyteHammerItem extends Item {
    private static final int SMASH_DAMAGE = 5;

    public ChlorophyteHammerItem() {
        super(new Item.Properties().durability(512));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = stack.copyWithCount(1);
        remainder.setDamageValue(remainder.getDamageValue() + 1);
        return remainder.getDamageValue() >= remainder.getMaxDamage() ? ItemStack.EMPTY : remainder;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState smashedBlock = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (!isSmashable(smashedBlock) || player == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Block replacement = replacementFor(smashedBlock, level);

            level.setBlockAndUpdate(pos, replacement.defaultBlockState());
            level.levelEvent(2001, pos, Block.getId(smashedBlock));
            context.getItemInHand().hurtAndBreak(SMASH_DAMAGE, player, LivingEntity.getSlotForHand(context.getHand()));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isSmashable(BlockState state) {
        return state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.STONE)
                || state.is(Blocks.NETHERRACK);
    }

    private static Block replacementFor(BlockState state, Level level) {
        float roll = level.getRandom().nextFloat();

        if (state.is(Blocks.BLACKSTONE)) {
            return CrystalAlloyHammerRoll.createsNode(roll, 0.01F)
                    ? CrystalnexusModBlocks.ANCIENT_DEBRIS_NODE.get()
                    : CrystalnexusModBlocks.TARROCK.get();
        }
        if (state.is(Blocks.DEEPSLATE)) {
            return CrystalAlloyHammerRoll.createsNode(roll, 0.05F)
                    ? randomDeepslateNode(level.getRandom().nextInt(3))
                    : Blocks.COBBLED_DEEPSLATE;
        }
        if (state.is(Blocks.STONE)) {
            return CrystalAlloyHammerRoll.createsNode(roll, 0.01F)
                    ? CrystalnexusModBlocks.OIL_NODE.get()
                    : Blocks.COBBLESTONE;
        }
        return CrystalAlloyHammerRoll.createsNode(roll, 0.05F)
                ? CrystalnexusModBlocks.LAVA_NODE.get()
                : Blocks.NETHER_WART_BLOCK;
    }

    private static Block randomDeepslateNode(int index) {
        return switch (index) {
            case 0 -> CrystalnexusModBlocks.IRON_NODE.get();
            case 1 -> CrystalnexusModBlocks.GOLD_NODE.get();
            default -> CrystalnexusModBlocks.COPPER_NODE.get();
        };
    }
}
