package net.crystalnexus.network.payload;

import net.crystalnexus.world.inventory.HeavenshatterMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_CelestialTracking(int menuId, CompoundTag state) implements CustomPacketPayload {
    public static final Type<S2C_CelestialTracking> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:celestial_tracking"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_CelestialTracking> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> { buffer.writeVarInt(packet.menuId()); buffer.writeNbt(packet.state()); },
        buffer -> new S2C_CelestialTracking(buffer.readVarInt(), buffer.readNbt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(S2C_CelestialTracking packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.state() != null && context.player().containerMenu instanceof HeavenshatterMenu menu && menu.containerId == packet.menuId())
                menu.tracking = packet.state();
        });
    }
}
