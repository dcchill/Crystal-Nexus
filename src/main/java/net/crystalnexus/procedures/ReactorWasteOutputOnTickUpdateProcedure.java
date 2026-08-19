package net.crystalnexus.procedures;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class ReactorWasteOutputOnTickUpdateProcedure {
	private ReactorWasteOutputOnTickUpdateProcedure() {
	}

	public static void execute(LevelAccessor world, BlockPos portPos) {
		CenteredMultiblockValidator.Link controller = CenteredMultiblockValidator.validateFromPort(world, portPos,
				CrystalnexusModBlocks.REACTOR_CORE.get(), CrystalnexusModBlocks.REACTOR_COMPUTER.get(), BlocksCheckerProcedure.CASING);
		if (controller == null || !(world instanceof ILevelExtension ext)) {
			return;
		}
		IItemHandler source = ext.getCapability(Capabilities.ItemHandler.BLOCK, controller.pos, null);
		IItemHandler destination = ext.getCapability(Capabilities.ItemHandler.BLOCK, portPos, null);
		if (source == null || destination == null) {
			return;
		}
		ItemStack available = source.extractItem(2, 1, true);
		if (available.isEmpty() || !ItemHandlerHelper.insertItemStacked(destination, available, true).isEmpty()) {
			return;
		}
		ItemStack extracted = source.extractItem(2, 1, false);
		ItemHandlerHelper.insertItemStacked(destination, extracted, false);
	}
}