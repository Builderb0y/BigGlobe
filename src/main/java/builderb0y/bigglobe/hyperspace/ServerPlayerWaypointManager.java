package builderb0y.bigglobe.hyperspace;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.networking.packets.WaypointAddS2CPacket;
import builderb0y.bigglobe.networking.packets.WaypointListS2CPacket;
import builderb0y.bigglobe.networking.packets.WaypointRemoveS2CPacket;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;
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

	@Override
	public void tick() {
		super.tick();
		if (this.getAllWaypoints().isEmpty()) {
			float targetHealth = switch (this.collapseProgress) {
				case 20 * 5 -> 16.0F;
				case 20 * 10 -> 10.0F;
				case 20 * 15 -> 1.0F;
				default -> Float.POSITIVE_INFINITY;
			};
			if (this.player.getHealth() > targetHealth) {
				this.player.damage(EntityVersions.getServerWorld(this.serverPlayer()), this.player.getDamageSources().outOfWorld(), this.player.getHealth() - targetHealth);
			}
			if (this.collapseProgress >= COLLAPSE_DURATION_TICKS) {
				this.collapseProgress = -1;
				ServerWorld overworld = EntityVersions.getServerWorld(this.serverPlayer()).getServer().getOverworld();
				double radius = BigGlobeConfig.INSTANCE.get().playerSpawning.maxSpawnRadius;
				int x = BigGlobeMath.floorI(Permuter.nextBoundedDouble(this.player.getRandom().nextLong(), -radius, radius));
				int z = BigGlobeMath.floorI(Permuter.nextBoundedDouble(this.player.getRandom().nextLong(), -radius, radius));
				int y = overworld.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true).sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x, z) + 1;
				EntityVersions.teleport(this.serverPlayer(), overworld, new Vec3d(x + 0.5D, y, z + 0.5D), Vec3d.ZERO, this.player.getYaw(), this.player.getPitch());
			}
		}
	}

	public void updateOnWorldChange(ServerWorld world) {
		ServerWaypointManager manager = ServerWaypointManager.get(world);
		if (manager != null) {
			this.clear();
			manager.getVisibleWaypoints(this.serverPlayer()).forEach((ServerWaypointData waypoint) -> this.addWaypoint(waypoint.toClientData(this.entrance != null ? this.entrance.pos() : null), false));
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