package builderb0y.bigglobe.hyperspace;

import builderb0y.bigglobe.BigGlobeMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.TicketType;

public class HyperspaceCollapseTicketType {

	public static final TicketType TYPE = Registry.register(
		BuiltInRegistries.TICKET_TYPE,
		BigGlobeMod.modID("hyperspace_ejection"),
		new TicketType(
			PlayerWaypointManager.COLLAPSE_DURATION_TICKS + 20,
			TicketType.FLAG_LOADING
		)
	);

	public static void init() {
	}
}