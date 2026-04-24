package net.crystalnexus.schematic;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.network.BuildgunMissingItemsMessage;
import net.crystalnexus.network.BuildgunUsageItemsMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class BuildgunSchematicManager {
	private static final String SELECTED = "buildgunSelectedSchematic";
	private static final String PLACEMENT_ACTIVE = "buildgunPlacementActive";
	private static final String PLACEMENT_DISTANCE = "buildgunPlacementDistance";
	private static final String PLACEMENT_ROTATION = "buildgunPlacementRotation";
	private static final String PLACEMENT_MODE = "buildgunPlacementMode";
	private static final String BUILDING_ACTIVE = "buildgunBuildingActive";
	private static final int STORAGE_RECURSION_LIMIT = 4;
	public static final String MODE_DEFAULT = "default";
	public static final String MODE_FLAT_GROUND = "flat_ground";

	private BuildgunSchematicManager() {
	}

	public static Path schematicDirectory() {
		return Paths.get("schematics").toAbsolutePath().normalize();
	}

	public static List<String> listSchematics() {
		Path dir = schematicDirectory();
		try {
			Files.createDirectories(dir);
			try (var stream = Files.list(dir)) {
				return stream
						.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".nbt"))
						.map(path -> path.getFileName().toString())
						.sorted(String.CASE_INSENSITIVE_ORDER)
						.toList();
			}
		} catch (IOException exception) {
			CrystalnexusMod.LOGGER.warn("Unable to list buildgun schematics", exception);
			return List.of();
		}
	}

	public static void selectSchematic(ServerPlayer player, ItemStack stack, String schematicName) {
		if (!isSafeSchematicName(schematicName)) {
			player.displayClientMessage(Component.literal("Invalid schematic name").withStyle(ChatFormatting.RED), true);
			return;
		}
		Path file = schematicDirectory().resolve(schematicName).normalize();
		if (!file.startsWith(schematicDirectory()) || !Files.isRegularFile(file)) {
			player.displayClientMessage(Component.literal("Schematic not found").withStyle(ChatFormatting.RED), true);
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putString(SELECTED, schematicName);
			tag.putBoolean(PLACEMENT_ACTIVE, false);
			tag.putBoolean(BUILDING_ACTIVE, false);
			tag.putInt(PLACEMENT_DISTANCE, 6);
			tag.putInt(PLACEMENT_ROTATION, 0);
			tag.putString(PLACEMENT_MODE, MODE_DEFAULT);
		});
		refreshModelState(stack);
		player.displayClientMessage(Component.literal("Selected schematic: " + schematicName).withStyle(ChatFormatting.AQUA), true);
	}

	public static void beginPlacement(ServerPlayer player, ItemStack stack) {
		String selected = getString(stack, SELECTED);
		if (selected.isBlank()) {
			player.displayClientMessage(Component.literal("Choose a schematic with the buildgun menu first").withStyle(ChatFormatting.RED), true);
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putBoolean(PLACEMENT_ACTIVE, true);
			tag.putBoolean(BUILDING_ACTIVE, false);
			if (!tag.contains(PLACEMENT_DISTANCE) || tag.getInt(PLACEMENT_DISTANCE) == 0) {
				tag.putInt(PLACEMENT_DISTANCE, 6);
			}
			if (!tag.contains(PLACEMENT_ROTATION)) {
				tag.putInt(PLACEMENT_ROTATION, 0);
			}
			if (!tag.contains(PLACEMENT_MODE)) {
				tag.putString(PLACEMENT_MODE, MODE_DEFAULT);
			}
		});
		refreshModelState(stack);
		try {
			SchematicData schematic = loadSchematic(player.serverLevel(), selected);
			showUsage(player, usageItems(schematic.blocks));
		} catch (IOException | RuntimeException exception) {
			player.displayClientMessage(Component.literal("Failed to load schematic").withStyle(ChatFormatting.RED), true);
			return;
		}
		player.displayClientMessage(Component.literal("Placement loaded: scroll to move, shift-scroll to rotate").withStyle(ChatFormatting.AQUA), true);
	}

	public static boolean isPlacementActive(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(PLACEMENT_ACTIVE);
	}

	public static void adjustPlacement(ServerPlayer player, ItemStack stack, int steps, boolean rotate) {
		if (!isPlacementActive(stack)) {
			return;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			if (rotate) {
				int next = Math.floorMod(tag.getInt(PLACEMENT_ROTATION) + steps, 4);
				tag.putInt(PLACEMENT_ROTATION, next);
			} else {
				tag.putInt(PLACEMENT_DISTANCE, Mth.clamp(tag.getInt(PLACEMENT_DISTANCE) + steps, -64, 64));
			}
		});
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		player.displayClientMessage(Component.literal("Distance " + tag.getInt(PLACEMENT_DISTANCE) + "  Rotation " + (tag.getInt(PLACEMENT_ROTATION) * 90) + " deg").withStyle(ChatFormatting.GRAY), true);
	}

	public static void togglePlacementMode(ServerPlayer player, ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			String current = normalizedMode(tag);
			tag.putString(PLACEMENT_MODE, MODE_FLAT_GROUND.equals(current) ? MODE_DEFAULT : MODE_FLAT_GROUND);
		});
		String mode = placementMode(stack);
		player.displayClientMessage(Component.literal("Buildgun mode: " + (MODE_FLAT_GROUND.equals(mode) ? "Prefer Flat Ground" : "Default")).withStyle(ChatFormatting.AQUA), true);
	}

	public static void tryBuild(ServerPlayer player, ItemStack stack) {
		String selected = getString(stack, SELECTED);
		if (selected.isBlank()) {
			player.displayClientMessage(Component.literal("No schematic selected").withStyle(ChatFormatting.RED), true);
			return;
		}

		SchematicData schematic;
		try {
			schematic = loadSchematic(player.serverLevel(), selected);
		} catch (IOException | RuntimeException exception) {
			player.displayClientMessage(Component.literal("Failed to load schematic").withStyle(ChatFormatting.RED), true);
			return;
		}

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		BlockPos origin = placementOrigin(player.serverLevel(), player.getEyePosition(), player.getLookAngle(), tag);
		int rotation = Math.floorMod(tag.getInt(PLACEMENT_ROTATION), 4);
		List<PlacedBlock> blocks = schematic.blocks.stream()
				.map(block -> block.rotated(rotation).at(origin))
				.filter(block -> !block.state.isAir() && !block.state.is(Blocks.STRUCTURE_VOID))
				.sorted(Comparator.comparingDouble(block -> block.pos.distSqr(player.blockPosition())))
				.toList();
		if (MODE_FLAT_GROUND.equals(normalizedMode(tag))) {
			blocks = settleBlocksToGround(player.serverLevel(), blocks);
		}

		BlockedPlacement blocked = findBlocked(player.serverLevel(), blocks);
		if (blocked != null) {
			player.displayClientMessage(Component.literal("Blocked by " + blocked.state.getBlock().getName().getString() + " at " + shortPos(blocked.pos)).withStyle(ChatFormatting.RED), true);
			return;
		}

		Map<Item, Integer> needed = requiredItems(player.serverLevel(), blocks);
		if (player.isCreative()) {
			animateBuild(player, stack, blocks);
			CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putBoolean(PLACEMENT_ACTIVE, false));
			refreshModelState(stack);
			player.displayClientMessage(Component.literal("Building " + selected).withStyle(ChatFormatting.GREEN), true);
			return;
		}
		Map<Item, Integer> missing = missingItems(player, needed);
		if (!missing.isEmpty()) {
			showMissing(player, missing);
			return;
		}

		consumeItems(player, needed);
		animateBuild(player, stack, blocks);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putBoolean(PLACEMENT_ACTIVE, false));
		refreshModelState(stack);
		player.displayClientMessage(Component.literal("Building " + selected).withStyle(ChatFormatting.GREEN), true);
	}

	private static SchematicData loadSchematic(ServerLevel level, String schematicName) throws IOException {
		if (!isSafeSchematicName(schematicName)) {
			throw new IOException("Invalid schematic name");
		}
		Path file = schematicDirectory().resolve(schematicName).normalize();
		if (!file.startsWith(schematicDirectory())) {
			throw new IOException("Invalid schematic path");
		}
		CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		ListTag paletteTag = root.getList("palette", Tag.TAG_COMPOUND);
		List<BlockState> palette = new ArrayList<>(paletteTag.size());
		for (int i = 0; i < paletteTag.size(); i++) {
			palette.add(NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), paletteTag.getCompound(i)));
		}

		ListTag blocksTag = root.getList("blocks", Tag.TAG_COMPOUND);
		List<SchematicBlock> blocks = new ArrayList<>(blocksTag.size());
		for (int i = 0; i < blocksTag.size(); i++) {
			CompoundTag blockTag = blocksTag.getCompound(i);
			ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
			BlockPos pos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
			BlockState state = palette.get(blockTag.getInt("state"));
			CompoundTag blockEntity = blockTag.contains("nbt", Tag.TAG_COMPOUND) ? blockTag.getCompound("nbt") : null;
			blocks.add(new SchematicBlock(pos, state, blockEntity));
		}
		return new SchematicData(blocks);
	}

	private static BlockedPlacement findBlocked(ServerLevel level, List<PlacedBlock> blocks) {
		for (PlacedBlock block : blocks) {
			BlockState existing = level.getBlockState(block.pos);
			if (!existing.isAir() && existing.getBlock() != block.state.getBlock() && !existing.canBeReplaced()) {
				return new BlockedPlacement(block.pos, existing);
			}
		}
		return null;
	}

	private static Map<Item, Integer> requiredItems(ServerLevel level, List<PlacedBlock> blocks) {
		Map<Item, Integer> required = new HashMap<>();
		for (PlacedBlock block : blocks) {
			if (level.getBlockState(block.pos).getBlock() == block.state.getBlock()) {
				continue;
			}
			Item item = block.state.getBlock().asItem();
			if (item != Items.AIR) {
				required.merge(item, 1, Integer::sum);
			}
		}
		return required;
	}

	private static Map<Item, Integer> usageItems(List<SchematicBlock> blocks) {
		Map<Item, Integer> required = new LinkedHashMap<>();
		for (SchematicBlock block : blocks) {
			if (block.state.isAir() || block.state.is(Blocks.STRUCTURE_VOID)) {
				continue;
			}
			Item item = block.state.getBlock().asItem();
			if (item != Items.AIR) {
				required.merge(item, 1, Integer::sum);
			}
		}
		return required;
	}

	private static Map<Item, Integer> missingItems(ServerPlayer player, Map<Item, Integer> needed) {
		Map<Item, Integer> missing = new LinkedHashMap<>();
		for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
			int available = countItem(player, entry.getKey());
			if (available < entry.getValue()) {
				missing.put(entry.getKey(), entry.getValue() - available);
			}
		}
		return missing;
	}

	private static int countItem(ServerPlayer player, Item item) {
		int count = 0;
		for (ItemStack stack : player.getInventory().items) {
			count += countItemInStack(stack, item, 0);
		}
		return count;
	}

	private static void consumeItems(ServerPlayer player, Map<Item, Integer> needed) {
		for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
			int remaining = entry.getValue();
			for (ItemStack stack : player.getInventory().items) {
				if (remaining <= 0) {
					break;
				}
				if (!stack.is(entry.getKey())) {
					continue;
				}
				int taken = Math.min(remaining, stack.getCount());
				stack.shrink(taken);
				remaining -= taken;
			}
			if (remaining > 0) {
				for (ItemStack stack : player.getInventory().items) {
					if (remaining <= 0) {
						break;
					}
					remaining = consumeFromContainedItems(stack, entry.getKey(), remaining, 0);
				}
			}
		}
		player.getInventory().setChanged();
	}

	private static void showMissing(ServerPlayer player, Map<Item, Integer> missing) {
		List<BuildgunMissingItemsMessage.Entry> entries = new ArrayList<>(missing.size());
		for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
			entries.add(new BuildgunMissingItemsMessage.Entry(entry.getKey(), entry.getValue()));
		}
		PacketDistributor.sendToPlayer(player, new BuildgunMissingItemsMessage(entries));
	}

	private static void showUsage(ServerPlayer player, Map<Item, Integer> usage) {
		List<BuildgunUsageItemsMessage.Entry> entries = new ArrayList<>(usage.size());
		for (Map.Entry<Item, Integer> entry : usage.entrySet()) {
			entries.add(new BuildgunUsageItemsMessage.Entry(entry.getKey(), entry.getValue()));
		}
		PacketDistributor.sendToPlayer(player, new BuildgunUsageItemsMessage(entries));
	}

	private static void animateBuild(ServerPlayer player, ItemStack stack, List<PlacedBlock> blocks) {
		ServerLevel level = player.serverLevel();
		int batchSize = 8;
		int totalBatches = Math.max(1, Mth.ceil((float) blocks.size() / batchSize));
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(BUILDING_ACTIVE, true));
		refreshModelState(stack);
		for (int i = 0; i < blocks.size(); i += batchSize) {
			int from = i;
			int to = Math.min(i + batchSize, blocks.size());
			CrystalnexusMod.queueServerWork(i / batchSize + 1, () -> {
				for (PlacedBlock block : blocks.subList(from, to)) {
					Vec3 start = player.position().add((level.random.nextDouble() - 0.5D) * 0.5D, 0.8D + level.random.nextDouble() * 0.35D, (level.random.nextDouble() - 0.5D) * 0.5D);
					Vec3 target = Vec3.atCenterOf(block.pos);
					Item item = block.state.getBlock().asItem();
					if (item != Items.AIR) {
						ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item));
						for (int step = 0; step < 5; step++) {
							double progress = step / 4.0D;
							Vec3 point = start.lerp(target, progress);
							level.sendParticles(particle, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
						}
					}
					level.setBlock(block.pos, block.state, Block.UPDATE_ALL);
					if (block.blockEntityTag != null) {
						BlockEntity blockEntity = level.getBlockEntity(block.pos);
						if (blockEntity != null) {
							CompoundTag nbt = sanitizedBlockEntityTag(block.blockEntityTag);
							nbt.putInt("x", block.pos.getX());
							nbt.putInt("y", block.pos.getY());
							nbt.putInt("z", block.pos.getZ());
							blockEntity.loadWithComponents(nbt, level.registryAccess());
							blockEntity.setChanged();
						}
					}
				}
			});
		}
		CrystalnexusMod.queueServerWork(totalBatches + 2, () -> {
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(BUILDING_ACTIVE, false));
			refreshModelState(stack);
		});
	}

	private static void refreshModelState(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		boolean active = tag.getBoolean(PLACEMENT_ACTIVE) || tag.getBoolean(BUILDING_ACTIVE);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(active ? 1 : 0));
	}

	public static String placementMode(ItemStack stack) {
		return normalizedMode(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
	}

	public static BlockPos placementOrigin(Level level, Vec3 eyePosition, Vec3 look, CompoundTag tag) {
		int distance = tag.getInt(PLACEMENT_DISTANCE);
		Vec3 target = eyePosition.add(look.scale(distance));
		String mode = normalizedMode(tag);
		if (MODE_FLAT_GROUND.equals(mode)) {
			return preferredFlatGroundPos(level, eyePosition, look, distance);
		}
		return BlockPos.containing(target);
	}

	private static BlockPos preferredFlatGroundPos(Level level, Vec3 eyePosition, Vec3 look, int distance) {
		double reach = Math.max(Math.abs(distance), 1);
		Vec3 clipEnd = eyePosition.add(look.scale(reach));
		BlockHitResult hit = level.clip(new ClipContext(eyePosition, clipEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
		if (hit.getType() != HitResult.Type.MISS) {
			return hit.getBlockPos().relative(hit.getDirection());
		}
		Vec3 target = eyePosition.add(look.scale(distance));
		return BlockPos.containing(target.x, 0.0D, target.z);
	}

	private static boolean isSafeSchematicName(String schematicName) {
		return schematicName != null && schematicName.endsWith(".nbt") && Objects.equals(Paths.get(schematicName).getFileName().toString(), schematicName);
	}

	private static String getString(ItemStack stack, String key) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(key);
	}

	private static String normalizedMode(CompoundTag tag) {
		return MODE_FLAT_GROUND.equals(tag.getString(PLACEMENT_MODE)) ? MODE_FLAT_GROUND : MODE_DEFAULT;
	}

	private static String shortPos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static int countItemInStack(ItemStack stack, Item item, int depth) {
		if (stack.isEmpty()) {
			return 0;
		}
		int count = stack.is(item) ? stack.getCount() : 0;
		if (depth >= STORAGE_RECURSION_LIMIT) {
			return count;
		}
		for (ItemStack contained : containedItems(stack)) {
			count += countItemInStack(contained, item, depth + 1);
		}
		return count;
	}

	private static int consumeFromContainedItems(ItemStack stack, Item item, int remaining, int depth) {
		if (remaining <= 0 || depth >= STORAGE_RECURSION_LIMIT) {
			return remaining;
		}
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null) {
			return remaining;
		}
		List<ItemStack> updated = new ArrayList<>();
		for (ItemStack contained : contents.stream().map(ItemStack::copy).toList()) {
			ItemStack mutable = contained.copy();
			if (remaining > 0 && mutable.is(item)) {
				int taken = Math.min(remaining, mutable.getCount());
				mutable.shrink(taken);
				remaining -= taken;
			}
			if (remaining > 0 && !mutable.isEmpty()) {
				remaining = consumeFromContainedItems(mutable, item, remaining, depth + 1);
			}
			updated.add(mutable);
		}
		stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(updated));
		return remaining;
	}

	private static Collection<ItemStack> containedItems(ItemStack stack) {
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		return contents == null ? List.of() : contents.stream().map(ItemStack::copy).toList();
	}

	private static CompoundTag sanitizedBlockEntityTag(CompoundTag original) {
		CompoundTag nbt = original.copy();
		nbt.remove("Items");
		return nbt;
	}

	private static List<PlacedBlock> settleBlocksToGround(ServerLevel level, List<PlacedBlock> blocks) {
		if (blocks.isEmpty()) {
			return blocks;
		}
		List<PlacedBlock> settled = blocks;
		int minY = level.getMinBuildHeight();
		int maxY = level.getMaxBuildHeight();

		for (int i = 0; i < maxY - minY && hasBlockingCollision(level, settled); i++) {
			settled = offsetBlocks(settled, 1);
		}
		for (int i = 0; i < maxY - minY && canMoveDown(level, settled); i++) {
			settled = offsetBlocks(settled, -1);
		}
		return settled;
	}

	private static boolean hasBlockingCollision(Level level, List<PlacedBlock> blocks) {
		for (PlacedBlock block : blocks) {
			BlockState existing = level.getBlockState(block.pos);
			if (!existing.isAir() && !existing.canBeReplaced()) {
				return true;
			}
		}
		return false;
	}

	private static boolean canMoveDown(Level level, List<PlacedBlock> blocks) {
		Set<BlockPos> occupied = blocks.stream().map(PlacedBlock::pos).collect(Collectors.toSet());
		for (PlacedBlock block : blocks) {
			BlockPos below = block.pos.below();
			if (below.getY() < level.getMinBuildHeight()) {
				return false;
			}
			if (occupied.contains(below)) {
				continue;
			}
			BlockState existing = level.getBlockState(below);
			if (!existing.isAir() && !existing.canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	private static List<PlacedBlock> offsetBlocks(List<PlacedBlock> blocks, int yOffset) {
		List<PlacedBlock> moved = new ArrayList<>(blocks.size());
		for (PlacedBlock block : blocks) {
			moved.add(new PlacedBlock(block.pos.offset(0, yOffset, 0), block.state, block.blockEntityTag));
		}
		return moved;
	}

	private record SchematicData(List<SchematicBlock> blocks) {
	}

	private record SchematicBlock(BlockPos pos, BlockState state, CompoundTag blockEntityTag) {
		private SchematicBlock rotated(int rotation) {
			return switch (Math.floorMod(rotation, 4)) {
				case 1 -> new SchematicBlock(new BlockPos(-pos.getZ(), pos.getY(), pos.getX()), rotateState(state, 1), blockEntityTag);
				case 2 -> new SchematicBlock(new BlockPos(-pos.getX(), pos.getY(), -pos.getZ()), rotateState(state, 2), blockEntityTag);
				case 3 -> new SchematicBlock(new BlockPos(pos.getZ(), pos.getY(), -pos.getX()), rotateState(state, 3), blockEntityTag);
				default -> this;
			};
		}

		private PlacedBlock at(BlockPos origin) {
			return new PlacedBlock(origin.offset(pos), state, blockEntityTag);
		}
	}

	private record PlacedBlock(BlockPos pos, BlockState state, CompoundTag blockEntityTag) {
	}

	private record BlockedPlacement(BlockPos pos, BlockState state) {
	}

	private static BlockState rotateState(BlockState state, int quarterTurns) {
		BlockState rotated = state;
		for (int i = 0; i < quarterTurns; i++) {
			rotated = rotated.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
		}
		return rotated;
	}
}
