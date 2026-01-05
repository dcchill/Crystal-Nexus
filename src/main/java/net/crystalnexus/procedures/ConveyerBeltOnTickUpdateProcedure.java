package net.crystalnexus.procedures;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;

import net.crystalnexus.init.CrystalnexusModBlocks;

import java.util.List;

public class ConveyerBeltOnTickUpdateProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof Level level)) return;
        if (level.isClientSide) return;

        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction back = facing.getOpposite();

        boolean isOutputBelt = state.getBlock() == CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get();
        boolean isInputBelt  = state.getBlock() == CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get();

        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double minY = pos.getY() + 0.55;
        double maxY = pos.getY() + 0.8;

        /* ===================================================
         * 1️⃣ PULL ITEMS FROM STORAGE IF THIS IS AN OUTPUT BELT
         * =================================================== */
        if (isOutputBelt) {
            BlockPos inputPos = pos.relative(back);
            IItemHandler inputHandler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    inputPos,
                    facing
            );

            if (inputHandler != null) {
                for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
                    ItemStack extracted = inputHandler.extractItem(slot, 1, true);
                    if (!extracted.isEmpty()) {
                        inputHandler.extractItem(slot, 1, false);

                        // Try to merge with existing items on belt first
                        boolean merged = false;
                        AABB beltBox = new AABB(centerX - 0.4, minY, centerZ - 0.4,
                                                centerX + 0.4, maxY, centerZ + 0.4);
                        List<ItemEntity> existing = level.getEntitiesOfClass(ItemEntity.class, beltBox);

                        for (ItemEntity entity : existing) {
                            if (!entity.isAlive()) continue;
                            ItemStack stack = entity.getItem();

                            // ✅ Merge by item type only
                            if (stack.getItem() == extracted.getItem()) {
                                int availableSpace = stack.getMaxStackSize() - stack.getCount();
                                int toAdd = Math.min(extracted.getCount(), availableSpace);
                                stack.grow(toAdd);
                                extracted.shrink(toAdd);
                                merged = true;
                                if (extracted.isEmpty()) break;
                            }
                        }

                        // If not merged, spawn new entity
                        if (!extracted.isEmpty()) {
                            ItemEntity entity = new ItemEntity(
                                    level,
                                    centerX,
                                    pos.getY() + 0.6,
                                    centerZ,
                                    extracted
                            );
                            entity.setDeltaMovement(0, 0, 0);
                            entity.setPickUpDelay(10);
                            entity.setUnlimitedLifetime();
                            level.addFreshEntity(entity);
                        }
                        break; // one item per tick
                    }
                }
            }
        }

        /* ===================================================
         * 2️⃣ MOVE ITEMS ON BELT (CENTERED)
         * =================================================== */
        double forwardSpeed = 0.075;
        double centeringStrength = 0.25;

        AABB beltBox = new AABB(centerX - 0.4, minY, centerZ - 0.4,
                                centerX + 0.4, maxY, centerZ + 0.4);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, beltBox);

        for (ItemEntity item : items) {
            if (!item.isAlive() || item.getItem().isEmpty()) continue;

            item.setUnlimitedLifetime();

            // Centering movement
            double offsetX = centerX - item.getX();
            double offsetZ = centerZ - item.getZ();

            double sideX = 0;
            double sideZ = 0;

            if (facing.getAxis() == Direction.Axis.Z) sideX = offsetX * centeringStrength;
            if (facing.getAxis() == Direction.Axis.X) sideZ = offsetZ * centeringStrength;

            sideX = Math.max(-0.05, Math.min(0.05, sideX));
            sideZ = Math.max(-0.05, Math.min(0.05, sideZ));

            item.setDeltaMovement(
                    facing.getStepX() * forwardSpeed + sideX,
                    0,
                    facing.getStepZ() * forwardSpeed + sideZ
            );
            item.hasImpulse = true;

            /* ===================================================
             * 3️⃣ PUSH INTO STORAGE IF THIS IS AN INPUT BELT
             * =================================================== */
            if (isInputBelt) {
                BlockPos outputPos = pos.relative(facing);
                IItemHandler outputHandler = level.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        outputPos,
                        back
                );

                if (outputHandler != null) {
                    ItemStack stack = item.getItem();
                    ItemStack remainder = ItemHandlerHelper.insertItem(outputHandler, stack, false);

                    if (remainder.isEmpty()) {
                        item.discard();
                    } else {
                        item.setItem(remainder);
                    }
                }
            }
        }
    }
}
