package net.crystalnexus.reactor;

public final class ReactorBalance {
	public static final int LAYOUT_RECHECK_TICKS = 20;
	public static final int COOLANT_PER_CHANNEL_MB_T = 25;
	public static final int HEAT_PER_MB_COOLANT = 2;
	public static final int BASE_FE_PER_ROD_T = 5000; //fe gen
	public static final double BASE_HEAT_PER_ROD_T = 8.0;
	public static final double DIRECT_FUEL_OUTPUT = 0.35;
	public static final double DIRECT_FUEL_HEAT = 1.1;
	public static final double MODERATED_FUEL_OUTPUT = 0.45;
	public static final double MODERATED_FUEL_EFFICIENCY = 0.35;
	public static final double MODERATED_FUEL_HEAT = 0.25;
	public static final double REFLECTOR_OUTPUT = 0.35;
	public static final double REFLECTOR_HEAT = 0.15;
	public static final double PASSIVE_HEAT_LOSS = 10.0;
	public static final int CONDUCTOR_RANGE = 4;
	public static final double CONDUCTOR_TRANSFER_PER_ROD = 12.0;
	public static final int MAX_TEMPERATURE = 1200;
	public static final int SCRAM_TEMPERATURE = 1200;
	public static final double FUEL_BURN_RATE_MULTIPLIER = 0.07;
	public static final double WASTE_MULTIPLIER = 0.05;
	public static final double CARBON_MODERATOR_EFFICIENCY_BONUS = 0.35;
	public static final double CARBON_MODERATOR_HEAT_REDUCTION = 0.75;
	public static final int CARBON_MODERATOR_RANGE = 2;

	private ReactorBalance() {
	}
}
