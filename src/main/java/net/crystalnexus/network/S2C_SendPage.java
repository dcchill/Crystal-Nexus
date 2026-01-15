package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record S2C_SendPage(
        List<Entry> entries,
        int upgradeLevel,
        long used,
        long capacity
) implements CustomPacketPayload {

    public static final Type<S2C_SendPage> TYPE =
            new Type<>(DepotNetIds.id("depot_send_page"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SendPage> STREAM_CODEC =
            StreamCodec.composite(
                    // item entries
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    S2C_SendPage::entries,

                    // upgrade level
                    ByteBufCodecs.VAR_INT,
                    S2C_SendPage::upgradeLevel,

                    // used storage
                    ByteBufCodecs.VAR_LONG,
                    S2C_SendPage::used,

                    // max capacity
                    ByteBufCodecs.VAR_LONG,
                    S2C_SendPage::capacity,

                    S2C_SendPage::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // =========================
    // Entry (single item row)
    // =========================
    public record Entry(ResourceLocation itemId, long count) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC,
                        Entry::itemId,

                        ByteBufCodecs.VAR_LONG,
                        Entry::count,

                        Entry::new
                );
    }
}
