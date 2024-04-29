package builderb0y.bigglobe.hyperspace;

import java.util.*;

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

	public Collection<D> getAllWaypoints() {
		return this.allWaypoints.values();
	}

	public @Nullable D getWaypoint(int id) {
		return this.allWaypoints.get(id);
	}

	public boolean addWaypoint(D waypoint, boolean sync) {
		if (waypoint.destinationPosition().world() == HyperspaceConstants.WORLD_KEY) {
			BigGlobeMod.LOGGER.warn("Attempt to add waypoint to hyperspace: " + waypoint);
			return false;
		}
		D old = this.allWaypoints.putIfAbsent(waypoint.id(), waypoint);
		if (old != null) {
			BigGlobeMod.LOGGER.warn("Attempt to add duplicate waypoint " + old + " -> " + waypoint);
			return false;
		}
		this.byOwner.computeIfAbsent(waypoint.owner(), (@Nullable UUID $) -> new WaypointLookup<>()).put(waypoint.id(), waypoint);
		this.markDirty();
		return true;
	}

	public @Nullable D removeWaypoint(int id, boolean sync) {
		D removed = this.allWaypoints.remove(id);
		if (removed == null) {
			BigGlobeMod.LOGGER.warn("Attempt to remove non-existent waypoint with ID " + id);
			return null;
		}
		WaypointLookup<D> byOwner = this.byOwner.get(removed.owner());
		byOwner.remove(id);
		if (byOwner.isEmpty()) this.byOwner.remove(removed.owner());

		this.markDirty();

		return removed;
	}

	public void clear() {
		this.allWaypoints.clear();
		this.byOwner.clear();

		this.markDirty();
	}
}