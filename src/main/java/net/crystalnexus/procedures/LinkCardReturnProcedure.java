package net.crystalnexus.procedures;
import net.minecraft.world.item.ItemStack;
import net.crystalnexus.item.LinkCardData;
public class LinkCardReturnProcedure {
    public static String execute(ItemStack stack){return LinkCardData.summary(stack);}
}
