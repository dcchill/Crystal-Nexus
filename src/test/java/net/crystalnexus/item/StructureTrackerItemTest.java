package net.crystalnexus.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureTrackerItemTest {
	@Test
	void formatsRegistryPathsForTheSelectionGui() {
		assertEquals("Ancient City", StructureTrackerNames.displayName("ancient_city"));
		assertEquals("Village/Plains", StructureTrackerNames.displayName("village/plains"));
		assertEquals("Resource Meteor", StructureTrackerNames.displayName("resource_meteor"));
	}

	@Test
	void compassMovesTowardTheTargetBearing() {
		int ahead = StructureTrackerCompass.bar(0, 1, 0, 10).indexOf("<>");
		int right = StructureTrackerCompass.bar(0, 1, 10, 0).indexOf("<>");
		int left = StructureTrackerCompass.bar(0, 1, -10, 0).indexOf("<>");
		assertEquals(7, ahead);
		assertEquals(0, left);
		assertEquals(14, right);
	}

	@Test
	void compassHidesThePointerWhenTargetIsBehind() {
		assertEquals("===============", StructureTrackerCompass.bar(0, 1, 0, -10));
		assertEquals("===============", StructureTrackerCompass.bar(0, 1, 10, -1));
	}
}
