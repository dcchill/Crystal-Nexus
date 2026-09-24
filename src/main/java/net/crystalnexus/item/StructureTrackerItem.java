package net.crystalnexus.item;

import com.mojang.datafixers.util.Pair;
import net.crystalnexus.data.MeteorTrackerSavedData;
import net.crystalnexus.network.StructureTrackerOptionsMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StructureTrackerItem extends Item {
	public static final String SELECTED_STRUCTURE = "structureTrackerSelected";
	private static final String TARGET_POS = "structureTrackerTargetPos";
	private static final String TARGET_DIMENSION = "structureTrackerTargetDimension";
	public static final ResourceLocation RESOURCE_METEOR = ResourceLocation.fromNamespaceAndPath("crystalnexus", "resource_meteor");
	public static final int SCAN_COST = 5_000;
	private static final int SEARCH_RADIUS_CHUNKS = 128;

	public StructureTrackerItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return ToolEnergy.barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x00FF00;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			return InteractionResultHolder.success(stack);
		}

		ServerPlayer serverPlayer = (ServerPlayer) player;
		String selected = selectedStructure(stack);
		if (player.isShiftKeyDown() || selected.isBlank()) {
			openSelection(serverPlayer, selected);
			return InteractionResultHolder.consume(stack);
		}

		locate(serverPlayer, stack, selected);
		return InteractionResultHolder.consume(stack);
	}

	public static String selectedStructure(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(SELECTED_STRUCTURE);
	}

	public static void select(ItemStack stack, ResourceLocation structureId) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putString(SELECTED_STRUCTURE, structureId.toString());
			tag.remove(TARGET_POS);
			tag.remove(TARGET_DIMENSION);
		});
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		if (!selected || !(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
			return;
		}
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (!tag.contains(TARGET_POS) || !tag.contains(TARGET_DIMENSION)) {
			return;
		}
		if (!serverLevel.dimension().location().toString().equals(tag.getString(TARGET_DIMENSION))) {
			player.displayClientMessage(Component.translatable("message.crystalnexus.structure_tracker.other_dimension").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		if (!ToolEnergy.consume(player, stack, 1, false)) {
			return;
		}

		BlockPos target = BlockPos.of(tag.getLong(TARGET_POS));
		double targetX = target.getX() + 0.5D - player.getX();
		double targetZ = target.getZ() + 0.5D - player.getZ();
		String compass = StructureTrackerCompass.bar(player.getLookAngle().x, player.getLookAngle().z, targetX, targetZ);
		int distance = (int) Math.round(Math.sqrt(player.blockPosition().distSqr(target)));
		ResourceLocation id = ResourceLocation.tryParse(tag.getString(SELECTED_STRUCTURE));
		String name = id == null ? "Target" : displayName(id);
		player.displayClientMessage(Component.literal(compass + "  " + name + "  " + distance + "m").withStyle(ChatFormatting.AQUA), true);
	}

	private static void openSelection(ServerPlayer player, String selected) {
		List<String> structures = new ArrayList<>(player.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
				.map(ResourceLocation::toString)
				.sorted()
				.toList());
		structures.add(0, RESOURCE_METEOR.toString());
		PacketDistributor.sendToPlayer(player, new StructureTrackerOptionsMessage(structures, selected));
	}

	private static void locate(ServerPlayer player, ItemStack stack, String selected) {
		ResourceLocation id = ResourceLocation.tryParse(selected);
		ServerLevel level = player.serverLevel();
		if (RESOURCE_METEOR.equals(id)) {
			locateMeteor(player, stack, level);
			return;
		}
		Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		Holder.Reference<Structure> structure = id == null
				? null
				: structures.getHolder(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
		if (structure == null) {
			player.displayClientMessage(Component.translatable("message.crystalnexus.structure_tracker.invalid").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (!hasScanEnergy(player, stack)) {
			return;
		}

		Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator().findNearestMapStructure(
				level, HolderSet.direct(structure), player.blockPosition(), SEARCH_RADIUS_CHUNKS, false);
		if (result == null) {
			player.displayClientMessage(Component.translatable("message.crystalnexus.structure_tracker.not_found").withStyle(ChatFormatting.YELLOW), true);
			return;
		}

		completeScan(player, stack, id, structureCenter(level, result.getFirst(), result.getSecond().value()));
	}

	private static BlockPos structureCenter(ServerLevel level, BlockPos locatePos, Structure structure) {
		ChunkPos chunkPos = new ChunkPos(locatePos);
		ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
		StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), structure, chunk);
		return start != null && start.isValid() ? start.getBoundingBox().getCenter() : locatePos;
	}

	private static void locateMeteor(ServerPlayer player, ItemStack stack, ServerLevel level) {
		if (!hasScanEnergy(player, stack)) {
			return;
		}
		BlockPos pos = MeteorTrackerSavedData.get(level).nearest(player.blockPosition());
		if (pos == null) {
			player.displayClientMessage(Component.translatable("message.crystalnexus.structure_tracker.meteor_not_found").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		completeScan(player, stack, RESOURCE_METEOR, pos);
	}

	private static boolean hasScanEnergy(ServerPlayer player, ItemStack stack) {
		if (ToolEnergy.consume(player, stack, SCAN_COST, true)) {
			return true;
		}
		player.displayClientMessage(Component.translatable("message.crystalnexus.structure_tracker.no_energy",
				String.format(Locale.ROOT, "%,d", SCAN_COST)).withStyle(ChatFormatting.RED), true);
		return false;
	}

	private static void completeScan(ServerPlayer player, ItemStack stack, ResourceLocation id, BlockPos pos) {
		ToolEnergy.consume(player, stack, SCAN_COST, false);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putLong(TARGET_POS, pos.asLong());
			tag.putString(TARGET_DIMENSION, player.level().dimension().location().toString());
		});
		int distance = (int) Math.round(Math.sqrt(player.blockPosition().distSqr(pos)));
		player.displayClientMessage(Component.translatable("message.crystalnexus.structure_tracker.found",
				displayName(id), pos.getX(), pos.getY(), pos.getZ(), distance).withStyle(ChatFormatting.AQUA), false);
		player.getCooldowns().addCooldown(stack.getItem(), 40);
	}

	public static String displayName(ResourceLocation id) {
		return StructureTrackerNames.displayName(id.getPath());
	}
}
