package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2S_Withdraw(ResourceLocation itemId, int amount) implements CustomPacketPayload {
    public static final Type<C2S_Withdraw> TYPE = new Type<>(DepotNetIds.id("depot_withdraw"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_Withdraw> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, C2S_Withdraw::itemId,
                    ByteBufCodecs.VAR_INT,         C2S_Withdraw::amount,
                    C2S_Withdraw::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
