package net.crystalnexus.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;

public class LinkCardItem extends Item {
    public LinkCardItem(){super(new Properties());}
    @Override public boolean isFoil(ItemStack stack){return LinkCardData.read(stack)!=null;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal(LinkCardData.summary(stack)));lines.add(Component.literal("Use on a block to set its destination."));
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){return useOn(c);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null)return InteractionResult.PASS;
        var hit=new BlockHitResult(c.getClickLocation(),c.getClickedFace(),c.getClickedPos(),c.isInside());
        return LinkCardData.link(c.getLevel(),c.getPlayer(),c.getHand(),hit)?InteractionResult.sidedSuccess(c.getLevel().isClientSide):InteractionResult.PASS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var hit=level.clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getViewVector(1).scale(5)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        if(hit.getType()==HitResult.Type.BLOCK&&LinkCardData.link(level,player,hand,hit))return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
