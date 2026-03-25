package builderb0y.bigglobe.hyperspace;

import java.util.Comparator;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;

import builderb0y.bigglobe.BigGlobeMod;

public class HyperspaceCollapseTicketType {

	#if MC_VERSION >= MC_1_21_9

		public static final ChunkTicketType TYPE = Registry.register(
			Registries.TICKET_TYPE,
			BigGlobeMod.modID("hyperspace_ejection"),
			new ChunkTicketType(
				PlayerWaypointManager.COLLAPSE_DURATION_TICKS + 20,
				ChunkTicketType.FOR_LOADING
			)
		);

	#elif MC_VERSION >= MC_1_21_5

		public static final ChunkTicketType TYPE = Registry.register(
			Registries.TICKET_TYPE,
			BigGlobeMod.modID("hyperspace_ejection"),
			new ChunkTicketType(
				PlayerWaypointManager.COLLAPSE_DURATION_TICKS + 20,
				false,
				ChunkTicketType.Use.LOADING
			)
		);

	#else

		public static final ChunkTicketType<ChunkPos> TYPE = ChunkTicketType.create(
			"bigglobe_hyperspace_ejection",
			Comparator.comparingLong(ChunkPos::toLong),
			PlayerWaypointManager.COLLAPSE_DURATION_TICKS + 20
		);

	#endif

	public static void init() {}
}