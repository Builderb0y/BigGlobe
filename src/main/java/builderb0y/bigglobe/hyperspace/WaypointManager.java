package builderb0y.bigglobe.hyperspace;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;

public abstract class WaypointManager<D extends WaypointData> extends PersistentState {

	public Map<@Nullable UUID, WaypointList<D>> owners = new HashMap<>(16);

	public @Nullable D getWaypoint(@Nullable UUID owner, @NotNull UUID uuid) {
		WaypointList<D> list = this.owners.get(owner);
		return list != null ? list.waypoints.get(uuid) : null;
	}

	public boolean addWaypoint(D waypoint, boolean executeCallbacks) {
		if (waypoint.world() == HyperspaceConstants.WORLD_KEY) {
			BigGlobeMod.LOGGER.warn("Attempt to add waypoint to hyperspace: " + waypoint);
			return false;
		}
		if (this.forUUID(waypoint.owner(), true).add(waypoint)) {
			this.markDirty();
			return true;
		}
		else {
			return false;
		}
	}

	public @Nullable D removeWaypoint(UUID owner, UUID uuid, boolean executeCallbacks) {
		WaypointList<D> list = this.forUUID(owner, false);
		if (list != null) {
			D removed = list.remove(uuid);
			if (removed != null) {
				this.markDirty();
			}
			return removed;
		}
		else {
			BigGlobeMod.LOGGER.warn("Attempt to remove " + uuid + " from player who doesn't have any waypoints.");
			return null;
		}
	}

	public WaypointList<D> forUUID(@Nullable UUID uuid, boolean create) {
		if (create) {
			return this.owners.computeIfAbsent(uuid, WaypointList::new);
		}
		else {
			return this.owners.get(uuid);
		}
	}

	public WaypointList<D> forPlayer(PlayerEntity player, boolean create) {
		if (create) {
			return this.owners.computeIfAbsent(player.getGameProfile().getId(), WaypointList::new);
		}
		else {
			return this.owners.get(player.getGameProfile().getId());
		}
	}

	public Stream<D> getAllWaypoints() {
		return this.owners.values().stream().flatMap((WaypointList<D> list) -> list.waypoints.values().stream());
	}

	public Stream<D> getRelevantWaypoints(@Nullable UUID playerUUID, RegistryKey<World> world) {
		Stream<D> stream;
		WaypointList<D> global = this.owners.get(null);
		if (playerUUID == null) {
			if (global != null && !global.waypoints.isEmpty()) {
				stream = global.waypoints.values().stream();
			}
			else {
				return Stream.empty();
			}
		}
		else {
			WaypointList<D> owned = this.owners.get(playerUUID);
			if (global != null && !global.waypoints.isEmpty()) {
				if (owned != null && !owned.waypoints.isEmpty()) {
					stream = Stream.concat(global.waypoints.values().stream(), owned.waypoints.values().stream());
				}
				else {
					stream = global.waypoints.values().stream();
				}
			}
			else {
				if (owned != null && !owned.waypoints.isEmpty()) {
					stream = owned.waypoints.values().stream();
				}
				else {
					return Stream.empty();
				}
			}
		}
		if (world != HyperspaceConstants.WORLD_KEY) {
			stream = stream.filter((D data) -> data.world() == world);
		}
		return stream;
	}

	public static class WaypointList<D extends WaypointData> {

		public @Nullable UUID owner;
		public Map<UUID, D> waypoints;

		public WaypointList(@Nullable UUID owner) {
			this.owner = owner;
			this.waypoints = new HashMap<>(16);
		}

		public boolean add(D waypoint) {
			if (!Objects.equals(this.owner, waypoint.owner())) {
				throw new IllegalArgumentException("Attempt to add " + waypoint + " to wrong WaypointList owned by " + this.owner);
			}
			if (this.waypoints.putIfAbsent(waypoint.uuid(), waypoint) == null) {
				return true;
			}
			else {
				BigGlobeMod.LOGGER.warn("Attempt to add duplicate waypoint: " + waypoint);
				return false;
			}
		}

		public D remove(UUID uuid) {
			D removed = this.waypoints.remove(uuid);
			if (removed == null) {
				BigGlobeMod.LOGGER.warn("Attempt to remove non-existent waypoint: " + uuid);
			}
			return removed;
		}
	}
}