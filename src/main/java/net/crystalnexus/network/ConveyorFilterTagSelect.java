package net.crystalnexus.network;

import net.crystalnexus.world.inventory.ConveyorFilterMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import javax.annotation.Nullable;

public record ConveyorFilterTagSelect(int menu, int slot, @Nullable ResourceLocation tag) implements CustomPacketPayload {
    public static final Type<ConveyorFilterTagSelect> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:conveyor_filter_tag"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConveyorFilterTagSelect> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            buffer.writeVarInt(value.menu);
            buffer.writeVarInt(value.slot);
            buffer.writeNullable(value.tag, (buf, tag) -> buf.writeResourceLocation(tag));
        }, buffer -> new ConveyorFilterTagSelect(buffer.readVarInt(), buffer.readVarInt(),
            buffer.readNullable(buf -> buf.readResourceLocation())));

    @Override public Type<ConveyorFilterTagSelect> type() { return TYPE; }

    public static void handle(ConveyorFilterTagSelect request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ConveyorFilterMenu menu)
                menu.selectTag(context.player(), request.menu, request.slot, request.tag);
        });
    }
}
