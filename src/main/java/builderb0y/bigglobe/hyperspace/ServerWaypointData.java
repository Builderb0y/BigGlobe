package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public record ServerWaypointData(
	RegistryKey<World> world,
	PackedPosition position,
	UUID uuid,
	@Nullable UUID owner
)
implements WaypointData {

	public ClientWaypointData relativize(PackedPosition entrance) {
		double x = this.position.x() - entrance.x();
		double y = this.position.y() - entrance.y();
		double z = this.position.z() - entrance.z();
		if (x != 0.0D || y != 0.0D || z != 0.0D) {
			double scalar = 1.0D / Math.sqrt(Math.sqrt(BigGlobeMath.squareD(x, y, z)));
			x *= scalar;
			y *= scalar;
			z *= scalar;
		}
		return new ClientWaypointData(this, new PackedPosition(x, y, z));
	}

	public ClientWaypointData absolutize() {
		return new ClientWaypointData(this, this.position);
	}

	public ClientWaypointData toClientData(PackedPosition entrance) {
		return entrance != null ? this.relativize(entrance) : this.absolutize();
	}

	public NbtCompound toNBT() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("world", this.world.getValue().toString());

		NbtList position = new NbtList();
		position.add(NbtDouble.of(this.position.x()));
		position.add(NbtDouble.of(this.position.y()));
		position.add(NbtDouble.of(this.position.z()));
		nbt.put("pos", position);

		nbt.putUuid("uuid", this.uuid);
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

		PackedPosition position;
		{
			NbtList positionNBT = nbt.getList("pos", NbtElement.DOUBLE_TYPE);
			if (positionNBT.size() == 3) {
				position = new PackedPosition(positionNBT.getDouble(0), positionNBT.getDouble(1), positionNBT.getDouble(2));
			}
			else {
				BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid position: " + nbt);
				return null;
			}
		}

		UUID uuid;
		try {
			uuid = nbt.getUuid("uuid");
		}
		catch (IllegalArgumentException exception) {
			BigGlobeMod.LOGGER.warn("Attempt to load waypoint with invalid UUID: " + nbt, exception);
			return null;
		}

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

		return new ServerWaypointData(world, position, uuid, owner);
	}

	public void toIncrementalByteBuffer(PacketByteBuf buffer) {
		buffer.writeRegistryKey(this.world);
		buffer
			.writeDouble(this.position.x())
			.writeDouble(this.position.y())
			.writeDouble(this.position.z())
			.writeUuid(this.uuid)
			.writeBoolean(this.owner != null);
	}

	public static ServerWaypointData fromIncrementalByteBuffer(PacketByteBuf buffer, UUID owner) {
		RegistryKey<World> world = buffer.readRegistryKey(RegistryKeyVersions.world());
		double x = buffer.readDouble();
		double y = buffer.readDouble();
		double z = buffer.readDouble();
		UUID uuid = buffer.readUuid();
		return new ServerWaypointData(world, new PackedPosition(x, y, z), uuid, owner);
	}

	public void toBulkByteBuffer(PacketByteBuf buffer, Object2IntMap<RegistryKey<World>> worldIDs) {
		buffer
			.writeVarInt(worldIDs.getInt(this.world))
			.writeDouble(this.position.x())
			.writeDouble(this.position.y())
			.writeDouble(this.position.z())
			.writeUuid(this.uuid);
	}

	public static ServerWaypointData fromBulkByteBuffer(PacketByteBuf buffer, Int2ObjectMap<RegistryKey<World>> worldIDs, UUID owner) {
		RegistryKey<World> world = worldIDs.get(buffer.readVarInt());
		double x = buffer.readDouble();
		double y = buffer.readDouble();
		double z = buffer.readDouble();
		UUID uuid = buffer.readUuid();
		return new ServerWaypointData(world, new PackedPosition(x, y, z), uuid, owner);
	}
}