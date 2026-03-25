package builderb0y.bigglobe.hyperspace;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.networking.packets.WaypointAddS2CPacket;
import builderb0y.bigglobe.networking.packets.WaypointListS2CPacket;
import builderb0y.bigglobe.networking.packets.WaypointRemoveS2CPacket;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.EntityVersions;

/**
manages waypoints that are visible to a ServerPlayerEntity.
when waypoints are added or removed this view (and syncing is enabled),
a packet will be sent to the associated client automatically to update their view too.
*/
public class ServerPlayerWaypointManager extends PlayerWaypointManager {

	public int ejectX, ejectZ;

	public ServerPlayerWaypointManager(ServerPlayerEntity player) {
		super(player);
	}

	public ServerPlayerEntity serverPlayer() {
		return (ServerPlayerEntity)(this.player);
	}

	@Override
	public void tick() {
		int oldCollapseProgress = this.collapseProgress;
		super.tick();
		switch (this.collapseProgress) {
			case 0 -> {
				if (oldCollapseProgress < 0) {
					double radius = BigGlobeConfig.INSTANCE.get().playerSpawning.maxSpawnRadius;
					this.ejectX = BigGlobeMath.floorI(Permuter.nextBoundedDouble(this.player.getRandom().nextLong(), -radius, radius));
					this.ejectZ = BigGlobeMath.floorI(Permuter.nextBoundedDouble(this.player.getRandom().nextLong(), -radius, radius));
					MinecraftServer server = EntityVersions.getServerWorld(this.serverPlayer()).getServer();
					ServerWorld overworld = server.getOverworld();
					ChunkPos pos = new ChunkPos(this.ejectX >> 4, this.ejectZ >> 4);
					overworld.getChunkManager().addTicket(
						HyperspaceCollapseTicketType.TYPE,
						pos,
						server.getPlayerManager().getViewDistance()
						#if MC_VERSION < MC_1_21_5 , pos #endif
					);
				}
			}
			case 20 * 5 -> {
				this.reducePlayerHealthTo(this.player.getMaxHealth() * 0.75F);
			}
			case 20 * 10 -> {
				this.reducePlayerHealthTo(this.player.getMaxHealth() * 0.5F);
			}
			case 20 * 15 -> {
				this.reducePlayerHealthTo(1.0F);
			}
			case COLLAPSE_DURATION_TICKS -> {
				ServerWorld overworld = EntityVersions.getServerWorld(this.serverPlayer()).getServer().getOverworld();
				int y = overworld.getChunk(this.ejectX >> 4, this.ejectZ >> 4, ChunkStatus.FULL, true).sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, this.ejectX, this.ejectZ) + 1;
				EntityVersions.teleport(this.serverPlayer(), overworld, new Vec3d(this.ejectX + 0.5D, y, this.ejectZ + 0.5D), Vec3d.ZERO, this.player.getYaw(), this.player.getPitch());
			}
		}
	}

	public void reducePlayerHealthTo(float targetHealth) {
		if (this.player.getHealth() > targetHealth) {
			this.player.damage(
				#if MC_VERSION >= MC_1_21_2
					EntityVersions.getServerWorld(this.serverPlayer()),
				#endif
				this.player.getDamageSources().outOfWorld(),
				this.player.getHealth() - targetHealth
			);
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