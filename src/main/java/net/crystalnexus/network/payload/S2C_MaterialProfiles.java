package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Server-authoritative datapack overrides used by the client-side generated recipe view. */
public record S2C_MaterialProfiles(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<S2C_MaterialProfiles> TYPE = new Type<>(DepotNetIds.id("material_profiles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_MaterialProfiles> STREAM_CODEC =
        StreamCodec.of(S2C_MaterialProfiles::write, S2C_MaterialProfiles::read);

    public record Entry(String source, MaterialProcessingCatalog.Profile profile) {}

    private static void write(RegistryFriendlyByteBuf buf, S2C_MaterialProfiles value) {
        buf.writeVarInt(value.entries.size());
        for (Entry entry : value.entries) {
            var profile = entry.profile;
            buf.writeUtf(entry.source, 128);
            buf.writeResourceLocation(profile.primaryMaterial());
            buf.writeResourceLocation(profile.reagent());
            buf.writeBoolean(profile.reagentTag());
            buf.writeVarInt(profile.reagentAmount());
            buf.writeVarInt(profile.crusherMultiplier());
            buf.writeVarInt(profile.advancedMultiplier());
            buf.writeBoolean(profile.secondary().isPresent());
            if (profile.secondary().isPresent()) {
                var secondary = profile.secondary().get();
                buf.writeResourceLocation(secondary.output().id());
                buf.writeBoolean(secondary.output().tag());
                buf.writeVarInt(secondary.output().count());
                buf.writeFloat(secondary.chance());
            }
            buf.writeVarInt(profile.disabledStages().size());
            profile.disabledStages().stream().sorted().forEach(stage -> buf.writeUtf(stage, 32));
        }
    }

    private static S2C_MaterialProfiles read(RegistryFriendlyByteBuf buf) {
        int size = Math.min(4096, buf.readVarInt());
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String source = buf.readUtf(128);
            ResourceLocation primary = buf.readResourceLocation();
            ResourceLocation reagent = buf.readResourceLocation();
            boolean reagentTag = buf.readBoolean();
            int reagentAmount = buf.readVarInt();
            int crusher = buf.readVarInt();
            int advanced = buf.readVarInt();
            Optional<MaterialProcessingCatalog.Secondary> secondary = Optional.empty();
            if (buf.readBoolean()) secondary = Optional.of(new MaterialProcessingCatalog.Secondary(
                new MaterialProcessingCatalog.Output(buf.readResourceLocation(), buf.readBoolean(), buf.readVarInt()),
                buf.readFloat()));
            int disabledSize = Math.min(16, buf.readVarInt());
            Set<String> disabled = new HashSet<>(disabledSize);
            for (int j = 0; j < disabledSize; j++) disabled.add(buf.readUtf(32));
            entries.add(new Entry(source, new MaterialProcessingCatalog.Profile(primary, reagent, reagentTag,
                reagentAmount, crusher, advanced, secondary, Set.copyOf(disabled))));
        }
        return new S2C_MaterialProfiles(List.copyOf(entries));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
