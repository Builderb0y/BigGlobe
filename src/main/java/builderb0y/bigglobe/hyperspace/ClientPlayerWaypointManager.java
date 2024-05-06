package builderb0y.bigglobe.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.WaypointEntity;

/**
manages waypoints visible to a ClientPlayerEntity.
when waypoints are added or removed from this view (and syncing is enabled),
an entity will be summoned or discarded in the client's world.
entities are also added when a chunk containing a waypoint is loaded on the client.
*/
@Environment(EnvType.CLIENT)
public class ClientPlayerWaypointManager extends PlayerWaypointManager {

	public ClientPlayerWaypointManager(ClientPlayerEntity player) {
		super(player);
	}

	public ClientPlayerEntity clientPlayer() {
		return (ClientPlayerEntity)(this.player);
	}

	@Override
	public void clear() {
		super.clear();
		ClientWorld world = this.clientPlayer().clientWorld;
		for (Entity entity : world.getEntities()) {
			if (entity instanceof WaypointEntity waypoint && waypoint.isFake) {
				entity.discard();
			}
		}
	}

	@Override
	public boolean addWaypoint(PlayerWaypointData waypoint, boolean sync) {
		if (super.addWaypoint(waypoint, sync)) {
			if (sync) {
				ClientWorld world = this.clientPlayer().clientWorld;
				WaypointEntity entity = BigGlobeEntityTypes.WAYPOINT.create(world);
				if (entity != null) {
					entity.setPosition(waypoint.displayPosition().x(), waypoint.displayPosition().y() - 1.0D, waypoint.displayPosition().z());
					entity.health = WaypointEntity.MAX_HEALTH;
					entity.isFake = true;
					entity.data = waypoint.destination();
					entity.setId(waypoint.destination().entityId());
					entity.setCustomName(waypoint.destination().name());
					world.addEntity(#if MC_VERSION < MC_1_20_2 entity.getId(), #endif entity);
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public PlayerWaypointData removeWaypoint(int id, boolean sync) {
		PlayerWaypointData waypoint = super.removeWaypoint(id, sync);
		if (waypoint != null && sync) {
			this.clientPlayer().clientWorld.removeEntity(waypoint.destination().entityId(), RemovalReason.DISCARDED);
		}
		return waypoint;
	}
}