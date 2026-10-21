package net.crystalnexus.network;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.crystalnexus.item.CrystalWrenchItem;
import net.crystalnexus.util.MachineInspection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "crystalnexus", bus = EventBusSubscriber.Bus.MOD)
public final class MachineInspectionNetwork {
    private MachineInspectionNetwork() {}
    private static final Map<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();
    // Assigned only by the client subscriber; no client classes are loaded by the server.
    public static Consumer<Response> receiver = response -> {};
    public static Consumer<DroneResponse> droneReceiver = response -> {};
    public record Request(ResourceLocation dimension, BlockPos pos, long sequence) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:inspect_machine"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> {
            b.writeResourceLocation(p.dimension); b.writeBlockPos(p.pos); b.writeLong(p.sequence);
        }, b -> new Request(b.readResourceLocation(), b.readBlockPos(), b.readLong()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(ResourceLocation dimension, BlockPos pos, long sequence, ResourceLocation block,
            MachineInspection snapshot) implements CustomPacketPayload {
        public static final Type<Response> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:machine_inspection"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(Response::write, Response::read);
        @Override public Type<Response> type() { return TYPE; }
        public boolean matches(ResourceLocation expectedDimension, BlockPos expectedPos, ResourceLocation expectedBlock,
                long accepted, long sent) {
            return dimension.equals(expectedDimension) && pos.equals(expectedPos) && block.equals(expectedBlock)
                && sequence > accepted && sequence <= sent;
        }
        private static void write(RegistryFriendlyByteBuf b, Response p) {
            b.writeResourceLocation(p.dimension); b.writeBlockPos(p.pos); b.writeLong(p.sequence); b.writeResourceLocation(p.block);
            writeSnapshot(b, p.snapshot);
        }
        private static Response read(RegistryFriendlyByteBuf b) {
            var dimension = b.readResourceLocation(); var pos = b.readBlockPos(); long sequence = b.readLong(); var block = b.readResourceLocation();
            return new Response(dimension, pos, sequence, block, readSnapshot(b));
        }
    }
    public record DroneRequest(ResourceLocation dimension, int entityId, UUID uuid, long sequence) implements CustomPacketPayload {
        public static final Type<DroneRequest> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:inspect_drone"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DroneRequest> CODEC = StreamCodec.of((b, p) -> {
            b.writeResourceLocation(p.dimension); b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeLong(p.sequence);
        }, b -> new DroneRequest(b.readResourceLocation(), b.readVarInt(), b.readUUID(), b.readLong()));
        @Override public Type<DroneRequest> type() { return TYPE; }
    }
    public record DroneResponse(ResourceLocation dimension, int entityId, UUID uuid, long sequence,
            MachineInspection snapshot) implements CustomPacketPayload {
        public static final Type<DroneResponse> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:drone_inspection"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DroneResponse> CODEC = StreamCodec.of((b, p) -> {
            b.writeResourceLocation(p.dimension); b.writeVarInt(p.entityId); b.writeUUID(p.uuid); b.writeLong(p.sequence);
            writeSnapshot(b, p.snapshot);
        }, b -> new DroneResponse(b.readResourceLocation(), b.readVarInt(), b.readUUID(), b.readLong(), readSnapshot(b)));
        @Override public Type<DroneResponse> type() { return TYPE; }
        public boolean matches(ResourceLocation expectedDimension, int expectedId, UUID expectedUuid, long accepted, long sent) {
            return dimension.equals(expectedDimension) && entityId == expectedId && uuid.equals(expectedUuid)
                && sequence > accepted && sequence <= sent;
        }
    }
    private static void writeSnapshot(RegistryFriendlyByteBuf b, MachineInspection snapshot) {
        b.writeBoolean(snapshot != null);
        if (snapshot == null) return;
        var s = snapshot;
        ComponentSerialization.STREAM_CODEC.encode(b, s.name()); b.writeInt(s.tier());
        b.writeBoolean(s.inventory()); b.writeVarInt(s.items().size());
        for (var item : s.items()) { ItemStack.STREAM_CODEC.encode(b, item.icon()); b.writeVarInt(item.count()); }
        b.writeVarInt(s.extraItems()); b.writeBoolean(s.energy()); b.writeLong(s.storedEnergy()); b.writeLong(s.energyCapacity());
        b.writeBoolean(s.fluids()); b.writeVarInt(s.tanks().size());
        for (var tank : s.tanks()) {
            ComponentSerialization.STREAM_CODEC.encode(b, tank.name()); b.writeVarInt(tank.amount()); b.writeVarInt(tank.capacity());
        }
        b.writeVarInt(s.extraTanks()); b.writeDouble(s.progress());
        b.writeVarInt(s.wrenchStatus().size());
        for (var line : s.wrenchStatus()) ComponentSerialization.STREAM_CODEC.encode(b, line);
    }
    private static MachineInspection readSnapshot(RegistryFriendlyByteBuf b) {
        if (!b.readBoolean()) return null;
        var name = ComponentSerialization.STREAM_CODEC.decode(b); int tier = b.readInt(); boolean inventory = b.readBoolean();
        int count = boundedCount(b.readVarInt(), MachineInspection.MAX_ITEMS);
        var items = new ArrayList<MachineInspection.ItemEntry>(count);
        for (int i = 0; i < count; i++) items.add(new MachineInspection.ItemEntry(ItemStack.STREAM_CODEC.decode(b), b.readVarInt()));
        int extraItems = b.readVarInt(); boolean energy = b.readBoolean(); long stored = b.readLong(), capacity = b.readLong();
        boolean fluids = b.readBoolean(); count = boundedCount(b.readVarInt(), MachineInspection.MAX_TANKS);
        var tanks = new ArrayList<MachineInspection.FluidEntry>(count);
        for (int i = 0; i < count; i++) tanks.add(new MachineInspection.FluidEntry(ComponentSerialization.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt()));
        int extraTanks = b.readVarInt(); double progress = b.readDouble();
        count = boundedCount(b.readVarInt(), MachineInspection.MAX_WRENCH_LINES);
        var wrenchStatus = new ArrayList<net.minecraft.network.chat.Component>(count);
        for (int i = 0; i < count; i++) wrenchStatus.add(ComponentSerialization.STREAM_CODEC.decode(b));
        return new MachineInspection(name, tier, inventory, items, extraItems,
            energy, stored, capacity, fluids, tanks, extraTanks, progress, java.util.List.copyOf(wrenchStatus));
    }
    static int boundedCount(int count, int maximum) {
        if (count < 0 || count > maximum) throw new IllegalArgumentException("Invalid inspection entry count");
        return count;
    }
    public static boolean ready(long now, Long previous) { return previous == null || now - previous >= 5; }
    public static boolean fresh(long now, long received) { return now >= received && now - received < 20; }
    public static boolean canInspect(ServerPlayer player, Request request) {
        if (!(player.getMainHandItem().getItem() instanceof CrystalWrenchItem)
                && !(player.getOffhandItem().getItem() instanceof CrystalWrenchItem)) return false;
        var level = player.serverLevel();
        if (!level.dimension().location().equals(request.dimension) || !level.hasChunkAt(request.pos)) return false;
        var hit = player.pick(player.blockInteractionRange(), 1, false);
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(request.pos);
    }

    public static boolean canInspect(ServerPlayer player, DroneRequest request) {
        if (!(player.getMainHandItem().getItem() instanceof CrystalWrenchItem)
                && !(player.getOffhandItem().getItem() instanceof CrystalWrenchItem)) return false;
        var level = player.serverLevel();
        if (!level.dimension().location().equals(request.dimension)) return false;
        var entity = level.getEntity(request.entityId);
        if (!(entity instanceof LUNADroneEntity drone) || !drone.isAlive() || !drone.getUUID().equals(request.uuid)) return false;
        double range = player.entityInteractionRange();
        var start = player.getEyePosition();
        var blockHit = player.pick(range, 1, false);
        double distance = blockHit.getType() == HitResult.Type.MISS ? range * range : start.distanceToSqr(blockHit.getLocation());
        var direction = player.getViewVector(1).scale(range);
        var hit = ProjectileUtil.getEntityHitResult(player, start, start.add(direction),
            player.getBoundingBox().expandTowards(direction).inflate(1),
            candidate -> !candidate.isSpectator() && candidate.isPickable(), distance);
        return hit != null && hit.getEntity() == drone && start.distanceToSqr(hit.getLocation()) < distance;
    }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            long now = player.server.getTickCount();
            if (!ready(now, LAST_REQUEST.get(player))) return;
            LAST_REQUEST.put(player, now);
            if (!canInspect(player, request)) return;
            var level = player.serverLevel();
            PacketDistributor.sendToPlayer(player, new Response(request.dimension, request.pos, request.sequence,
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(request.pos).getBlock()), MachineInspection.read(level, request.pos)));
        }));
        registrar.playToServer(DroneRequest.TYPE, DroneRequest.CODEC, (request, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            long now = player.server.getTickCount();
            if (!ready(now, LAST_REQUEST.get(player))) return;
            LAST_REQUEST.put(player, now);
            if (!canInspect(player, request)) return;
            var drone = (LUNADroneEntity) player.serverLevel().getEntity(request.entityId);
            PacketDistributor.sendToPlayer(player, new DroneResponse(request.dimension, request.entityId, request.uuid,
                request.sequence, MachineInspection.read(drone, player)));
        }));
        registrar.playToClient(DroneResponse.TYPE, DroneResponse.CODEC,
            (response, context) -> context.enqueueWork(() -> droneReceiver.accept(response)));
        registrar.playToClient(Response.TYPE, Response.CODEC, (response, context) -> context.enqueueWork(() -> receiver.accept(response)));
    }
}
