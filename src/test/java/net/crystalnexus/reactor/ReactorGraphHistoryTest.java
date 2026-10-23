package net.crystalnexus.reactor;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReactorGraphHistoryTest {
	@Test
	void recordsEveryTenTicksAndKeepsTheLastMinuteInOrder() {
		var history = new ReactorGraphHistory();
		for (int tick = 0; tick < 9; tick++) history.tick(1, 10, 20, 30, 40);
		assertEquals(0, history.size());
		history.tick(2, 20, 25, 30, 40);
		assertEquals(1, history.size());
		assertEquals(2, history.heat(0));
		assertEquals(20, history.coolingCapacity(0));
		assertEquals(25, history.temperature(0));
		assertEquals(30, history.feOutput(0));
		assertEquals(40, history.maxFe(0));
		for (int sample = 1; sample <= 130; sample++)
			for (int tick = 0; tick < 10; tick++) history.tick(sample, 1000 + sample, 20 + sample, 2000 + sample, 3000 + sample);
		assertEquals(120, history.size());
		for (int i = 0; i < history.size(); i++) {
			assertEquals(11 + i, history.heat(i));
			assertEquals(1011 + i, history.coolingCapacity(i));
			assertEquals(31 + i, history.temperature(i));
			assertEquals(2011 + i, history.feOutput(i));
			assertEquals(3011 + i, history.maxFe(i));
		}
	}

	@Test
	void menuSnapshotKeepsHistoryAndSamplingPhaseWithoutSharingState() {
		var history = new ReactorGraphHistory();
		for (int tick = 0; tick < 1257; tick++) history.tick(tick / 10, 1000 + tick / 10, 20 + tick / 10, 2000 + tick / 10, 3000 + tick / 10);
		var buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			history.write(buffer);
			var reopened = ReactorGraphHistory.read(buffer);
			assertEquals(0, buffer.readableBytes());
			assertEquals(history.size(), reopened.size());
			for (int i = 0; i < history.size(); i++) {
				assertEquals(history.heat(i), reopened.heat(i));
				assertEquals(history.coolingCapacity(i), reopened.coolingCapacity(i));
				assertEquals(history.temperature(i), reopened.temperature(i));
				assertEquals(history.feOutput(i), reopened.feOutput(i));
				assertEquals(history.maxFe(i), reopened.maxFe(i));
			}
			reopened.tick(500, 600, 700, 800, 900);
			reopened.tick(500, 600, 700, 800, 900);
			assertEquals(124, reopened.heat(119));
			reopened.tick(500, 600, 700, 800, 900);
			assertEquals(500, reopened.heat(119));
			assertEquals(600, reopened.coolingCapacity(119));
			assertEquals(700, reopened.temperature(119));
			assertEquals(800, reopened.feOutput(119));
			assertEquals(900, reopened.maxFe(119));
			assertEquals(124, history.heat(119));
			assertEquals(144, history.temperature(119));
			assertEquals(2124, history.feOutput(119));
			assertEquals(3124, history.maxFe(119));
		} finally {
			buffer.release();
		}
	}

	@Test
	void hoveringSelectsHistoricalSamplesAndSkipsUnrecordedTime() {
		var history = new ReactorGraphHistory();
		assertEquals(-1, history.sampleAt(1));
		for (int tick = 0; tick < 37; tick++) history.tick(tick, tick, tick, tick, tick);
		assertEquals(-1, history.sampleAt(0));
		assertEquals(0, history.sampleAt(117.0 / 119));
		assertEquals(1, history.sampleAt(118.0 / 119));
		assertEquals(2, history.sampleAt(1));
		assertEquals(9, history.feOutput(history.sampleAt(117.0 / 119)));
		assertEquals(29, history.feOutput(history.sampleAt(1)));
		assertEquals(1.35, history.secondsAgo(0), 1e-9);
		assertEquals(0.35, history.secondsAgo(2), 1e-9);
		for (int tick = 0; tick < 1200; tick++) history.tick(tick, tick, tick, tick, tick);
		assertEquals(0, history.sampleAt(0));
		assertEquals(119, history.sampleAt(1));
	}

	@Test
	void rejectsInvalidSnapshotBounds() {
		for (int[] header : new int[][] {{-1, 0}, {10, 0}, {0, -1}, {0, 121}}) {
			var buffer = new FriendlyByteBuf(Unpooled.buffer());
			try {
				buffer.writeVarInt(header[0]).writeVarInt(header[1]);
				assertThrows(DecoderException.class, () -> ReactorGraphHistory.read(buffer));
			} finally {
				buffer.release();
			}
		}
	}
}
