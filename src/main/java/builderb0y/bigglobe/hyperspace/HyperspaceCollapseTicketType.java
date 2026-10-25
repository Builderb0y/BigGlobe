package builderb0y.bigglobe.hyperspace;

import java.util.Comparator;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

public class HyperspaceCollapseTicketType {

	public static final TicketType<ChunkPos> TYPE = TicketType.create(
		"bigglobe_hyperspace_ejection",
		Comparator.comparingLong(ChunkPos::toLong),
		PlayerWaypointManager.COLLAPSE_DURATION_TICKS + 20
	);

	public static void init() {}
}
