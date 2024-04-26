package builderb0y.bigglobe.hyperspace;

import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

/**
a position used to sync waypoints to the client.
to save bandwidth, the position is quantized to the nearest 1/64'th of a block.
this allows positions out to the world border to be stored in an ordinary int.
*/
public record PackedWorldPos(RegistryKey<World> world, double x, double y, double z) {

	public static final PackedWorldPos ZERO = new PackedWorldPos(World.OVERWORLD, 0.0D, 0.0D, 0.0D);

	public PackedWorldPos(RegistryKey<World> world, int x, int y, int z) {
		this(world, unpack(x), unpack(y), unpack(z));
	}

	public PackedWorldPos(RegistryKey<World> world, Vec3d vector) {
		this(world, vector.x, vector.y, vector.z);
	}

	public PackedWorldPos(RegistryKey<World> world, Vector3dc vector) {
		this(world, vector.x(), vector.y(), vector.z());
	}

	public Vec3d toMCVec() {
		return new Vec3d(this.x, this.y, this.z);
	}

	public Vector3d toJomlVec() {
		return new Vector3d(this.x, this.y, this.z);
	}

	public int packedX() {
		return pack(this.x);
	}

	public int packedY() {
		return pack(this.y);
	}

	public int packedZ() {
		return pack(this.z);
	}

	public static double unpack(int coordinate) {
		return coordinate * 0.015625D;
	}

	public static int pack(double coordinate) {
		return BigGlobeMath.floorI(coordinate * 64.0D);
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeRegistryKey(this.world);
		buffer.writeInt(this.packedX()).writeInt(this.packedY()).writeInt(this.packedZ());
	}

	public static PackedWorldPos read(PacketByteBuf buffer) {
		return new PackedWorldPos(buffer.readRegistryKey(RegistryKeyVersions.world()), buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	public void writeBulk(PacketByteBuf buffer, Object2IntMap<RegistryKey<World>> worlds) {
		buffer.writeVarInt(worlds.getInt(this.world));
		buffer.writeInt(this.packedX()).writeInt(this.packedY()).writeInt(this.packedZ());
	}

	public static PackedWorldPos readBulk(PacketByteBuf buffer, List<RegistryKey<World>> worlds) {
		return new PackedWorldPos(worlds.get(buffer.readVarInt()), buffer.readInt(), buffer.readInt(), buffer.readInt());
	}
}