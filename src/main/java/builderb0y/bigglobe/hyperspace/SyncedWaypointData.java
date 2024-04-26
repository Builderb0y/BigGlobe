package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;

/**
information about a waypoint for the purposes of syncing that waypoint to a client.
since the player can only see public waypoints and their own private waypoints,
it is wasteful to sync the entire UUID of the waypoint's owner.
instead, we sync a single boolean. if that boolean is true,
then the owner of the waypoint is assumed to be the player receiving it.
otherwise, the waypoint is assumed to be public.
*/
public record SyncedWaypointData(
	UUID uuid,
	boolean owned,
	PackedWorldPos destinationPosition,
	PackedWorldPos displayedPosition
) {

	public PlayerWaypointData resolve(PlayerEntity player) {
		return new PlayerWaypointData(
			new ServerWaypointData(
				this.destinationPosition,
				this.uuid,
				this.owned ? player.getGameProfile().getId() : null
			),
			this.displayedPosition
		);
	}
}