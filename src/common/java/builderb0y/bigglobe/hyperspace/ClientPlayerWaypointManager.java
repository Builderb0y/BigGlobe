package builderb0y.bigglobe.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntitySpawnReason;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.versions.EntityVersions;

/**
manages waypoints visible to a ClientPlayerEntity.
when waypoints are added or removed from this view (and syncing is enabled),
an entity will be summoned or discarded in the client's world.
entities are also added when a chunk containing a waypoint is loaded on the client.
*/
@Environment(EnvType.CLIENT)
public class ClientPlayerWaypointManager extends PlayerWaypointManager {

	public ClientPlayerWaypointManager(LocalPlayer player) {
		super(player);
	}

	public LocalPlayer clientPlayer() {
		return (LocalPlayer)(this.player);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.collapseProgress == 0 && this.getAllWaypoints().isEmpty() && EntityVersions.getWorld(this.player).dimension() == HyperspaceConstants.WORLD_KEY) {
			EntityVersions.getClientWorld(this.clientPlayer()).playSound(Minecraft.getInstance().player, Minecraft.getInstance().player, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.AMBIENT, 1.0F, 0.5F);
		}
	}

	@Override
	public void clear() {
		super.clear();
		ClientLevel world = EntityVersions.getClientWorld(this.clientPlayer());
		for (Entity entity : world.entitiesForRendering()) {
			if (entity instanceof WaypointEntity waypoint && waypoint.isFake) {
				entity.discard();
			}
		}
	}

	@Override
	public boolean addWaypoint(PlayerWaypointData waypoint, boolean sync) {
		if (super.addWaypoint(waypoint, sync)) {
			if (sync) {
				ClientLevel world = EntityVersions.getClientWorld(this.clientPlayer());
				WaypointEntity entity = BigGlobeEntityTypes.WAYPOINT.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
				if (entity != null) {
					entity.setPos(waypoint.displayPosition().x(), waypoint.displayPosition().y() - 1.0D, waypoint.displayPosition().z());
					entity.health = WaypointEntity.MAX_HEALTH;
					entity.isFake = true;
					entity.data = waypoint.destination();
					entity.setId(waypoint.destination().entityId());
					entity.setCustomName(waypoint.destination().name());
					entity.setColor(waypoint.destination().color());
					world.addEntity(entity);
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
			EntityVersions.getClientWorld(this.clientPlayer()).removeEntity(waypoint.destination().entityId(), RemovalReason.DISCARDED);
		}
		return waypoint;
	}
}