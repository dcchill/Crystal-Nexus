package net.crystalnexus.procedures;

import net.crystalnexus.item.LinkCardData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.HitResult;

public class LinkCardRightclickedOnBlockProcedure {
    public static boolean execute(LevelAccessor world,Entity entity){
        if(!(entity instanceof Player player)||!(world instanceof Level level))return false;
        var hit=level.clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getViewVector(1).scale(5)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        return hit.getType()==HitResult.Type.BLOCK&&LinkCardData.link(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
    }
}
