package builderb0y.bigglobe.hyperspace;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.networking.packets.WaypointAddS2CPacket;
import builderb0y.bigglobe.networking.packets.WaypointListS2CPacket;
import builderb0y.bigglobe.networking.packets.WaypointRemoveS2CPacket;
import builderb0y.bigglobe.versions.EntityVersions;

/**
manages waypoints that are visible to a ServerPlayerEntity.
when waypoints are added or removed this view (and syncing is enabled),
a packet will be sent to the associated client automatically to update their view too.
*/
public class ServerPlayerWaypointManager extends PlayerWaypointManager {

	public ServerPlayerWaypointManager(ServerPlayerEntity player) {
		super(player);
	}

	public ServerPlayerEntity serverPlayer() {
		return (ServerPlayerEntity)(this.player);
	}

	public void updateOnWorldChange() {
		ServerWaypointManager manager = ServerWaypointManager.get(EntityVersions.getServerWorld(this.serverPlayer()));
		if (manager != null) {
			this.clear();
			manager.getVisibleWaypoints(this.serverPlayer()).forEach((ServerWaypointData waypoint) -> this.addWaypoint(waypoint.toClientData(this.entrance != null ? this.entrance.position() : null), false));
			WaypointListS2CPacket.INSTANCE.send(this.serverPlayer());
		}
	}

	@Override
	public boolean addWaypoint(PlayerWaypointData waypoint, boolean sync) {
		if (super.addWaypoint(waypoint, sync)) {
			if (sync) {
				WaypointAddS2CPacket.INSTANCE.send(this, waypoint.sync());
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public @Nullable PlayerWaypointData removeWaypoint(int id, boolean sync) {
		PlayerWaypointData removed = super.removeWaypoint(id, sync);
		if (removed != null && sync) {
			WaypointRemoveS2CPacket.INSTANCE.send(this.serverPlayer(), id);
		}
		return removed;
	}
}