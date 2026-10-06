package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.MultiblockPlansItem;
import net.crystalnexus.reactor.ReactorPlanner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record ReactorPlanSaveMessage(int size, int speed, byte[] columns) implements CustomPacketPayload {
    public static final Type<ReactorPlanSaveMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "reactor_plan_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorPlanSaveMessage> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> { buffer.writeVarInt(message.size); buffer.writeVarInt(message.speed); buffer.writeByteArray(message.columns); },
            buffer -> new ReactorPlanSaveMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray((ReactorPlanner.MAX_SIZE - 2) * (ReactorPlanner.MAX_SIZE - 2))));

    @Override public Type<ReactorPlanSaveMessage> type() { return TYPE; }

    public static void handleData(ReactorPlanSaveMessage message, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) save(player, message);
        });
    }

    public static void save(ServerPlayer player, ReactorPlanSaveMessage message) {
        ItemStack plans = player.getMainHandItem();
        if (!(plans.getItem() instanceof MultiblockPlansItem)) plans = player.getOffhandItem();
        if (!(plans.getItem() instanceof MultiblockPlansItem)) plans = player.getInventory().items.stream()
                .filter(stack -> stack.getItem() instanceof MultiblockPlansItem && !MultiblockPlansItem.hasGeneratedReactor(stack)
                        && MultiblockPlansItem.previewTemplate(stack) == null).findFirst().orElse(ItemStack.EMPTY);
        if (plans.isEmpty()) {
            player.displayClientMessage(Component.literal("Carry blank Multiblock Plans, or hold the plans you want to overwrite."), false);
            return;
        }
        try {
            if (message.speed < 0 || message.speed > 100) throw new IllegalArgumentException("Invalid speed");
            var build = ReactorPlanner.fromColumns(message.size, message.columns);
            if (!build.layout().valid || build.layout().activeCoolantChannels == 0)
                throw new IllegalArgumentException("Invalid reactor layout");
            MultiblockPlansItem.saveReactor(plans, build, message.speed);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.displayClientMessage(Component.literal("Reactor design copied to Multiblock Plans. Right-click a Reactor Computer to preview it."), false);
        } catch (IllegalArgumentException exception) {
            player.displayClientMessage(Component.literal("Could not copy this reactor design."), false);
        }
    }

    @SubscribeEvent public static void registerMessage(FMLCommonSetupEvent event) {
        CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, ReactorPlanSaveMessage::handleData);
    }
}
