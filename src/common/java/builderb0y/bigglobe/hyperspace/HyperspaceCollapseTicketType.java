package builderb0y.bigglobe.hyperspace;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.TicketType;

import builderb0y.bigglobe.BigGlobeMod;

public class HyperspaceCollapseTicketType {

	public static final TicketType TYPE = Registry.register(
		BuiltInRegistries.TICKET_TYPE,
		BigGlobeMod.modID("hyperspace_ejection"),
		new TicketType(
			PlayerWaypointManager.COLLAPSE_DURATION_TICKS + 20,
			TicketType.FLAG_LOADING
		)
	);

	public static void init() {}
}