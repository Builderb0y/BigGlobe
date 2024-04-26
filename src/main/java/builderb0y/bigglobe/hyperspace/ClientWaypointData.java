package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.math.BigGlobeMath;

public record ClientWaypointData(
	ServerWaypointData destination,
	PackedPosition clientPosition
)
implements WaypointData {

	@Override
	public UUID uuid() {
		return this.destination.uuid();
	}

	@Override
	public UUID owner() {
		return this.destination.owner();
	}

	@Override
	public RegistryKey<World> world() {
		return this.destination.world();
	}

	public ChunkPos chunkPos() {
		return new ChunkPos(
			BigGlobeMath.floorI(this.clientPosition.x()) >> 4,
			BigGlobeMath.floorI(this.clientPosition.z()) >> 4
		);
	}
}