package builderb0y.bigglobe.hyperspace;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ChunkTicketType;

import builderb0y.bigglobe.BigGlobeMod;

public class HyperspaceCollapseTicketType {

	public static final ChunkTicketType TYPE = Registry.register(
		Registries.TICKET_TYPE,
		BigGlobeMod.modID("hyperspace_ejection"),
		new ChunkTicketType(
			PlayerWaypointManager.COLLAPSE_DURATION_TICKS + 20,
			ChunkTicketType.FOR_LOADING
		)
	);

	public static void init() {}
}