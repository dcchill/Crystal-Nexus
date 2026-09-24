package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.data.MeteorTrackerSavedData;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class MeteorTrackerEvents {
	private MeteorTrackerEvents() {
	}

	@SubscribeEvent
	public static void indexExistingMeteor(ChunkEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel level)) {
			return;
		}
		LevelChunkSection[] sections = event.getChunk().getSections();
		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			LevelChunkSection section = sections[sectionIndex];
			if (!section.maybeHas(state -> state.is(CrystalnexusModBlocks.METEORITE_SCRAP_BLOCK.get()))) {
				continue;
			}
			int minX = event.getChunk().getPos().getMinBlockX();
			int minY = level.getSectionYFromSectionIndex(sectionIndex) << 4;
			int minZ = event.getChunk().getPos().getMinBlockZ();
			for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
				if (section.getBlockState(x, y, z).is(CrystalnexusModBlocks.METEORITE_SCRAP_BLOCK.get())) {
					MeteorTrackerSavedData.get(level).add(new BlockPos(minX + x, minY + y, minZ + z));
					return;
				}
			}
		}
	}
}
