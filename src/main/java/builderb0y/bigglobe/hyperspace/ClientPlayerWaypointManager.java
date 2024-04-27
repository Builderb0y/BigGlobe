package builderb0y.bigglobe.hyperspace;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.versions.BlockPosVersions;

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

	public static void init() {
		ClientChunkEvents.CHUNK_LOAD.register((ClientWorld world, WorldChunk chunk) -> {
			//MinecraftClient
			//.getInstance()
			//.player
			//.as(WaypointTracker)
			//.bigglobe_getWaypointManager()
			//.as(ClientPlayerWaypointManager)
			//.onChunkLoaded(world, chunk)
			(
				(ClientPlayerWaypointManager)(
					(
						(WaypointTracker)(
							MinecraftClient.getInstance().player
						)
					)
					.bigglobe_getWaypointManager()
				)
			)
			.onChunkLoaded(world, chunk);
		});
	}

	public ClientPlayerEntity clientPlayer() {
		return (ClientPlayerEntity)(this.player);
	}

	@Override
	public void clear() {
		super.clear();
		ClientWorld world = this.clientPlayer().clientWorld;
		if (world.getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
			for (Entity entity : world.getEntities()) {
				if (entity instanceof WaypointEntity waypoint && waypoint.isFake) {
					entity.discard();
				}
			}
		}
	}

	@Override
	public boolean addWaypoint(PlayerWaypointData waypoint, boolean sync) {
		if (super.addWaypoint(waypoint, sync)) {
			if (BigGlobeEntityTypes.WAYPOINT != null && sync) {
				ClientWorld world = this.clientPlayer().clientWorld;
				if (world.isChunkLoaded(BlockPosVersions.floor(waypoint.displayPosition().x(), waypoint.displayPosition().y() - 1.0D, waypoint.displayPosition().z()))) {
					WaypointEntity entity = BigGlobeEntityTypes.WAYPOINT.create(world);
					if (entity != null) {
						entity.setPosition(waypoint.displayPosition().x(), waypoint.displayPosition().y() - 1.0D, waypoint.displayPosition().z());
						entity.setHealth(WaypointEntity.MAX_HEALTH);
						entity.isFake = true;
						entity.data = waypoint.destination();
						world.addEntity(entity);
					}
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
			ClientWorld world = this.clientPlayer().clientWorld;
			List<WaypointEntity> found = world.getEntitiesByClass(
				WaypointEntity.class,
				new Box(
					waypoint.displayPosition().x() - 1.0D,
					waypoint.displayPosition().y() - 1.0D,
					waypoint.displayPosition().z() - 1.0D,
					waypoint.displayPosition().x() + 1.0D,
					waypoint.displayPosition().y() + 1.0D,
					waypoint.displayPosition().z() + 1.0D
				),
				(WaypointEntity entity) -> entity.isFake && entity.data != null && entity.data.id() == waypoint.id()
			);
			switch (found.size()) {
				case 0 -> BigGlobeMod.LOGGER.warn("Did not find any waypoints in client world with ID " + waypoint.id());
				case 1 -> found.get(0).discard();
				default -> {
					BigGlobeMod.LOGGER.warn("Found more than one waypoint in client world with ID " + waypoint.id());
					found.forEach(WaypointEntity::discard);
				}
			}
		}
		return waypoint;
	}

	public void onChunkLoaded(ClientWorld world, WorldChunk chunk) {
		if (BigGlobeEntityTypes.WAYPOINT != null) {
			WaypointLookup<PlayerWaypointData> waypoints = this.byChunk.get(new WorldChunkPos(chunk));
			if (waypoints != null && !waypoints.isEmpty()) {
				for (PlayerWaypointData waypoint : waypoints.values()) {
					if (waypoint.displayPosition().y() - 1.0D >= chunk.getBottomY() && waypoint.displayPosition().y() - 1.0D < chunk.getTopY()) {
						WaypointEntity entity = BigGlobeEntityTypes.WAYPOINT.create(world);
						if (entity != null) {
							entity.setPosition(waypoint.displayPosition().x(), waypoint.displayPosition().y() - 1.0D, waypoint.displayPosition().z());
							entity.setHealth(WaypointEntity.MAX_HEALTH);
							entity.isFake = true;
							entity.data = waypoint.destination();
							world.addEntity(entity);
						}
					}
				}
			}
		}
	}
}