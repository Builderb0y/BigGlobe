package builderb0y.bigglobe.hyperspace;

import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.versions.GameProfileVersions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
information about a waypoint for the purposes of syncing that waypoint to a client.
since the player can only see public waypoints and their own private waypoints,
it is wasteful to sync the entire UUID of the waypoint's owner.
instead, we sync a single boolean. if that boolean is true,
then the owner of the waypoint is assumed to be the player receiving it.
otherwise, the waypoint is assumed to be public.
*/
public record SyncedWaypointData(
	int id,
	int entityId,
	boolean owned,
	PackedWorldPos destinationPosition,
	PackedWorldPos displayedPosition,
	@Nullable Component name,
	CloudColor color
) {

	public PlayerWaypointData resolve(Player player) {
		return new PlayerWaypointData(
			new ServerWaypointData(
				this.id,
				this.entityId,
				this.owned ? GameProfileVersions.getUUID(player.getGameProfile()) : null,
				this.destinationPosition,
				this.name,
				this.color
			),
			this.displayedPosition
		);
	}
}