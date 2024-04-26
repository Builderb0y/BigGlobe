package builderb0y.bigglobe.hyperspace;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.PersistentState;

import builderb0y.bigglobe.BigGlobeMod;

/**
manages waypoints. waypoints can be added or removed,
and looked up by owner or by chunk.
different subclasses of this class provide different views of all waypoints.
see the documentation for each subclass for more details.
*/
public abstract class WaypointManager<D extends WaypointData> extends PersistentState {

	public WaypointLookup<D> allWaypoints = new WaypointLookup<>();
	public Map<@Nullable UUID, WaypointLookup<D>> byOwner = new HashMap<>(16);
	public Map<WorldChunkPos, WaypointLookup<D>> byChunk = new HashMap<>(16);

	public Collection<D> getAllWaypoints() {
		return this.allWaypoints.values();
	}

	public @Nullable D getWaypoint(@Nullable UUID owner, @NotNull UUID uuid) {
		Map<UUID, D> ownedByOwner = this.byOwner.get(owner);
		return ownedByOwner != null ? ownedByOwner.get(uuid) : null;
	}

	public Collection<D> getWaypointsInChunk(WorldChunkPos pos) {
		Map<UUID, D> map = this.byChunk.get(pos);
		return map != null ? map.values() : Collections.emptySet();
	}

	public boolean addWaypoint(D waypoint, boolean sync) {
		if (waypoint.destinationPosition().world() == HyperspaceConstants.WORLD_KEY) {
			BigGlobeMod.LOGGER.warn("Attempt to add waypoint to hyperspace: " + waypoint);
			return false;
		}
		D old = this.allWaypoints.putIfAbsent(waypoint.uuid(), waypoint);
		if (old != null) {
			BigGlobeMod.LOGGER.warn("Attempt to add duplicate waypoint " + old + " -> " + waypoint);
			return false;
		}
		this.byOwner.computeIfAbsent(waypoint.owner(), $ -> new WaypointLookup<>()).put(waypoint.uuid(), waypoint);
		this.byChunk.computeIfAbsent(waypoint.displayChunkPos(), $ -> new WaypointLookup<>()).put(waypoint.uuid(), waypoint);
		return true;
	}

	public @Nullable D removeWaypoint(UUID owner, UUID uuid, boolean sync) {
		D removed = this.allWaypoints.remove(uuid);
		if (removed == null) {
			BigGlobeMod.LOGGER.warn("Attempt to remove non-existent waypoint with UUID " + uuid + " owned by " + owner);
			return null;
		}
		WaypointLookup<D> byOwner = this.byOwner.get(owner);
		byOwner.remove(uuid);
		if (byOwner.isEmpty()) this.byOwner.remove(owner);

		WorldChunkPos chunkPos = removed.displayChunkPos();
		WaypointLookup<D> byChunk = this.byChunk.get(chunkPos);
		byChunk.remove(uuid);
		if (byChunk.isEmpty()) this.byChunk.remove(chunkPos);

		return removed;
	}

	public void clear() {
		this.allWaypoints.clear();
		this.byOwner.clear();
		this.byChunk.clear();
	}
}