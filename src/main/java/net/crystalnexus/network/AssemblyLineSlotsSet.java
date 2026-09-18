package net.crystalnexus.network;

import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record AssemblyLineSlotsSet(int menu, int node, List<String> inputs, List<String> outputs,
                                   List<String> inputFluids, List<String> outputFluids)
        implements CustomPacketPayload {
    public static final Type<AssemblyLineSlotsSet> TYPE =
            new Type<>(ResourceLocation.parse("crystalnexus:assembly_slots_set"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyLineSlotsSet> STREAM_CODEC =
            StreamCodec.of(AssemblyLineSlotsSet::encode, AssemblyLineSlotsSet::decode);

    private static void encode(RegistryFriendlyByteBuf buf, AssemblyLineSlotsSet packet) {
        buf.writeVarInt(packet.menu);
        buf.writeVarInt(packet.node);
        write(buf, packet.inputs);
        write(buf, packet.outputs);
        write(buf, packet.inputFluids);
        write(buf, packet.outputFluids);
    }

    private static AssemblyLineSlotsSet decode(RegistryFriendlyByteBuf buf) {
        return new AssemblyLineSlotsSet(buf.readVarInt(), buf.readVarInt(), read(buf), read(buf), read(buf), read(buf));
    }

    private static void write(RegistryFriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(Math.min(32, values.size()));
        values.stream().limit(32).forEach(value -> buf.writeUtf(value == null ? "" : value, 256));
    }

    private static List<String> read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 32) throw new IllegalArgumentException("Invalid assembly slot count");
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) values.add(buf.readUtf(256));
        return values;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AssemblyLineSlotsSet packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof AssemblyLineMenu menu)
                    || menu.containerId != packet.menu || !menu.stillValid(context.player())) return;
            menu.controller.setGraphSlots(packet.node, packet.inputs, packet.outputs, packet.inputFluids, packet.outputFluids);
        });
    }
}
