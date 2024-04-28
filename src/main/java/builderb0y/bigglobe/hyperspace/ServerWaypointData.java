package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.util.TextCoding;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

/**
data about a specific waypoint that is known to the server.
the displayed position of a server waypoint is
always the same as its destination position.
*/
public record ServerWaypointData(
	int id,
	@Nullable UUID owner,
	PackedWorldPos position,
	@Nullable Text name
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

	public ServerWaypointData withName(Text name) {
		return new ServerWaypointData(this.id, this.owner, this.position, name);
	}

	public PlayerWaypointData relativize(PackedPos entrance) {
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

	public PlayerWaypointData toClientData(PackedPos entrance) {
		return entrance != null ? this.relativize(entrance) : this.absolutize();
	}

	public NbtCompound toNBT() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("world", this.position.world().getValue().toString());
		nbt.put("pos", this.position.pos().toNbt());

		nbt.putInt("id", this.id);
		if (this.owner != null) nbt.putUuid("owner", this.owner);

		if (this.name != null) {
			NbtElement nbtName = TextCoding.toNbt(this.name);
			if (nbtName != null) nbt.put("name", nbtName);
		}
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

		Text name = TextCoding.fromNbt(nbt.get("name"));

		return new ServerWaypointData(id, owner, position, name);
	}
}