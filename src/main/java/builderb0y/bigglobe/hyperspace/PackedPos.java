package builderb0y.bigglobe.hyperspace;

import org.joml.Vector3dc;
import builderb0y.bigglobe.math.BigGlobeMath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

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

	public PackedPos(Vec3 vector) {
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

	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(this.packedX()).writeInt(this.packedY()).writeInt(this.packedZ());
	}

	public static PackedPos read(FriendlyByteBuf buffer) {
		return new PackedPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
	}
}