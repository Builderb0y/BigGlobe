package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

/**
data about a specific waypoint that is known to the server.
the displayed position of a server waypoint is
always the same as its destination position.
*/
public record ServerWaypointData(
	PackedWorldPos position,
	int id,
	@Nullable UUID owner
)
implements WaypointData {

	@Override
	public PackedWorldPos destinationPosition() {
		return this.position;
	}

	@Override
	public PackedWorldPos displayPosition() {
		return this.position;
	}

	public PlayerWaypointData relativize(PackedWorldPos entrance) {
		double x = this.position.x() - entrance.x();
		double y = this.position.y() - entrance.y();
		double z = this.position.z() - entrance.z();
		if (x != 0.0D || y != 0.0D || z != 0.0D) {
			double scalar = 1.0D / Math.sqrt(Math.sqrt(BigGlobeMath.squareD(x, y, z)));
			x *= scalar;
			y *= scalar;
			z *= scalar;
		}
		return new PlayerWaypointData(this, new PackedWorldPos(HyperspaceConstants.WORLD_KEY, x, y, z));
	}

	public PlayerWaypointData absolutize() {
		return new PlayerWaypointData(this, this.position);
	}

	public PlayerWaypointData toClientData(PackedWorldPos entrance) {
		return entrance != null ? this.relativize(entrance) : this.absolutize();
	}

	public NbtCompound toNBT() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("world", this.position.world().getValue().toString());

		NbtList position = new NbtList();
		position.add(NbtDouble.of(this.position.x()));
		position.add(NbtDouble.of(this.position.y()));
		position.add(NbtDouble.of(this.position.z()));
		nbt.put("pos", position);

		nbt.putInt("id", this.id);
		if (this.owner != null) nbt.putUuid("owner", this.owner);
		return nbt;
	}

	public static @Nullable ServerWaypointData fromNBT(NbtCompound nbt) {
		RegistryKey<World> world;
		{
			String worldName = nbt.getString("world");
			if (worldName.isEmpty()) {
				BigGlobeMod.LOGGER.warn("Attempt to load waypoint with no world: " + nbt);
				return null;
			}
			Identifier worldIdentifier;
			try {
				worldIdentifier = new Identifier(worldName);
			}
			catch (InvalidIdentifierException exception) {
				BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid world: " + nbt, exception);
				return null;
			}
			world = RegistryKey.of(RegistryKeyVersions.world(), worldIdentifier);
		}

		PackedWorldPos position;
		{
			NbtList positionNBT = nbt.getList("pos", NbtElement.DOUBLE_TYPE);
			if (positionNBT.size() == 3) {
				position = new PackedWorldPos(world, positionNBT.getDouble(0), positionNBT.getDouble(1), positionNBT.getDouble(2));
			}
			else {
				BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid position: " + nbt);
				return null;
			}
		}

		int id = nbt.getInt("id");

		UUID owner;
		{
			NbtElement ownerNBT = nbt.get("owner");
			if (ownerNBT != null) try {
				owner = NbtHelper.toUuid(ownerNBT);
			}
			catch (IllegalArgumentException exception) {
				BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid owner: " + nbt, exception);
				return null;
			}
			else {
				owner = null;
			}
		}

		return new ServerWaypointData(position, id, owner);
	}
}