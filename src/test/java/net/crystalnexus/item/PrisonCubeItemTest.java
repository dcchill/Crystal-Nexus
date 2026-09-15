package net.crystalnexus.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrisonCubeItemTest {
	@Test
	void stripsIdentityAndMovementBeforeRespawning() {
		assertEquals(java.util.List.of("UUID", "Pos", "Motion", "Rotation"), PrisonCubeData.TRANSIENT_ENTITY_FIELDS);
	}
}
