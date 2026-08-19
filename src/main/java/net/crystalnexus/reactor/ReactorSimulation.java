package net.crystalnexus.reactor;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.init.CrystalnexusModGameRules;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class ReactorSimulation {
	private ReactorSimulation() {
	}

	public static void tick(LevelAccessor world, BlockPos pos, ReactorComputerBlockEntity computer) {
		CompoundTag data = computer.getPersistentData();
		ReactorLayout layout = computer.getCachedLayout();
		int radius = data.getInt("multiblockRadius");
		if (!data.getBoolean("canOpenInventory") || radius <= 0 || layout == null || !layout.valid) {
			setStatus(world, pos, computer, layout == null ? "Offline" : layout.reason, 1);
			return;
		}
		ItemStack fuel = computer.getItem(0);
		boolean hasFuel = isFuel(fuel) && computer.getItem(2).getCount() < 64;
		double temperature = Math.max(20, data.getDouble("heat"));
		
		if (!hasFuel) {
			// Passive cooling when no fuel
			if (temperature <= 20) {
				setStatus(world, pos, computer, "Offline", 1);
				return;
			}
			// Apply passive cooling
			double nextTemperature = Math.max(20, temperature - ReactorBalance.PASSIVE_HEAT_LOSS);
			data.putDouble("heat", nextTemperature);
			data.putDouble("maxHeat", ReactorBalance.MAX_TEMPERATURE);
			data.putDouble("lastFEt", 0);
			data.putDouble("heatGenerated", 0);
			data.putDouble("heatRemoved", ReactorBalance.PASSIVE_HEAT_LOSS);
			data.putDouble("coolantDemand", 0);
			data.putDouble("coolantCapacity", 0);
			data.putDouble("coolantUsed", 0);
			data.putDouble("fuelEfficiency", layout.fuelEfficiency);
			data.putDouble("fuelColumns", layout.fuelColumns);
			data.putDouble("activeCoolantChannels", layout.activeCoolantChannels);
			setStatus(world, pos, computer, "Cooling", 1);
			return;
		}
		double insertion = data.getDouble("controlRodInsertion");
		insertion = Math.max(0, Math.min(100, insertion));
		double control = 1.0 - insertion / 100.0;
		if (control <= 0.0) {
			setStatus(world, pos, computer, "Offline", 1);
			return;
		}
		double fuelPower = fuel.getItem() == CrystalnexusModItems.PURE_BLUTONIUM.get() ? 1.75 : 1.0;
		if (fuel.getItem() == CrystalnexusModItems.COAL_SINGULARITY.get()) {
			fuelPower = 0.8;
		}
		if (computer.getItem(1).getItem() == CrystalnexusModItems.REACTOR_UPGRADE.get()) {
			fuelPower *= 2.0;
		}
		boolean permafrost = computer.getItem(1).getItem() == CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get();
		double tempEfficiency = temperatureCurve(temperature);
		int fe = (int) Math.round(ReactorBalance.BASE_FE_PER_ROD_T * layout.fuelRods * layout.outputMultiplier * layout.fuelEfficiency * fuelPower * control * tempEfficiency);
		double heatGenerated = ReactorBalance.BASE_HEAT_PER_ROD_T * layout.fuelRods * layout.heatMultiplier * fuelPower * control;
		heatGenerated *= 1.0 + Math.max(0, layout.fuelRods - layout.fuelColumns) * 0.08;
		double reachableHeat = heatGenerated * layout.conductorCoolingAccess;
		int coolantDemand = permafrost ? 0 : (int) Math.ceil(reachableHeat / ReactorBalance.HEAT_PER_MB_COOLANT);
		int coolantCapacity = permafrost ? coolantDemand : layout.coolantCapacityMbT;
		int coolantUsed = 0;
		if (coolantDemand > 0 && coolantCapacity > 0) {
			coolantUsed = computer.getFluidTank().drain(Math.min(coolantDemand, coolantCapacity), IFluidHandler.FluidAction.EXECUTE).getAmount();
		}
		double heatRemoved = permafrost ? heatGenerated : coolantUsed * ReactorBalance.HEAT_PER_MB_COOLANT;
		double nextTemperature = Math.max(20, temperature + heatGenerated - heatRemoved - ReactorBalance.PASSIVE_HEAT_LOSS);
		data.putDouble("heat", Math.min(nextTemperature, ReactorBalance.MAX_TEMPERATURE + 250));
		data.putDouble("maxHeat", ReactorBalance.MAX_TEMPERATURE);
		data.putDouble("lastFEt", fe);
		data.putDouble("heatGenerated", heatGenerated);
		data.putDouble("heatRemoved", heatRemoved);
		data.putDouble("coolantDemand", coolantDemand);
		data.putDouble("coolantCapacity", coolantCapacity);
		data.putDouble("coolantUsed", coolantUsed);
		data.putDouble("fuelEfficiency", layout.fuelEfficiency);
		data.putDouble("fuelColumns", layout.fuelColumns);
		data.putDouble("activeCoolantChannels", layout.activeCoolantChannels);
		if (nextTemperature >= ReactorBalance.SCRAM_TEMPERATURE || (!permafrost && coolantDemand > 0 && coolantUsed < Math.min(coolantDemand, coolantCapacity))) {
			setStatus(world, pos, computer, nextTemperature >= ReactorBalance.SCRAM_TEMPERATURE ? "SCRAM" : "Coolant Limited", 1);
			return;
		}
		computer.getEnergyStorage().generateEnergy(fe, false);
		progressFuelCycle(computer, fuel, layout, control);
		setStatus(world, pos, computer, "Stable", 2);
		if (world instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.VAULT_CONNECTION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.5, 0, 0.5, 0);
		}
	}

	private static boolean isFuel(ItemStack fuel) {
		return fuel.getItem() == CrystalnexusModItems.BLUTONIUM_INGOT.get()
				|| fuel.getItem() == CrystalnexusModItems.PURE_BLUTONIUM.get()
				|| fuel.getItem() == CrystalnexusModItems.COAL_SINGULARITY.get();
	}

	private static void progressFuelCycle(ReactorComputerBlockEntity computer, ItemStack fuel, ReactorLayout layout, double control) {
		CompoundTag data = computer.getPersistentData();
		double burn = Math.max(1, layout.fuelRods * control / Math.max(0.25, layout.fuelEfficiency) * ReactorBalance.FUEL_BURN_RATE_MULTIPLIER);
		double maxProgress = 2000;
		double progress = data.getDouble("progress") + burn;
		data.putDouble("maxProgress", maxProgress);
		if (progress < maxProgress) {
			data.putDouble("progress", progress);
			return;
		}
		data.putDouble("progress", 0);
		if (fuel.getItem() != CrystalnexusModItems.COAL_SINGULARITY.get()) {
			fuel.shrink(1);
			computer.setItem(0, fuel);
		}
		ItemStack waste = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get());
		waste.setCount(Math.min(64, computer.getItem(2).getCount() + (int) Math.round(Math.max(1, layout.fuelColumns) * ReactorBalance.WASTE_MULTIPLIER)));
		computer.setItem(2, waste);
	}

	private static double temperatureCurve(double temperature) {
		if (temperature < 200) {
			return 0.55;
		}
		if (temperature < 500) {
			return 0.9;
		}
		if (temperature < 900) {
			return 1.15;
		}
		return 1.35;
	}

	private static void setStatus(LevelAccessor world, BlockPos pos, ReactorComputerBlockEntity computer, String status, int blockState) {
		CompoundTag data = computer.getPersistentData();
		data.putString("reactorStatus", status);
		computer.setChanged();
		if (world instanceof Level level) {
			var state = level.getBlockState(pos);
			if (state.getBlock().getStateDefinition().getProperty("blockstate") instanceof net.minecraft.world.level.block.state.properties.IntegerProperty prop
					&& prop.getPossibleValues().contains(blockState)) {
				level.setBlock(pos, state.setValue(prop, blockState), 3);
			}
			level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
			if ("SCRAM".equals(status) && !level.getLevelData().getGameRules().getBoolean(CrystalnexusModGameRules.DISABLE_MELTDOWNS)) {
				level.playSound(null, pos, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crystalnexus:reactor_failure")), SoundSource.BLOCKS, 1, 1);
			}
		}
	}
}
