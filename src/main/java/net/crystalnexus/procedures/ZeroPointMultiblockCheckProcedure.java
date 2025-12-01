package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class ZeroPointMultiblockCheckProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double energy = 0;
		if (CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y - 1, z - 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y - 1, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y - 1, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y - 1, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y - 1, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y - 1, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y - 1, z + 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y - 1, z - 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y - 1, z - 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y - 1, z + 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y - 1, z + 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y - 1, z - 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y - 1, z - 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y - 1, z + 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y - 1, z + 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 8, y - 1, z - 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 8, y - 1, z + 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 7, y - 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 7, y - 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 6, y - 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 6, y - 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 5, y - 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 5, y - 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 4, y - 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 4, y - 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 3, y - 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 3, y - 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 2, y - 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 2, y - 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 1, y - 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 1, y - 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 0, y - 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 0, y - 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 1, y - 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 1, y - 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 2, y - 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 2, y - 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 3, y - 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 3, y - 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 4, y - 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 4, y - 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 5, y - 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 5, y - 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 6, y - 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 6, y - 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 7, y - 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 7, y - 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 8, y - 1, z - 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 8, y - 1, z + 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y - 1, z - 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y - 1, z - 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y - 1, z + 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y - 1, z + 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y - 1, z - 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y - 1, z - 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y - 1, z + 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y - 1, z + 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y - 1, z - 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y - 1, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y - 1, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y - 1, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y - 1, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y - 1, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y - 1, z + 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 12, y + 0, z - 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 12, y + 0, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 12, y + 0, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 12, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 12, y + 0, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 12, y + 0, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 12, y + 0, z + 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 0, z - 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 0, z - 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 0, z + 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 0, z + 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 0, z - 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 0, z - 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 0, z + 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 0, z + 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y + 0, z - 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 9, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y + 0, z + 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 8, y + 0, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 8, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 8, y + 0, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 7, y + 0, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 7, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 7, y + 0, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 6, y + 0, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 6, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 6, y + 0, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 5, y + 0, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 5, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 5, y + 0, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 4, y + 0, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 4, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 4, y + 0, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 3, y + 0, z - 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 3, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 3, y + 0, z + 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 2, y + 0, z - 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 2, y + 0, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 2, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 2, y + 0, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 2, y + 0, z + 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 0, z - 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 0, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 0, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 0, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 0, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 0, z + 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 0, z - 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 0, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 0, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 0, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 0, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 0, z + 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 2, y + 0, z - 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 2, y + 0, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 2, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 2, y + 0, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 2, y + 0, z + 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 3, y + 0, z - 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 3, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 3, y + 0, z + 12))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 4, y + 0, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 4, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 4, y + 0, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 5, y + 0, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 5, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 5, y + 0, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 6, y + 0, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 6, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 6, y + 0, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 7, y + 0, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 7, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 7, y + 0, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 8, y + 0, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 8, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 8, y + 0, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y + 0, z - 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 9, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y + 0, z + 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 0, z - 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 0, z - 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 0, z + 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 0, z + 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 0, z - 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 0, z - 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 0, z + 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 0, z + 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 12, y + 0, z - 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 12, y + 0, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 12, y + 0, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 12, y + 0, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 12, y + 0, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_GLASS.get() == (world.getBlockState(BlockPos.containing(x + 12, y + 0, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 12, y + 0, z + 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 1, z - 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 1, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 1, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 1, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 1, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 1, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 11, y + 1, z + 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 1, z - 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 1, z - 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 1, z + 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 10, y + 1, z + 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y + 1, z - 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y + 1, z - 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y + 1, z + 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 9, y + 1, z + 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 8, y + 1, z - 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 8, y + 1, z + 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 7, y + 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 7, y + 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 6, y + 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 6, y + 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 5, y + 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 5, y + 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 4, y + 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 4, y + 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 3, y + 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 3, y + 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 2, y + 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 2, y + 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x - 1, y + 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 1, y + 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 2, y + 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 2, y + 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 3, y + 1, z - 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 3, y + 1, z + 11))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 4, y + 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 4, y + 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 5, y + 1, z - 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 5, y + 1, z + 10))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 6, y + 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 6, y + 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 7, y + 1, z - 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 7, y + 1, z + 9))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 8, y + 1, z - 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 8, y + 1, z + 8))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y + 1, z - 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y + 1, z - 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y + 1, z + 6))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 9, y + 1, z + 7))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 1, z - 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 1, z - 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 1, z + 4))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 10, y + 1, z + 5))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 1, z - 3))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 1, z - 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 1, z - 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 1, z + 0))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 1, z + 1))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 1, z + 2))).getBlock()
				&& CrystalnexusModBlocks.CARBON_BLOCK.get() == (world.getBlockState(BlockPos.containing(x + 11, y + 1, z + 3))).getBlock()
				&& CrystalnexusModBlocks.ZERO_POINT.get() == (world.getBlockState(BlockPos.containing(x + 0, y + 0, z + 0))).getBlock()) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.ELECTRIC_SPARK, (x + 0.5), (y + 0.5), (z + 0.5), 5, 3, 0, 3, 0.2);
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
			if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") == 300) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", 0);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crystalnexus:zero_point_active")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crystalnexus:zero_point_active")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
			} else {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			}
			if (canReceiveEnergy(world, BlockPos.containing(x, y + 1, z), Direction.DOWN)) {
				energy = receiveEnergySimulate(world, BlockPos.containing(x, y + 1, z), 10240000, Direction.DOWN);
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y + 1, z), Direction.DOWN);
					if (_entityStorage != null)
						_entityStorage.receiveEnergy((int) energy, false);
				}
			}
			if (canReceiveEnergy(world, BlockPos.containing(x, y - 1, z), Direction.UP)) {
				energy = receiveEnergySimulate(world, BlockPos.containing(x, y - 1, z), 10240000, Direction.UP);
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
					if (_entityStorage != null)
						_entityStorage.receiveEnergy((int) energy, false);
				}
			}
			if (canReceiveEnergy(world, BlockPos.containing(x + 1, y, z), Direction.WEST)) {
				energy = receiveEnergySimulate(world, BlockPos.containing(x + 1, y, z), 10240000, Direction.WEST);
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + 1, y, z), Direction.WEST);
					if (_entityStorage != null)
						_entityStorage.receiveEnergy((int) energy, false);
				}
			}
			if (canReceiveEnergy(world, BlockPos.containing(x - 1, y, z), Direction.EAST)) {
				energy = receiveEnergySimulate(world, BlockPos.containing(x - 1, y, z), 10240000, Direction.EAST);
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x - 1, y, z), Direction.EAST);
					if (_entityStorage != null)
						_entityStorage.receiveEnergy((int) energy, false);
				}
			}
			if (canReceiveEnergy(world, BlockPos.containing(x, y, z - 1), Direction.SOUTH)) {
				energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z - 1), 10240000, Direction.SOUTH);
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z - 1), Direction.SOUTH);
					if (_entityStorage != null)
						_entityStorage.receiveEnergy((int) energy, false);
				}
			}
			if (canReceiveEnergy(world, BlockPos.containing(x, y, z + 1), Direction.NORTH)) {
				energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z + 1), 10240000, Direction.NORTH);
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z + 1), Direction.NORTH);
					if (_entityStorage != null)
						_entityStorage.receiveEnergy((int) energy, false);
				}
			}
		} else {
			{
				int _value = 1;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}

	private static boolean canReceiveEnergy(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.canReceive();
		}
		return false;
	}

	private static int receiveEnergySimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.receiveEnergy(amount, true);
		}
		return 0;
	}
}