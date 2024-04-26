package builderb0y.bigglobe.hyperspace;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.math.BigGlobeMath;

public record PackedPosition(double x, double y, double z) {

	public static final PackedPosition ZERO = new PackedPosition(0.0D, 0.0D, 0.0D);

	public PackedPosition(int x, int y, int z) {
		this(unpack(x), unpack(y), unpack(z));
	}

	public PackedPosition(Vec3d vector) {
		this(vector.x, vector.y, vector.z);
	}

	public PackedPosition(Vector3dc vector) {
		this(vector.x(), vector.y(), vector.z());
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
		buffer.writeInt(this.packedX()).writeInt(this.packedY()).writeInt(this.packedZ());
	}

	public static PackedPosition read(PacketByteBuf buffer) {
		return new PackedPosition(buffer.readInt(), buffer.readInt(), buffer.readInt());
	}
}