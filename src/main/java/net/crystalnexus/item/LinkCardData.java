package net.crystalnexus.item;

import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public final class LinkCardData {
    public record Destination(BlockPos pos,String dimension,Direction face,String block) {}
    private LinkCardData() {}
    public static Destination read(ItemStack stack) {
        CompoundTag p=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        for(String key:new String[]{"linkX","linkY","linkZ"})if(!p.contains(key,99)||!Double.isFinite(p.getDouble(key)))return null;
        double x=p.getDouble("linkX"),y=p.getDouble("linkY"),z=p.getDouble("linkZ");
        if(Math.abs(x)>29999984||Math.abs(z)>29999984||y < -2048||y>2048)return null;
        String dimension=p.getString("linkDimension");
        if(!dimension.isEmpty()&&ResourceLocation.tryParse(dimension)==null)return null;
        Direction face=Direction.byName(p.getString("linkFace"));
        return new Destination(BlockPos.containing(x,y,z),dimension,face==null?Direction.UP:face,p.getString("linkBlock"));
    }
    public static boolean link(Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        ItemStack stack=player.getItemInHand(hand);
        if(!stack.is(CrystalnexusModItems.LINK_CARD.get())||!level.hasChunkAt(hit.getBlockPos())||!level.isInWorldBounds(hit.getBlockPos()))return false;
        if(level.isClientSide)return true;
        BlockPos pos=hit.getBlockPos();
        CustomData.update(DataComponents.CUSTOM_DATA,stack,p->{
            p.putDouble("linkX",pos.getX());p.putDouble("linkY",pos.getY());p.putDouble("linkZ",pos.getZ());
            p.putString("linkDimension",level.dimension().location().toString());p.putString("linkFace",hit.getDirection().getName());
            p.putString("linkBlock",BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
        });
        player.displayClientMessage(Component.literal("Linked to "+name(stack)+" ("+pos.toShortString()+")"),true);return true;
    }
    public static String name(ItemStack stack) {
        if(stack.has(DataComponents.CUSTOM_NAME))return stack.getHoverName().getString();
        Destination link=read(stack);ResourceLocation block=link==null?null:ResourceLocation.tryParse(link.block());
        return block!=null&&BuiltInRegistries.BLOCK.containsKey(block)?BuiltInRegistries.BLOCK.get(block).getName().getString():"Link Card";
    }
    public static String summary(ItemStack stack) {
        Destination link=read(stack);if(link==null)return "Unlinked - use on a block";
        return link.pos().toShortString()+" | "+(link.dimension().isEmpty()?"Relink this card":link.dimension())+" | "+link.face().getName();
    }
}
