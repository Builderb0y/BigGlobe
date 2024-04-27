package builderb0y.bigglobe.hyperspace;

import org.joml.Vector3dc;

import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.math.BigGlobeMath;

/**
a position used to sync waypoints to the client.
to save bandwidth, the position is quantized to the nearest 1/64'th of a block.
this allows positions out to the world border to be stored in an ordinary int.
*/
public record PackedPos(double x, double y, double z) {

	public static final PackedPos ZERO = new PackedPos(0.0D, 0.0D, 0.0D);

	public PackedPos(int x, int y, int z) {
		this(unpack(x), unpack(y), unpack(z));
	}

	public PackedPos(Vec3d vector) {
		this(vector.x, vector.y, vector.z);
	}

	public PackedPos(Vector3dc vector) {
		this(vector.x(), vector.y(), vector.z());
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
		buffer.writeInt(this.packedX()).writeInt(this.packedY()).writeInt(this.packedZ());
	}

	public static PackedPos read(PacketByteBuf buffer) {
		return new PackedPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	public NbtList toNbt() {
		NbtList list = new NbtList();
		list.add(NbtDouble.of(this.x));
		list.add(NbtDouble.of(this.y));
		list.add(NbtDouble.of(this.z));
		return list;
	}

	public static PackedPos fromNbt(NbtList list) {
		return new PackedPos(list.getDouble(0), list.getDouble(1), list.getDouble(2));
	}
}