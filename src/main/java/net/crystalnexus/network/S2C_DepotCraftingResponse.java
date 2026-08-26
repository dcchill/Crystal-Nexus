package net.crystalnexus.network.payload;

import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.cli.DepotJeiRecipeCache;
import net.crystalnexus.network.DepotNetIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record S2C_DepotCraftingResponse(int menuId, Kind kind, boolean success, String message,
        DepotCraftingService.CatalogPage catalog, DepotCraftingService.Preview preview) implements CustomPacketPayload {
    public enum Kind { CATALOG, PREVIEW, RESULT }
    private static final DepotCraftingService.CatalogPage EMPTY_CATALOG =
            new DepotCraftingService.CatalogPage(List.of(), 0, 1);
    private static final DepotCraftingService.Preview EMPTY_PREVIEW = new DepotCraftingService.Preview(false, false,
            ResourceLocation.parse("minecraft:air"), 0, 0, 0, List.of(), List.of());
    public static final Type<S2C_DepotCraftingResponse> TYPE = new Type<>(DepotNetIds.id("depot_crafting_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_DepotCraftingResponse> STREAM_CODEC = StreamCodec.of(
            S2C_DepotCraftingResponse::write, S2C_DepotCraftingResponse::read);

    public static S2C_DepotCraftingResponse catalog(int menuId, DepotCraftingService.CatalogPage page) {
        return new S2C_DepotCraftingResponse(menuId, Kind.CATALOG, true, "", page, EMPTY_PREVIEW);
    }

    public static S2C_DepotCraftingResponse preview(int menuId, DepotCraftingService.Preview preview) {
        return new S2C_DepotCraftingResponse(menuId, Kind.PREVIEW, preview.success(), "", EMPTY_CATALOG, preview);
    }

    public static S2C_DepotCraftingResponse result(int menuId, boolean success, String message) {
        return new S2C_DepotCraftingResponse(menuId, Kind.RESULT, success, message, EMPTY_CATALOG, EMPTY_PREVIEW);
    }

    private static void write(RegistryFriendlyByteBuf buf, S2C_DepotCraftingResponse value) {
        buf.writeVarInt(value.menuId());
        buf.writeEnum(value.kind());
        buf.writeBoolean(value.success());
        buf.writeUtf(value.message(), 256);
        if (value.kind() == Kind.CATALOG) writeCatalog(buf, value.catalog());
        if (value.kind() == Kind.PREVIEW) writePreview(buf, value.preview());
    }

    private static S2C_DepotCraftingResponse read(RegistryFriendlyByteBuf buf) {
        int menuId = buf.readVarInt();
        Kind kind = buf.readEnum(Kind.class);
        boolean success = buf.readBoolean();
        String message = buf.readUtf(256);
        DepotCraftingService.CatalogPage catalog = kind == Kind.CATALOG ? readCatalog(buf) : EMPTY_CATALOG;
        DepotCraftingService.Preview preview = kind == Kind.PREVIEW ? readPreview(buf) : EMPTY_PREVIEW;
        return new S2C_DepotCraftingResponse(menuId, kind, success, message, catalog, preview);
    }

    private static void writeCatalog(RegistryFriendlyByteBuf buf, DepotCraftingService.CatalogPage page) {
        buf.writeVarInt(page.page());
        buf.writeVarInt(page.totalPages());
        buf.writeVarInt(page.entries().size());
        for (DepotCraftingService.CatalogEntry entry : page.entries()) {
            buf.writeResourceLocation(entry.itemId());
            buf.writeVarLong(entry.stored());
            buf.writeBoolean(entry.craftable());
        }
    }

    private static DepotCraftingService.CatalogPage readCatalog(RegistryFriendlyByteBuf buf) {
        int page = buf.readVarInt();
        int total = buf.readVarInt();
        int size = Math.min(12, buf.readVarInt());
        List<DepotCraftingService.CatalogEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) entries.add(new DepotCraftingService.CatalogEntry(
                buf.readResourceLocation(), buf.readVarLong(), buf.readBoolean()));
        return new DepotCraftingService.CatalogPage(List.copyOf(entries), page, total);
    }

    private static void writePreview(RegistryFriendlyByteBuf buf, DepotCraftingService.Preview preview) {
        buf.writeBoolean(preview.success());
        buf.writeBoolean(preview.startable());
        buf.writeResourceLocation(preview.targetId());
        buf.writeVarInt(preview.requested());
        buf.writeVarLong(preview.totalWork());
        buf.writeVarLong(preview.estimatedTicks());
        buf.writeVarInt(preview.nodes().size());
        for (DepotCraftingService.PreviewNode node : preview.nodes()) writeNode(buf, node);
        buf.writeVarInt(Math.min(8, preview.details().size()));
        preview.details().stream().limit(8).forEach(detail -> buf.writeUtf(detail, 256));
    }

    private static DepotCraftingService.Preview readPreview(RegistryFriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        boolean startable = buf.readBoolean();
        ResourceLocation target = buf.readResourceLocation();
        int requested = buf.readVarInt();
        long work = buf.readVarLong();
        long ticks = buf.readVarLong();
        int nodeCount = Math.min(64, buf.readVarInt());
        List<DepotCraftingService.PreviewNode> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) nodes.add(readNode(buf));
        int detailCount = Math.min(8, buf.readVarInt());
        List<String> details = new ArrayList<>(detailCount);
        for (int i = 0; i < detailCount; i++) details.add(buf.readUtf(256));
        return new DepotCraftingService.Preview(success, startable, target, requested, work, ticks,
                List.copyOf(nodes), List.copyOf(details));
    }

    private static void writeNode(RegistryFriendlyByteBuf buf, DepotCraftingService.PreviewNode node) {
        buf.writeVarInt(node.id());
        buf.writeInt(node.parentId());
        buf.writeResourceLocation(node.itemId());
        buf.writeVarLong(node.required());
        buf.writeVarLong(node.stored());
        buf.writeEnum(node.source());
        buf.writeBoolean(node.selectedRoute() != null);
        if (node.selectedRoute() != null) buf.writeResourceLocation(node.selectedRoute());
        buf.writeBoolean(node.selectedMachine() != null);
        if (node.selectedMachine() != null) buf.writeResourceLocation(node.selectedMachine());
        buf.writeVarInt(Math.min(DepotCraftingService.MAX_PREVIEW_CHOICES, node.alternatives().size()));
        node.alternatives().stream().limit(DepotCraftingService.MAX_PREVIEW_CHOICES)
                .forEach(choice -> writeChoice(buf, choice));
    }

    private static DepotCraftingService.PreviewNode readNode(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int parent = buf.readInt();
        ResourceLocation item = buf.readResourceLocation();
        long required = buf.readVarLong();
        long stored = buf.readVarLong();
        DepotCraftingService.PreviewSource source = buf.readEnum(DepotCraftingService.PreviewSource.class);
        ResourceLocation selected = buf.readBoolean() ? buf.readResourceLocation() : null;
        ResourceLocation machine = buf.readBoolean() ? buf.readResourceLocation() : null;
        int count = Math.min(DepotCraftingService.MAX_PREVIEW_CHOICES, buf.readVarInt());
        List<DepotCraftingService.RecipeChoice> choices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) choices.add(readChoice(buf));
        return new DepotCraftingService.PreviewNode(id, parent, item, required, stored, source, selected, machine, List.copyOf(choices));
    }

    private static void writeChoice(RegistryFriendlyByteBuf buf, DepotCraftingService.RecipeChoice choice) {
        buf.writeResourceLocation(choice.id());
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(choice.output().getItem()));
        buf.writeVarInt(choice.output().getCount());
        buf.writeBoolean(choice.processing());
        buf.writeUtf(choice.category(), 64);
        buf.writeVarInt(Math.min(9, choice.inputs().size()));
        for (DepotJeiRecipeCache.Slot slot : choice.inputs().stream().limit(9).toList()) {
            buf.writeVarInt(Math.min(4, slot.alternatives().size()));
            for (DepotJeiRecipeCache.StackRef stack : slot.alternatives().stream().limit(4).toList()) {
                buf.writeResourceLocation(stack.itemId());
                buf.writeVarInt(stack.count());
            }
        }
        buf.writeVarInt(Math.min(16, choice.machineTypes().size()));
        choice.machineTypes().stream().limit(16).forEach(buf::writeResourceLocation);
    }

    private static DepotCraftingService.RecipeChoice readChoice(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        ResourceLocation outputId = buf.readResourceLocation();
        int outputCount = buf.readVarInt();
        boolean processing = buf.readBoolean();
        String category = buf.readUtf(64);
        int inputCount = Math.min(9, buf.readVarInt());
        List<DepotJeiRecipeCache.Slot> inputs = new ArrayList<>(inputCount);
        for (int i = 0; i < inputCount; i++) {
            int alternativeCount = Math.min(4, buf.readVarInt());
            List<DepotJeiRecipeCache.StackRef> alternatives = new ArrayList<>(alternativeCount);
            for (int j = 0; j < alternativeCount; j++) alternatives.add(
                    new DepotJeiRecipeCache.StackRef(buf.readResourceLocation(), buf.readVarInt()));
            inputs.add(new DepotJeiRecipeCache.Slot(List.copyOf(alternatives)));
        }
        int machineCount = Math.min(16, buf.readVarInt());
        List<ResourceLocation> machines = new ArrayList<>(machineCount);
        for (int i = 0; i < machineCount; i++) machines.add(buf.readResourceLocation());
        return new DepotCraftingService.RecipeChoice(id,
                new ItemStack(BuiltInRegistries.ITEM.get(outputId), outputCount), processing, category,
                List.copyOf(inputs), List.copyOf(machines));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
