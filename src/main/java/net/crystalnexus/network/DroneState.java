package net.crystalnexus.network;

import net.crystalnexus.world.inventory.DroneMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DroneState(int menu,CompoundTag data) implements CustomPacketPayload {
    public static final Type<DroneState> TYPE=new Type<>(ResourceLocation.parse("crystalnexus:drone_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DroneState> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeNbt(p.data);},b->new DroneState(b.readVarInt(),b.readNbt()));
    @Override public Type<DroneState> type(){return TYPE;}
    public static void handle(DroneState p,IPayloadContext context){context.enqueueWork(()->{if(context.player().containerMenu instanceof DroneMenu m && m.containerId==p.menu && p.data!=null)m.state=p.data;});}
}
