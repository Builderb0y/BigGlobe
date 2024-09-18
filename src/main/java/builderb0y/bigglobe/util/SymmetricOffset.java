package builderb0y.bigglobe.util;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

/**
note: the rotations performed by this class are intentionally backwards,
because minecraft itself has coordinates reversed for the x and z axes.
*/
public record SymmetricOffset(
	@UseName("x") int offsetX,
	@UseName("y") int offsetY,
	@UseName("z") int offsetZ,

	@UseName("r" /* named r for backwards compatibility */)
	@UseCoder(name = "RAW_SYMMETRY_CODER", in = SymmetricOffset.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
	Symmetry symmetry
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
	public static final AutoCoder<Symmetry> RAW_SYMMETRY_CODER = Testing.enabled ? null : new NamedCoder<>("SymmetricOffset.RAW_SYMMETRY_CODER") {

		@Override
		@OverrideOnly
		public <T_Encoded> @Nullable Symmetry decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			return Symmetry.VALUES[context.forceAsNumber().intValue()];
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, Symmetry> context) throws EncodeException {
			if (context.object == null) return context.empty();
			return context.createByte((byte)(context.object.ordinal()));
		}
	};

	@UnknownNullability
	public static final AutoCoder<SymmetricOffset> CODER = Testing.enabled ? null : BigGlobeAutoCodec.AUTO_CODEC.createCoder(SymmetricOffset.class);

	public static final SymmetricOffset IDENTITY = new SymmetricOffset(0, 0, 0, Symmetry.IDENTITY);

	public static SymmetricOffset fromCenter(int centerX, int centerZ, Symmetry symmetry) {
		return IDENTITY.rotateAround(centerX, centerZ, symmetry);
	}

	public SymmetricOffset rotate(Symmetry symmetry) {
		return new SymmetricOffset(
			symmetry.getX(this.offsetX, this.offsetZ),
			this.offsetY,
			symmetry.getZ(this.offsetX, this.offsetZ),
			symmetry.compose(this.symmetry)
		);
	}

	public SymmetricOffset offset(int deltaX, int deltaY, int deltaZ) {
		return deltaX == 0 && deltaY == 0 && deltaZ == 0 ? this : new SymmetricOffset(this.offsetX + deltaX, this.offsetY + deltaY, this.offsetZ + deltaZ, this.symmetry);
	}

	public SymmetricOffset rotateAround(int x, int z, Symmetry symmetry) {
		return this.offset(-x, 0, -z).rotate(symmetry).offset(x, 0, z);
	}

	public int getX(int x, int y, int z) {
		return this.offsetX + this.symmetry.getX(x, z);
	}

	public int getY(int x, int y, int z) {
		return this.offsetY + y;
	}

	public int getZ(int x, int y, int z) {
		return this.offsetZ + this.symmetry.getZ(x, z);
	}

	public double getX(double x, double y, double z) {
		return this.offsetX + this.symmetry.getX(x, z);
	}

	public double getY(double x, double y, double z) {
		return this.offsetY + y;
	}

	public double getZ(double x, double y, double z) {
		return this.offsetZ + this.symmetry.getZ(x, z);
	}
}