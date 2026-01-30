package net.crystalnexus;

import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import net.minecraft.resources.ResourceLocation;

public final class ModChunkTickets {
	private ModChunkTickets() {}

	public static final ResourceLocation DARK_MATTER_LOADER_ID =
			ResourceLocation.fromNamespaceAndPath("crystalnexus", "dark_matter_chunk_loader");

	public static TicketController DARK_MATTER_LOADER;

	public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
		DARK_MATTER_LOADER = new TicketController(DARK_MATTER_LOADER_ID);
		event.register(DARK_MATTER_LOADER);
	}
}
