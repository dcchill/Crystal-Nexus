package net.crystalnexus.reactor;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

public final class ReactorGraphHistory {
	public static final int CAPACITY = 120;
	private final double[] heat = new double[CAPACITY];
	private final double[] cooling = new double[CAPACITY];
	private final double[] temperatures = new double[CAPACITY];
	private final double[] feOutput = new double[CAPACITY];
	private final double[] maxFe = new double[CAPACITY];
	private int size, next, sampleTicks;

	public void tick(double heating, double maxCooling, double temperature, double outputFePerTick, double maxFePerTick) {
		if (++sampleTicks < 10) return;
		sampleTicks = 0;
		add(heating, maxCooling, temperature, outputFePerTick, maxFePerTick);
	}

	private void add(double heating, double maxCooling, double temperature, double outputFePerTick, double maxFePerTick) {
		heat[next] = Math.max(0, heating);
		cooling[next] = Math.max(0, maxCooling);
		temperatures[next] = Math.max(0, temperature);
		feOutput[next] = Math.max(0, outputFePerTick);
		maxFe[next] = Math.max(0, maxFePerTick);
		next = (next + 1) % CAPACITY;
		size = Math.min(size + 1, CAPACITY);
	}

	public int size() { return size; }
	public double heat(int index) { return heat[(next - size + index + CAPACITY) % CAPACITY]; }
	public double coolingCapacity(int index) { return cooling[(next - size + index + CAPACITY) % CAPACITY]; }
	public double temperature(int index) { return temperatures[(next - size + index + CAPACITY) % CAPACITY]; }
	public double feOutput(int index) { return feOutput[(next - size + index + CAPACITY) % CAPACITY]; }
	public double maxFe(int index) { return maxFe[(next - size + index + CAPACITY) % CAPACITY]; }
	public double secondsAgo(int index) { return ((size - 1 - index) * 10 + sampleTicks) / 20.0; }

	public int sampleAt(double fractionOfWindow) {
		int index = (int) Math.round(Math.clamp(fractionOfWindow, 0.0, 1.0) * (CAPACITY - 1)) - (CAPACITY - size);
		return index < 0 ? -1 : index;
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(sampleTicks);
		buffer.writeVarInt(size);
		for (int i = 0; i < size; i++) {
			buffer.writeDouble(heat(i));
			buffer.writeDouble(coolingCapacity(i));
			buffer.writeDouble(temperature(i));
			buffer.writeDouble(feOutput(i));
			buffer.writeDouble(maxFe(i));
		}
	}

	public static ReactorGraphHistory read(FriendlyByteBuf buffer) {
		ReactorGraphHistory history = new ReactorGraphHistory();
		history.sampleTicks = buffer.readVarInt();
		int count = buffer.readVarInt();
		if (history.sampleTicks < 0 || history.sampleTicks >= 10 || count < 0 || count > CAPACITY)
			throw new DecoderException("Invalid reactor graph history");
		for (int i = 0; i < count; i++)
			history.add(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		return history;
	}
}
