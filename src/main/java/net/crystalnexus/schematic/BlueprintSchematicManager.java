package net.crystalnexus.schematic;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BlueprintSchematicManager {
	private BlueprintSchematicManager() {
	}

	public static void saveFromController(ServerPlayer player, BlockPos controllerPos, String requestedName) {
		ServerLevel level = player.serverLevel();
		BlueprintVolume volume = findVolume(level, controllerPos);
		if (volume.error != null) {
			player.displayClientMessage(Component.literal(volume.error).withStyle(ChatFormatting.RED), true);
			return;
		}

		String fileName = cleanFileName(requestedName);
		if (fileName.isBlank()) {
			player.displayClientMessage(Component.literal("Enter a blueprint name first").withStyle(ChatFormatting.RED), true);
			return;
		}

		try {
			String saved = saveInterior(level, volume, fileName);
			player.displayClientMessage(Component.literal("Saved blueprint " + saved).withStyle(ChatFormatting.GREEN), false);
		} catch (IOException | RuntimeException exception) {
			CrystalnexusMod.LOGGER.warn("Unable to save blueprint", exception);
			player.displayClientMessage(Component.literal("Failed to save blueprint: " + exception.getMessage()).withStyle(ChatFormatting.RED), true);
		}
	}

	private static BlueprintVolume findVolume(ServerLevel level, BlockPos controllerPos) {
		Set<BlockPos> structure = collectConnectedStructure(level, controllerPos);
		List<BlockPos> bases = structure.stream().filter(pos -> isBase(level.getBlockState(pos))).toList();
		List<BlockPos> frames = structure.stream().filter(pos -> isFrame(level.getBlockState(pos))).toList();
		if (bases.isEmpty()) {
			return BlueprintVolume.error("Controller must touch a blueprint base/frame structure");
		}
		if (frames.isEmpty()) {
			return BlueprintVolume.error("Blueprint frame is missing");
		}

		int floorY = bases.stream().mapToInt(BlockPos::getY).min().orElse(controllerPos.getY());
		if (bases.stream().anyMatch(pos -> pos.getY() != floorY)) {
			return BlueprintVolume.error("Blueprint base floor must be one flat layer");
		}
		int minX = bases.stream().mapToInt(BlockPos::getX).min().orElse(0);
		int maxX = bases.stream().mapToInt(BlockPos::getX).max().orElse(0);
		int minZ = bases.stream().mapToInt(BlockPos::getZ).min().orElse(0);
		int maxZ = bases.stream().mapToInt(BlockPos::getZ).max().orElse(0);
		if (maxX - minX < 2 || maxZ - minZ < 2) {
			return BlueprintVolume.error("Blueprint floor needs at least a 3x3 area");
		}

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (!isBase(level.getBlockState(new BlockPos(x, floorY, z)))) {
					return BlueprintVolume.error("Blueprint floor must be fully filled with Blueprint Base");
				}
			}
		}

		int topY = frames.stream().mapToInt(BlockPos::getY).max().orElse(floorY);
		if (topY <= floorY + 1) {
			return BlueprintVolume.error("Blueprint frame must be at least two blocks tall");
		}

		BlockPos[] corners = {
				new BlockPos(minX, 0, minZ),
				new BlockPos(minX, 0, maxZ),
				new BlockPos(maxX, 0, minZ),
				new BlockPos(maxX, 0, maxZ)
		};
		for (BlockPos corner : corners) {
			for (int y = floorY + 1; y <= topY; y++) {
				if (!isFrame(level.getBlockState(new BlockPos(corner.getX(), y, corner.getZ())))) {
					return BlueprintVolume.error("Blueprint frame needs four complete corner pillars");
				}
			}
		}

		for (int x = minX; x <= maxX; x++) {
			if (!isFrame(level.getBlockState(new BlockPos(x, topY, minZ))) || !isFrame(level.getBlockState(new BlockPos(x, topY, maxZ)))) {
				return BlueprintVolume.error("Blueprint frame must be connected across the top");
			}
		}
		for (int z = minZ; z <= maxZ; z++) {
			if (!isFrame(level.getBlockState(new BlockPos(minX, topY, z))) || !isFrame(level.getBlockState(new BlockPos(maxX, topY, z)))) {
				return BlueprintVolume.error("Blueprint frame must be connected across the top");
			}
		}

		return new BlueprintVolume(minX + 1, floorY + 1, minZ + 1, maxX - 1, topY - 1, maxZ - 1, null);
	}

	private static Set<BlockPos> collectConnectedStructure(ServerLevel level, BlockPos controllerPos) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(controllerPos);
		visited.add(controllerPos);
		while (!queue.isEmpty() && visited.size() < 8192) {
			BlockPos pos = queue.removeFirst();
			for (var direction : net.minecraft.core.Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (visited.contains(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (isBase(state) || isFrame(state) || isController(state)) {
					visited.add(next);
					queue.add(next);
				}
			}
		}
		return visited;
	}

	private static String saveInterior(ServerLevel level, BlueprintVolume volume, String baseName) throws IOException {
		Map<BlockState, Integer> paletteIndex = new LinkedHashMap<>();
		List<CompoundTag> blockTags = new ArrayList<>();
		BlockPos min = new BlockPos(volume.minX, volume.minY, volume.minZ);

		for (BlockPos worldPos : BlockPos.betweenClosed(volume.minX, volume.minY, volume.minZ, volume.maxX, volume.maxY, volume.maxZ)) {
			BlockState state = level.getBlockState(worldPos);
			if (state.isAir() || state.is(Blocks.STRUCTURE_VOID) || isBlueprintPart(state)) {
				continue;
			}
			int palette = paletteIndex.computeIfAbsent(state, ignored -> paletteIndex.size());
			CompoundTag blockTag = new CompoundTag();
			blockTag.put("pos", intList(worldPos.getX() - min.getX(), worldPos.getY() - min.getY(), worldPos.getZ() - min.getZ()));
			blockTag.putInt("state", palette);
			BlockEntity blockEntity = level.getBlockEntity(worldPos);
			if (blockEntity != null) {
				blockTag.put("nbt", blockEntity.saveWithFullMetadata(level.registryAccess()));
			}
			blockTags.add(blockTag);
		}

		CompoundTag root = new CompoundTag();
		root.put("size", intList(volume.maxX - volume.minX + 1, volume.maxY - volume.minY + 1, volume.maxZ - volume.minZ + 1));
		ListTag palette = new ListTag();
		for (BlockState state : paletteIndex.keySet()) {
			palette.add(NbtUtils.writeBlockState(state));
		}
		root.put("palette", palette);
		ListTag blocks = new ListTag();
		blockTags.forEach(blocks::add);
		root.put("blocks", blocks);

		Files.createDirectories(BuildgunSchematicManager.schematicDirectory());
		String fileName = uniqueFileName(baseName);
		NbtIo.writeCompressed(root, BuildgunSchematicManager.schematicDirectory().resolve(fileName));
		return fileName;
	}

	private static String cleanFileName(String name) {
		String cleaned = name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
		while (cleaned.startsWith(".")) {
			cleaned = cleaned.substring(1);
		}
		if (cleaned.endsWith(".nbt")) {
			cleaned = cleaned.substring(0, cleaned.length() - 4);
		}
		return cleaned;
	}

	private static String uniqueFileName(String baseName) throws IOException {
		Path dir = BuildgunSchematicManager.schematicDirectory();
		String candidate = baseName + ".nbt";
		int index = 2;
		while (Files.exists(dir.resolve(candidate))) {
			candidate = baseName + "_" + index + ".nbt";
			index++;
		}
		return candidate;
	}

	private static ListTag intList(int x, int y, int z) {
		ListTag tag = new ListTag();
		tag.add(IntTag.valueOf(x));
		tag.add(IntTag.valueOf(y));
		tag.add(IntTag.valueOf(z));
		return tag;
	}

	private static boolean isBase(BlockState state) {
		return state.is(CrystalnexusModBlocks.BLUEPRINT_BASE.get());
	}

	private static boolean isFrame(BlockState state) {
		return state.is(CrystalnexusModBlocks.BLUEPRINT_FRAME.get());
	}

	private static boolean isController(BlockState state) {
		return state.is(CrystalnexusModBlocks.BLUEPRINT_CONTROLLER.get());
	}

	private static boolean isBlueprintPart(BlockState state) {
		return isBase(state) || isFrame(state) || isController(state);
	}

	private record BlueprintVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String error) {
		private static BlueprintVolume error(String message) {
			return new BlueprintVolume(0, 0, 0, 0, 0, 0, message);
		}
	}
}
