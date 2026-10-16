package net.crystalnexus.item;

import net.crystalnexus.drone.DroneCommands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class DroneControllerItem extends Item {
    public DroneControllerItem(){super(new Properties().stacksTo(1));}
    public static void bind(ItemStack stack,UUID id){CustomData.update(DataComponents.CUSTOM_DATA,stack,p->p.putUUID("drone",id));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            DroneCommands.recall(server,player.isShiftKeyDown()||!tag.hasUUID("drone")?null:tag.getUUID("drone"),false);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.literal("Use on your drone to bind and program."));
        lines.add(net.minecraft.network.chat.Component.literal("Use: recall bound drone. Sneak-use: recall all."));
    }
}
