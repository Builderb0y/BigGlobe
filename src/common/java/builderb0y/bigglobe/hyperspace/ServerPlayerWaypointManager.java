package builderb0y.bigglobe.hyperspace;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
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

	public ServerPlayerWaypointManager(ServerPlayer player) {
		super(player);
	}

	public ServerPlayer serverPlayer() {
		return (ServerPlayer)(this.player);
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
					ServerLevel overworld = server.overworld();
					ChunkPos pos = new ChunkPos(this.ejectX >> 4, this.ejectZ >> 4);
					overworld.getChunkSource().addTicketWithRadius(
						HyperspaceCollapseTicketType.TYPE,
						pos,
						server.getPlayerList().getViewDistance()

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
				ServerLevel overworld = EntityVersions.getServerWorld(this.serverPlayer()).getServer().overworld();
				int y = overworld.getChunk(this.ejectX >> 4, this.ejectZ >> 4, ChunkStatus.FULL, true).getHeight(Heightmap.Types.OCEAN_FLOOR, this.ejectX, this.ejectZ) + 1;
				EntityVersions.teleport(this.serverPlayer(), overworld, new Vec3(this.ejectX + 0.5D, y, this.ejectZ + 0.5D), Vec3.ZERO, this.player.getYRot(), this.player.getXRot());
			}
		}
	}

	public void reducePlayerHealthTo(float targetHealth) {
		if (this.player.getHealth() > targetHealth) {
			this.player.hurtServer(

				EntityVersions.getServerWorld(this.serverPlayer()),

				this.player.damageSources().fellOutOfWorld(),
				this.player.getHealth() - targetHealth
			);
		}
	}

	public void updateOnWorldChange(ServerLevel world) {
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