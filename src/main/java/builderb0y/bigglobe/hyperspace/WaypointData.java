package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import it.unimi.dsi.fastutil.Hash;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.math.BigGlobeMath;

/**
data about a specific waypoint.
this data includes things like who owns it, where it is, and so on.
*/
public interface WaypointData {

	public static final Hash.Strategy<WaypointData> UUID_STRATEGY = HashStrategies.map(HashStrategies.defaultStrategy(), WaypointData::uuid);

	public abstract UUID uuid();

	public abstract UUID owner();

	public abstract PackedWorldPos destinationPosition();

	public abstract PackedWorldPos displayPosition();

	public default WorldChunkPos displayChunkPos() {
		return new WorldChunkPos(
			this.displayPosition().world(),
			BigGlobeMath.floorI(this.displayPosition().x()) >> 4,
			BigGlobeMath.floorI(this.displayPosition().z()) >> 4
		);
	}
}