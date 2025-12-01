package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class ChlorophyteAcceleratorOnTickUpdateProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        // Only run if the block has enough energy
        if (getEnergyStored(world, BlockPos.containing(x, y, z), null) >= 64) {

            int radius = 5; // how far around the block to boost growth
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -1; dy <= 1; dy++) { // check slightly above/below
                        BlockPos targetPos = BlockPos.containing(x + dx, y + dy, z + dz);
                        BlockState state = world.getBlockState(targetPos);
					   	Block block = state.getBlock();
                        // Only grow crops that have the AGE property
						if (block instanceof CropBlock crop && state.hasProperty(CropBlock.AGE)) {
   							 int age = state.getValue(CropBlock.AGE);
    						 int maxAge = crop.getMaxAge();
    						 if (age < maxAge && world.getRandom().nextFloat() < 0.25f) {
       							 world.setBlock(targetPos, state.setValue(CropBlock.AGE, age + 1), 2);
 							   }
						}
						 else if (block instanceof AmethystClusterBlock cluster && world instanceof ServerLevel serverWorld) {
    							world.scheduleTick(targetPos, block, 1 + world.getRandom().nextInt(3));
						}
                        // Nether Wart
                        else if (state.getBlock() instanceof NetherWartBlock) {
                            int age = state.getValue(NetherWartBlock.AGE);
                            if (age < 3 && world.getRandom().nextFloat() < 0.25f) {
                                world.setBlock(targetPos, state.setValue(NetherWartBlock.AGE, age + 1), 2);
                            }
                        }
						else if (state.getBlock() == Blocks.TORCHFLOWER_CROP) {
							    continue;
						}
                        // Sugar Cane & Cactus
                        else if (state.hasProperty(SugarCaneBlock.AGE)) {
                            int age = state.getValue(SugarCaneBlock.AGE);
                            if (age < 15 && world.getRandom().nextFloat() < 0.25f) {
                                world.setBlock(targetPos, state.setValue(SugarCaneBlock.AGE, age + 1), 2);
                            }
                        } else if (state.hasProperty(CactusBlock.AGE)) {
                            int age = state.getValue(CactusBlock.AGE);
                            if (age < 15 && world.getRandom().nextFloat() < 0.25f) {
                                world.setBlock(targetPos, state.setValue(CactusBlock.AGE, age + 1), 2);
                            }
                        
                        }
                    }
                }
            }
        }			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
				if (_entityStorage != null)
					_entityStorage.extractEnergy(64, false);
			}
 // end energy check
    }

    public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
        if (level instanceof ILevelExtension levelExtension) {
            IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
            if (energyStorage != null)
                return energyStorage.getEnergyStored();
        }
        return 0;
        
    }
    
}
