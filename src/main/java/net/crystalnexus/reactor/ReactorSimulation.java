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
		double temperature = Math.max(ReactorBalance.AMBIENT_TEMPERATURE, data.getDouble("heat"));
		
		if (!hasFuel) {
			coolIdle(world, pos, computer, layout, temperature);
			return;
		}
		ReactorLayout.OperatingTotals operatingTotals = layout.operatingTotals(world);
		if (operatingTotals.reactiveFuelRods() <= 0) {
			coolIdle(world, pos, computer, layout, temperature);
			return;
		}
		double fuelPower = fuel.getItem() == CrystalnexusModItems.PURE_BLUTONIUM.get() ? 1.50 : 1.0;
		if (fuel.getItem() == CrystalnexusModItems.COAL_SINGULARITY.get()) {
			fuelPower = 0.8;
		}
		if (computer.getItem(1).getItem() == CrystalnexusModItems.REACTOR_UPGRADE.get()) {
			fuelPower *= 1.50;
		}
		boolean permafrost = computer.getItem(1).getItem() == CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get();
		double tempEfficiency = temperatureCurve(temperature);
		int unthrottledFe = (int) Math.round(ReactorBalance.BASE_FE_PER_ROD_T * operatingTotals.output() * layout.fuelEfficiency * fuelPower * tempEfficiency);
		double unthrottledHeat = ReactorBalance.BASE_HEAT_PER_ROD_T * operatingTotals.heat() * fuelPower;
		unthrottledHeat *= 1.0 + Math.max(0, layout.fuelRods - layout.fuelColumns) * 0.08;
		int coolantDemand = runningCoolantDemand(temperature, unthrottledHeat);
		int coolantCapacity = layout.coolantCapacityMbT;
		int coolantOffset = permafrost ? (int) Math.floor(coolantDemand * ReactorBalance.PERMAFROST_COOLANT_OFFSET) : 0;
		int actualDemand = Math.max(0, coolantDemand - coolantOffset);
		int coolantUsed = 0;
		if (actualDemand > 0 && coolantCapacity > 0) {
			coolantUsed = computer.getFluidTank().drain(Math.min(actualDemand, coolantCapacity), IFluidHandler.FluidAction.EXECUTE).getAmount();
		}
		double operatingFactor = coolantDemand == 0 ? 1.0
				: Math.max(ReactorBalance.MIN_OPERATING_FACTOR, (coolantOffset + coolantUsed) / (double) coolantDemand);
		int fe = (int) Math.round(unthrottledFe * operatingFactor);
		double heatGenerated = unthrottledHeat * operatingFactor;
		double heatRemoved = (coolantOffset + coolantUsed) * ReactorBalance.HEAT_PER_MB_COOLANT;
		double passiveHeatLoss = passiveHeatLoss(temperature);
		double nextTemperature = Math.max(ReactorBalance.AMBIENT_TEMPERATURE, temperature + heatGenerated - heatRemoved - passiveHeatLoss);
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
		if (nextTemperature >= ReactorBalance.SCRAM_TEMPERATURE) {
			setStatus(world, pos, computer, "SCRAM", 1);
			return;
		}
		computer.getEnergyStorage().generateEnergy(fe, false);
		progressFuelCycle(computer, fuel, layout, operatingTotals, operatingFactor);
		String status = coolantCapacity < actualDemand ? "Cooling Capacity Limited"
				: coolantUsed < actualDemand ? "Coolant Limited" : "Stable";
		setStatus(world, pos, computer, status, 2);
		if (world instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.VAULT_CONNECTION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.5, 0, 0.5, 0);
		}
	}

	private static void coolIdle(LevelAccessor world, BlockPos pos, ReactorComputerBlockEntity computer, ReactorLayout layout, double temperature) {
		if (temperature <= ReactorBalance.AMBIENT_TEMPERATURE) {
			CompoundTag data = computer.getPersistentData();
			data.putDouble("heat", ReactorBalance.AMBIENT_TEMPERATURE);
			data.putDouble("lastFEt", 0);
			data.putDouble("heatGenerated", 0);
			data.putDouble("heatRemoved", 0);
			data.putDouble("coolantDemand", 0);
			data.putDouble("coolantUsed", 0);
			setStatus(world, pos, computer, "Offline", 1);
			return;
		}
		CompoundTag data = computer.getPersistentData();
		int coolantCapacity = layout.coolantCapacityMbT;
		int coolantDemand = idleCoolantDemand(temperature);
		int coolantUsed = computer.getFluidTank().drain(Math.min(coolantDemand, coolantCapacity), IFluidHandler.FluidAction.EXECUTE).getAmount();
		double coolantHeatRemoved = coolantUsed * ReactorBalance.HEAT_PER_MB_COOLANT;
		double passiveHeatLoss = passiveHeatLoss(temperature);
		double nextTemperature = Math.max(ReactorBalance.AMBIENT_TEMPERATURE, temperature - coolantHeatRemoved - passiveHeatLoss);
		data.putDouble("heat", nextTemperature);
		data.putDouble("maxHeat", ReactorBalance.MAX_TEMPERATURE);
		data.putDouble("lastFEt", 0);
		data.putDouble("heatGenerated", 0);
		data.putDouble("heatRemoved", coolantHeatRemoved + passiveHeatLoss);
		data.putDouble("coolantDemand", coolantDemand);
		data.putDouble("coolantCapacity", coolantCapacity);
		data.putDouble("coolantUsed", coolantUsed);
		data.putDouble("fuelEfficiency", layout.fuelEfficiency);
		data.putDouble("fuelColumns", layout.fuelColumns);
		data.putDouble("activeCoolantChannels", layout.activeCoolantChannels);
		setStatus(world, pos, computer, "Cooling", 1);
	}

	private static int runningCoolantDemand(double temperature, double heatGenerated) {
		double requestedHeatRemoval = heatGenerated - passiveHeatLoss(temperature)
				+ (temperature - ReactorBalance.TARGET_TEMPERATURE) * ReactorBalance.COOLING_FEEDBACK_PER_DEGREE;
		return (int) Math.ceil(Math.max(0, requestedHeatRemoval) / ReactorBalance.HEAT_PER_MB_COOLANT);
	}

	private static int idleCoolantDemand(double temperature) {
		double requestedHeatRemoval = (temperature - ReactorBalance.AMBIENT_TEMPERATURE) * ReactorBalance.IDLE_COOLING_PER_DEGREE;
		return (int) Math.ceil(Math.max(0, requestedHeatRemoval) / ReactorBalance.HEAT_PER_MB_COOLANT);
	}

	private static double passiveHeatLoss(double temperature) {
		return Math.max(0, temperature - ReactorBalance.AMBIENT_TEMPERATURE) * ReactorBalance.PASSIVE_HEAT_LOSS_PER_DEGREE;
	}

	private static boolean isFuel(ItemStack fuel) {
		return fuel.getItem() == CrystalnexusModItems.BLUTONIUM_INGOT.get()
				|| fuel.getItem() == CrystalnexusModItems.PURE_BLUTONIUM.get()
				|| fuel.getItem() == CrystalnexusModItems.COAL_SINGULARITY.get();
	}

	private static void progressFuelCycle(ReactorComputerBlockEntity computer, ItemStack fuel, ReactorLayout layout,
			ReactorLayout.OperatingTotals operatingTotals, double operatingFactor) {
		CompoundTag data = computer.getPersistentData();
		double burn = operatingTotals.reactiveFuelRods() * operatingFactor
				/ Math.max(0.25, layout.fuelEfficiency) * ReactorBalance.FUEL_BURN_RATE_MULTIPLIER;
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
		} else {
			int coalCycles = data.getInt("coalSingularityCycles") + 1;
			if (coalCycles >= ReactorBalance.COAL_SINGULARITY_CYCLES) {
				fuel.shrink(1);
				computer.setItem(0, fuel);
				data.putInt("coalSingularityCycles", 0);
			} else {
				data.putInt("coalSingularityCycles", coalCycles);
			}
		}
		ItemStack waste = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get());
		int wasteProduced = Math.max(1, (int) Math.round(operatingTotals.reactiveFuelColumns() * ReactorBalance.WASTE_MULTIPLIER));
		waste.setCount(Math.min(64, computer.getItem(2).getCount() + wasteProduced));
		computer.setItem(2, waste);
	}

	private static double temperatureCurve(double temperature) {
		if (temperature <= 200) {
			return 0.60;
		}
		if (temperature <= 500) {
			return interpolate(temperature, 200, 0.60, 500, 0.95);
		}
		if (temperature <= 700) {
			return interpolate(temperature, 500, 0.95, 700, 1.10);
		}
		if (temperature <= 900) {
			return interpolate(temperature, 700, 1.10, 900, 1.00);
		}
		if (temperature <= 1050) {
			return interpolate(temperature, 900, 1.00, 1050, 0.75);
		}
		return interpolate(temperature, 1050, 0.75, ReactorBalance.SCRAM_TEMPERATURE, 0.50);
	}

	private static double interpolate(double value, double lowerX, double lowerY, double upperX, double upperY) {
		return lowerY + (upperY - lowerY) * Math.min(1, Math.max(0, (value - lowerX) / (upperX - lowerX)));
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
