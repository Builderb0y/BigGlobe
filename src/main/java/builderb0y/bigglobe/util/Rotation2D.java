package builderb0y.bigglobe.util;

import org.jetbrains.annotations.UnknownNullability;

import net.minecraft.util.BlockRotation;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.AutoHandler.HandlerMapper;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

/**
note: the rotations performed by this class are intentionally backwards,
because minecraft itself has coordinates reversed for the x and z axes.
*/
public record Rotation2D(
	@UseName("x") int offsetX,
	@UseName("z") int offsetZ,
	@UseName("r") @UseCoder(name = "RAW_ROTATION_CODER", in = Rotation2D.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) BlockRotation rotation
) {

	public static class Testing {

		/**
		enabled by JUnit, disables creation of AutoCoder's in class initializer.
		this is important because attempting to create an AutoCoder requires the
		game to be bootstrapped, and it won't be if we're calling this from JUnit.
		*/
		public static boolean enabled = false;
	}

	@UnknownNullability
	public static final AutoCoder<BlockRotation> RAW_ROTATION_CODER = Testing.enabled ? null : (
		BigGlobeAutoCodec
		.AUTO_CODEC
		.createCoder(byte.class)
		.mapCoder(
			ReifiedType.from(BlockRotation.class),
			HandlerMapper.nullSafe(rotation -> (byte)(rotation.ordinal())),
			HandlerMapper.nullSafe((Byte ordinal) -> Directions.ROTATIONS[ordinal])
		)
	);

	@UnknownNullability
	public static final AutoCoder<Rotation2D> CODER = Testing.enabled ? null : BigGlobeAutoCodec.AUTO_CODEC.createCoder(Rotation2D.class);

	public static final Rotation2D IDENTITY = new Rotation2D(0, 0, BlockRotation.NONE);

	public static Rotation2D fromCenter(int centerX, int centerZ, BlockRotation rotation) {
		return IDENTITY.rotateAround(centerX, centerZ, rotation);
	}

	public Rotation2D rotate(BlockRotation rotation) {
		return switch (rotation) {
			case NONE -> this;
			case COUNTERCLOCKWISE_90 -> new Rotation2D(this.offsetZ, -this.offsetX, this.rotation.rotate(rotation));
			case CLOCKWISE_180 -> new Rotation2D(-this.offsetX, -this.offsetZ, this.rotation.rotate(rotation));
			case CLOCKWISE_90 -> new Rotation2D(-this.offsetZ, this.offsetX, this.rotation.rotate(rotation));
		};
	}

	public Rotation2D offset(int deltaX, int deltaZ) {
		return deltaX == 0 && deltaZ == 0 ? this : new Rotation2D(this.offsetX + deltaX, this.offsetZ + deltaZ, this.rotation);
	}

	public Rotation2D rotateAround(int x, int z, BlockRotation rotation) {
		return this.offset(-x, -z).rotate(rotation).offset(x, z);
	}

	public int getX(int x, int z) {
		return this.offsetX + switch (this.rotation) {
			case NONE -> x;
			case COUNTERCLOCKWISE_90 -> z;
			case CLOCKWISE_180 -> -x;
			case CLOCKWISE_90 -> -z;
		};
	}

	public int getZ(int x, int z) {
		return this.offsetZ + switch (this.rotation) {
			case NONE -> z;
			case COUNTERCLOCKWISE_90 -> -x;
			case CLOCKWISE_180 -> -z;
			case CLOCKWISE_90 -> x;
		};
	}

	public double getX(double x, double z) {
		return this.offsetX + switch (this.rotation) {
			case NONE -> x;
			case COUNTERCLOCKWISE_90 -> z;
			case CLOCKWISE_180 -> -x;
			case CLOCKWISE_90 -> -z;
		};
	}

	public double getZ(double x, double z) {
		return this.offsetZ + switch (this.rotation) {
			case NONE -> z;
			case COUNTERCLOCKWISE_90 -> -x;
			case CLOCKWISE_180 -> -z;
			case CLOCKWISE_90 -> x;
		};
	}
}