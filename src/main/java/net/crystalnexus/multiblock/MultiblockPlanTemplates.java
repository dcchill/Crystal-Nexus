package net.crystalnexus.multiblock;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads the same NBT templates that multiblock controllers validate against. */
public final class MultiblockPlanTemplates {
    private static final Map<Block, ResourceLocation> TEMPLATES = Map.of(
        CrystalnexusModBlocks.ZERO_POINT.get(), id("zero_point"),
        CrystalnexusModBlocks.COMET_FORGE_CONTROLLER.get(), id("comet_forge"),
        CrystalnexusModBlocks.GRAVITATIONAL_ARRAY_CONTROLLER.get(), id("gravitational_array_new"),
        CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get(), id("solar_sim"),
        CrystalnexusModBlocks.SOLAR_ENGINE_CONTROLLER.get(), id("solar_engine"),
        CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get(), id("plasma_gen"),
        CrystalnexusModBlocks.DIESEL_GENERATOR_CONTROLLER.get(), id("diesel_generator"),
        CrystalnexusModBlocks.MEGA_CHEMICAL_REACTION_CHAMBER.get(), id("mega_chem_reactor"),
        CrystalnexusModBlocks.REACTOR_COMPUTER.get(), id("reactor"),
        CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER.get(), id("reaction")
    );

    private MultiblockPlanTemplates() {}

    public record PlanBlock(BlockPos pos, BlockState state) {}

    public static ResourceLocation templateFor(Block block) { return TEMPLATES.get(block); }

    public static List<PlanBlock> read(Level level, BlockPos controllerPos, ResourceLocation id) {
        BlockState controller = level.getBlockState(controllerPos);
        if (!id.equals(templateFor(controller.getBlock())) || !controller.hasProperty(HorizontalDirectionalBlock.FACING)) return List.of();
        CompoundTag root;
        String path = "data/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt";
        try (InputStream stream = MultiblockPlanTemplates.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return List.of();
            root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException ignored) {
            return List.of();
        }
        ListTag paletteTag = root.contains("palettes", Tag.TAG_LIST)
            ? root.getList("palettes", Tag.TAG_LIST).getList(0) : root.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        var lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < paletteTag.size(); i++) palette.add(NbtUtils.readBlockState(lookup, paletteTag.getCompound(i)));

        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        BlockPos anchor = null;
        Direction templateFacing = null;
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            BlockState state = palette.get(entry.getInt("state"));
            if (state.is(controller.getBlock()) && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                anchor = pos(entry);
                templateFacing = state.getValue(HorizontalDirectionalBlock.FACING);
                break;
            }
        }
        if (anchor == null) return List.of();
        Rotation rotation = rotationBetween(templateFacing, controller.getValue(HorizontalDirectionalBlock.FACING));
        BlockPos origin = controllerPos.subtract(transform(anchor, rotation));
        List<PlanBlock> result = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            BlockState state = palette.get(entry.getInt("state")).rotate(rotation);
            result.add(new PlanBlock(origin.offset(transform(pos(entry), rotation)), state));
        }
        return result;
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("crystalnexus", path); }
    private static BlockPos pos(CompoundTag entry) { ListTag p = entry.getList("pos", Tag.TAG_INT); return new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2)); }
    private static Rotation rotationBetween(Direction from, Direction to) {
        for (Rotation rotation : Rotation.values()) if (rotation.rotate(from) == to) return rotation;
        return Rotation.NONE;
    }
    private static BlockPos transform(BlockPos pos, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }
}
