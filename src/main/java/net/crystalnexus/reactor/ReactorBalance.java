package net.crystalnexus.reactor;

public final class ReactorBalance {
	public static final int LAYOUT_RECHECK_TICKS = 20; // how often the game rechecks the reactor multiblock shape
	public static final int COOLANT_PER_CHANNEL_MB_T = 25; // how much coolant each channel can pump per tick
	public static final double HEAT_PER_MB_COOLANT = 15.0; // how much heat each bucket of coolant absorbs
	public static final double AMBIENT_TEMPERATURE = 20.0; // default temp when reactor is off
	public static final double TARGET_TEMPERATURE = 700.0; // the ideal temp to run the reactor at
	public static final double PASSIVE_HEAT_LOSS_PER_DEGREE = 0.0005; // how fast heat bleeds out naturally
	public static final double COOLING_FEEDBACK_PER_DEGREE = 0.01; // cooling gets better the hotter it runs (up to a point)
	public static final double IDLE_COOLING_PER_DEGREE = 0.005; // how fast the reactor cools down when its not doing anything
	public static final double MIN_OPERATING_FACTOR = 0.20; // lowest % power output when theres not enough coolant
	public static final int BASE_FE_PER_ROD_T = 22500; // base energy per rod per tick, before anything else modifies it
	public static final double BASE_HEAT_PER_ROD_T = 3.0; // base heat per rod per tick *------------------------*
	public static final double DIRECT_FUEL_OUTPUT = 0.35; // energy multiplier for fuel thats not moderated
	public static final double DIRECT_FUEL_HEAT = 0.5; // heat multiplier for unmoderated fuel
	public static final double MODERATED_FUEL_OUTPUT = 0.45; // energy output when fuel is properly moderated
	public static final double MODERATED_FUEL_EFFICIENCY = 0.35; // efficiency penalty for moderated fuel
	public static final double MODERATED_FUEL_HEAT = 0.25; // heat multiplier when fuel is moderated
	public static final double REFLECTOR_OUTPUT = 0.35; // energy bonus from neutron reflectors
	public static final double REFLECTOR_HEAT = 0.10; // heat that neutron reflectors add
	public static final int CONDUCTOR_RANGE = 4; // how far heat conductors can reach
	public static final double CONDUCTOR_TRANSFER_PER_ROD = 50.0; // heat transfer per fuel rod for conductors
	public static final int MAX_TEMPERATURE = 1200; // absolute max temp before bad stuff happens
	public static final int SCRAM_TEMPERATURE = 1100; // temp that triggers emergency shutdown
	public static final double PERMAFROST_COOLANT_OFFSET = 0.75; // permafrost upgrade reduces coolant needed by this much
	public static final int COAL_SINGULARITY_CYCLES = 128; // how many times coal singularity burns before its used up
	public static final double FUEL_BURN_RATE_MULTIPLIER = 0.02; // how fast fuel gets consumed
	public static final double WASTE_MULTIPLIER = 0.05; // how much waste is produced from burning fuel
	public static final double CARBON_MODERATOR_EFFICIENCY_BONUS = 0.35; // efficiency boost from carbon moderators
	public static final double CARBON_MODERATOR_HEAT_REDUCTION = 0.75; // heat reduction from carbon moderators
	public static final int CARBON_MODERATOR_RANGE = 2; // how far carbon moderators affect things

	private ReactorBalance() {
	}
}
