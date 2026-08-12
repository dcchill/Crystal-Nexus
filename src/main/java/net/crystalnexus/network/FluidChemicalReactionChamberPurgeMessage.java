package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.world.inventory.FluidChemicalReactionChamberGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record FluidChemicalReactionChamberPurgeMessage(int tank, BlockPos pos) implements CustomPacketPayload {
    public static final Type<FluidChemicalReactionChamberPurgeMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "fluid_chemical_reaction_chamber_purge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidChemicalReactionChamberPurgeMessage> STREAM_CODEC = StreamCodec.of(
        (buffer, message) -> { buffer.writeVarInt(message.tank); buffer.writeBlockPos(message.pos); },
        buffer -> new FluidChemicalReactionChamberPurgeMessage(buffer.readVarInt(), buffer.readBlockPos()));

    @Override public Type<FluidChemicalReactionChamberPurgeMessage> type() { return TYPE; }

    public static void handle(FluidChemicalReactionChamberPurgeMessage message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) return;
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof FluidChemicalReactionChamberGUIMenu menu)
                || menu.x != message.pos.getX() || menu.y != message.pos.getY() || menu.z != message.pos.getZ()
                || message.tank < 0 || message.tank > 2) return;
            if (context.player().level().getBlockEntity(message.pos) instanceof FluidChemicalReactionChamberBlockEntity chamber)
                chamber.purge(message.tank);
        }).exceptionally(error -> {
            context.connection().disconnect(Component.literal(error.getMessage()));
            return null;
        });
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, FluidChemicalReactionChamberPurgeMessage::handle);
    }
}
