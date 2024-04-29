package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

/**
data about a waypoint which is visible to a specific player.
if the player is currently in hyperspace,
then the displayed position of the waypoint will almost
certainly not match up with its destination's position.
ont the other hand, if the player is NOT in hyperspace,
then the displayed position of the waypoint will always
match its destination position.
this is a necessary distinction because different players
might see the same waypoint at a different position in hyperspace.
*/
public record PlayerWaypointData(
	ServerWaypointData destination,
	PackedWorldPos displayPosition
)
implements WaypointData {

	@Override
	public int id() {
		return this.destination.id();
	}

	@Override
	public UUID owner() {
		return this.destination.owner();
	}

	@Override
	public PackedWorldPos destinationPosition() {
		return this.destination.position();
	}

	public SyncedWaypointData sync() {
		return new SyncedWaypointData(this.id(), this.destination.entityId(), this.owner() != null, this.destinationPosition(), this.displayPosition, this.destination.name());
	}
}