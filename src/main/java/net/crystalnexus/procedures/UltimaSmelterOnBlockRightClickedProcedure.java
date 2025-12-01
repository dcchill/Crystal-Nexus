package net.crystalnexus.procedures;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.crystalnexus.world.inventory.UltimaSmelterGuiMenu;
import net.crystalnexus.init.CrystalnexusModBlocks;

import io.netty.buffer.Unpooled;

public class UltimaSmelterOnBlockRightClickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		BlockState core = Blocks.AIR.defaultBlockState();
		core = CrystalnexusModBlocks.MACHINE_CORE.get().defaultBlockState();
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == core.getBlock()) {
			MachineBlocksCheckerProcedure.execute(world, x + 1, y, z);
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == core.getBlock()) {
			MachineBlocksCheckerProcedure.execute(world, x - 1, y, z);
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == core.getBlock()) {
			BlocksCheckerProcedure.execute(world, x, y, z + 1);
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == core.getBlock()) {
			MachineBlocksCheckerProcedure.execute(world, x, y, z - 1);
		}
		if (getBlockNBTLogic(world, BlockPos.containing(x, y, z), "canOpenInventory")) {
			if (entity instanceof ServerPlayer _ent) {
				BlockPos _bpos = BlockPos.containing(x, y, z);
				_ent.openMenu(new MenuProvider() {
					@Override
					public Component getDisplayName() {
						return Component.literal("UltimaSmelterGui");
					}

					@Override
					public boolean shouldTriggerClientSideContainerClosingOnOpen() {
						return false;
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
						return new UltimaSmelterGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos));
					}
				}, _bpos);
			}
		}
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getBoolean(tag);
		return false;
	}
}