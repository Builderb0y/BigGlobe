package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.versions.AutoCodecVersions;

public class StalactiteGrid2D implements Grid2D {

	public final Seed salt;
	public final @UseVerifier(name = "checkNotZero", in = StalactiteGrid2D.class, usage = MemberUsage.METHOD_IS_HANDLER) double amplitude;
	public final @VerifyIntRange(min = 0, minInclusive = false) int scale;
	public final transient double rcp;

	public StalactiteGrid2D(Seed salt, double amplitude, int scale) {
		this.salt = salt;
		this.amplitude = amplitude;
		this.rcp = 1.0D / (this.scale = scale);
	}

	public static <T_Encoded> void checkNotZero(VerifyContext<T_Encoded, Double> context) throws VerifyException {
		if (context.object != null && context.object.doubleValue() == 0.0D) {
			throw AutoCodecVersions.newVerifyException(() -> context.pathToStringBuilder().append(" cannot be 0.").toString());
		}
	}

	@Override
	public double minValue() {
		return Math.min(this.amplitude, 0.0D);
	}

	@Override
	public double maxValue() {
		return Math.max(this.amplitude, 0.0D);
	}

	public double getCenterX(long seed, int relativeX, int relativeY) {
		return Permuter.toPositiveDouble(Permuter.permute(seed ^ 0x830C14AA6E17AA46L, relativeX, relativeY));
	}

	public double getCenterY(long seed, int relativeX, int relativeY) {
		return Permuter.toPositiveDouble(Permuter.permute(seed ^ 0x8B1B250A939D67EFL, relativeX, relativeY));
	}

	public double getSize(long seed, int relativeX, int relativeY) {
		return Permuter.toPositiveDouble(Permuter.permute(seed ^ 0x8DE478CEFA3C739AL, relativeX, relativeY));
	}

	@Override
	public double getValue(long seed, int x, int y) {
		seed ^= this.salt.value;
		int relativeX = Math.floorDiv(x, this.scale);
		int relativeY = Math.floorDiv(y, this.scale);
		double fracX = BigGlobeMath.modulus_BP(x, this.scale) * this.rcp;
		double fracY = BigGlobeMath.modulus_BP(y, this.scale) * this.rcp;
		double totalHeight = 0.0D;
		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetY = -1; offsetY <= 1; offsetY++) {
				int newRelativeX = relativeX + offsetX;
				int newRelativeY = relativeY + offsetY;
				double centerX = this.getCenterX(seed, newRelativeX, newRelativeY) + offsetX;
				double centerY = this.getCenterY(seed, newRelativeX, newRelativeY) + offsetY;
				double size    = this.getSize   (seed, newRelativeX, newRelativeY);
				size = BigGlobeMath.exp2(-4.0D * size);
				double sizeSquared = size * size;
				double distanceSquared = BigGlobeMath.squareD(centerX - fracX, centerY - fracY);
				if (distanceSquared < sizeSquared) {
					distanceSquared /= sizeSquared;
					double distance = Math.sqrt(distanceSquared);
					double height = BigGlobeMath.squareD(1.0D - distance);
					totalHeight += height * size * this.amplitude;
				}
			}
		}
		return totalHeight;
	}
}