package net.crystalnexus.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.WeakHashMap;

/** Validates world blocks against a Structure NBT while allowing explicit block substitutions. */
public final class StructureNbtValidator {
    private static final Map<StructureTemplate, ParsedTemplate> CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());

    private StructureNbtValidator() {
    }

    public record Match(BlockPos origin, Vec3 center, List<BlockPos> substitutionPositions,
                        Map<Block, List<BlockPos>> templatePositions) {
        public Match {
            substitutionPositions = List.copyOf(substitutionPositions);
            templatePositions = templatePositions.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        }

        public List<BlockPos> positionsFor(Block block) { return templatePositions.getOrDefault(block, List.of()); }
    }

    public record ValidationResult(Optional<Match> match, String message) {}

    public static Optional<Match> validate(ServerLevel level, ResourceLocation structureId,
                                           BlockPos controllerPos, Direction controllerFacing,
                                           Block controllerBlock, Property<Direction> facingProperty,
                                           Map<Block, Set<Block>> substitutions, boolean requireSubstitution,
                                           boolean centerMustBeAir) {
        return validate(level, structureId, controllerPos, controllerFacing, controllerBlock, facingProperty,
            substitutions, Set.of(), requireSubstitution, centerMustBeAir);
    }

    public static Optional<Match> validate(ServerLevel level, ResourceLocation structureId,
                                           BlockPos controllerPos, Direction controllerFacing,
                                           Block controllerBlock, Property<Direction> facingProperty,
                                           Map<Block, Set<Block>> substitutions, boolean requireSubstitution,
                                           boolean centerMustBeAir, Map<Block, Integer> maxSubstitutions) {
        return validate(level, structureId, controllerPos, controllerFacing, controllerBlock, facingProperty,
            substitutions, Set.of(), requireSubstitution, centerMustBeAir, maxSubstitutions);
    }

    public static Optional<Match> validate(ServerLevel level, ResourceLocation structureId,
                                           BlockPos controllerPos, Direction controllerFacing,
                                           Block controllerBlock, Property<Direction> facingProperty,
                                           Map<Block, Set<Block>> substitutions, Set<Block> stateAgnosticBlocks,
                                           boolean requireSubstitution, boolean centerMustBeAir) {
        return validate(level, structureId, controllerPos, controllerFacing, controllerBlock, facingProperty,
            substitutions, stateAgnosticBlocks, requireSubstitution, centerMustBeAir, Map.of());
    }

    public static Optional<Match> validate(ServerLevel level, ResourceLocation structureId,
                                           BlockPos controllerPos, Direction controllerFacing,
                                           Block controllerBlock, Property<Direction> facingProperty,
                                           Map<Block, Set<Block>> substitutions, Set<Block> stateAgnosticBlocks,
                                           boolean requireSubstitution, boolean centerMustBeAir,
                                           Map<Block, Integer> maxSubstitutions) {
        return validate(level, structureId, controllerPos, controllerFacing, controllerBlock, facingProperty,
            substitutions, stateAgnosticBlocks, requireSubstitution, centerMustBeAir, maxSubstitutions, false);
    }

    public static Optional<Match> validate(ServerLevel level, ResourceLocation structureId,
                                           BlockPos controllerPos, Direction controllerFacing,
                                           Block controllerBlock, Property<Direction> facingProperty,
                                           Map<Block, Set<Block>> substitutions, Set<Block> stateAgnosticBlocks,
                                           boolean requireSubstitution, boolean centerMustBeAir,
                                           Map<Block, Integer> maxSubstitutions, boolean ignoreBlockStates) {
        return validateDetailed(level, structureId, controllerPos, controllerFacing, controllerBlock, facingProperty,
            substitutions, stateAgnosticBlocks, requireSubstitution, centerMustBeAir, maxSubstitutions, ignoreBlockStates).match();
    }

    public static ValidationResult validateDetailed(ServerLevel level, ResourceLocation structureId,
                                                    BlockPos controllerPos, Direction controllerFacing,
                                                    Block controllerBlock, Property<Direction> facingProperty,
                                                    Map<Block, Set<Block>> substitutions, Set<Block> stateAgnosticBlocks,
                                                    boolean requireSubstitution, boolean centerMustBeAir,
                                                    Map<Block, Integer> maxSubstitutions, boolean ignoreBlockStates) {
        Optional<StructureTemplate> loaded = level.getStructureManager().get(structureId);
        if (loaded.isEmpty()) return failure("Template unavailable: " + structureId);

        StructureTemplate template = loaded.get();
        ParsedTemplate parsed;
        synchronized (CACHE) {
            parsed = CACHE.computeIfAbsent(template, key -> parse(key, level));
        }

        BlockPos anchor = null;
        Direction templateFacing = null;
        for (TemplateBlock entry : parsed.blocks) {
            BlockState state = entry.state;
            if (!state.is(controllerBlock)) continue;
            if (anchor != null || !state.hasProperty(facingProperty)) return failure("Template controller is invalid");
            anchor = entry.pos;
            templateFacing = state.getValue(facingProperty);
        }
        if (anchor == null || templateFacing == null) return failure("Template controller is missing");

        Rotation rotation = rotationBetween(templateFacing, controllerFacing);
        if (rotation == null) return failure("Controller facing is invalid");
        BlockPos transformedAnchor = StructureTemplate.transform(anchor, Mirror.NONE, rotation, BlockPos.ZERO);
        BlockPos origin = controllerPos.subtract(transformedAnchor);

        List<BlockPos> replacements = new ArrayList<>();
        Map<Block, List<BlockPos>> templatePositions = new HashMap<>();
        Map<Block, Integer> replacementCounts = new HashMap<>();
        int substitutionSlots = 0;
        for (TemplateBlock entry : parsed.rotated(rotation)) {
            BlockPos worldPos = origin.offset(entry.pos);
            templatePositions.computeIfAbsent(entry.state.getBlock(), ignored -> new ArrayList<>()).add(worldPos.immutable());
            BlockState actual = level.getBlockState(worldPos);
            Set<Block> replacementsForBlock = substitutions.get(entry.state.getBlock());
            if (replacementsForBlock != null) {
                substitutionSlots++;
                if (replacementsForBlock.stream().anyMatch(actual::is)) {
                    int count = replacementCounts.merge(actual.getBlock(), 1, Integer::sum);
                    if (count > maxSubstitutions.getOrDefault(actual.getBlock(), Integer.MAX_VALUE))
                        return failure("Too many " + blockName(actual.getBlock()));
                    replacements.add(worldPos.immutable());
                } else if (!((ignoreBlockStates || stateAgnosticBlocks.contains(entry.state.getBlock())) && actual.is(entry.state.getBlock()))
                    && !actual.equals(entry.state)) {
                    return mismatch(entry.state.getBlock(), actual.getBlock(), worldPos);
                }
            } else if (!((ignoreBlockStates || stateAgnosticBlocks.contains(entry.state.getBlock())) && actual.is(entry.state.getBlock()))
                && !actual.equals(entry.state)) {
                return mismatch(entry.state.getBlock(), actual.getBlock(), worldPos);
            }
        }
        if (requireSubstitution && replacements.isEmpty()) return failure("No multiblock port installed");
        if (substitutionSlots > 1 && replacements.size() == substitutionSlots)
            return failure("Leave one port slot as its template block");

        Vec3 center = StructureTemplate.transform(parsed.center, Mirror.NONE, rotation, BlockPos.ZERO)
            .add(origin.getX(), origin.getY(), origin.getZ());
        if (centerMustBeAir && !level.getBlockState(BlockPos.containing(center)).isAir()) return failure("Structure center must be air");
        return new ValidationResult(Optional.of(new Match(origin, center, replacements, templatePositions)), "Structure complete");
    }

    private static ValidationResult failure(String message) { return new ValidationResult(Optional.empty(), message); }

    private static ValidationResult mismatch(Block expected, Block actual, BlockPos pos) {
        return failure("Expected " + blockName(expected) + " at " + pos.toShortString() + ", found " + blockName(actual));
    }

    private static String blockName(Block block) { return BuiltInRegistries.BLOCK.getKey(block).getPath(); }

    private static ParsedTemplate parse(StructureTemplate template, ServerLevel level) {
        CompoundTag nbt = template.save(new CompoundTag());
        ListTag paletteTag = nbt.contains("palettes", Tag.TAG_LIST)
            ? nbt.getList("palettes", Tag.TAG_LIST).getList(0)
            : nbt.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        var blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(blockLookup, paletteTag.getCompound(i)));
        }

        ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
        List<TemplateBlock> parsedBlocks = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            parsedBlocks.add(new TemplateBlock(readPos(entry), palette.get(entry.getInt("state"))));
        }
        Vec3 rawCenter = new Vec3(template.getSize().getX() / 2.0D,
            template.getSize().getY() / 2.0D, template.getSize().getZ() / 2.0D);
        return new ParsedTemplate(rawCenter, parsedBlocks);
    }

    private record TemplateBlock(BlockPos pos, BlockState state) {}

    private static final class ParsedTemplate {
        private final Vec3 center;
        private final List<TemplateBlock> blocks;
        private final Map<Rotation, List<TemplateBlock>> rotations = new EnumMap<>(Rotation.class);

        private ParsedTemplate(Vec3 center, List<TemplateBlock> blocks) {
            this.center = center;
            this.blocks = List.copyOf(blocks);
        }

        private List<TemplateBlock> rotated(Rotation rotation) {
            return rotations.computeIfAbsent(rotation, key -> blocks.stream()
                .map(entry -> new TemplateBlock(
                    StructureTemplate.transform(entry.pos, Mirror.NONE, key, BlockPos.ZERO),
                    entry.state.rotate(key)))
                .toList());
        }
    }

    private static BlockPos readPos(CompoundTag entry) {
        ListTag pos = entry.getList("pos", Tag.TAG_INT);
        return new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
    }

    private static Rotation rotationBetween(Direction from, Direction to) {
        for (Rotation rotation : Rotation.values()) {
            if (rotation.rotate(from) == to) return rotation;
        }
        return null;
    }
}
