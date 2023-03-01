package builderb0y.bigglobe.trees.trunks;

import java.util.random.RandomGenerator;

import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;

public class TwistedTrunkConfig extends TrunkConfig {

	public final double waveAngleStart, waveAngleSpeed;

	public TwistedTrunkConfig(
		double startX,
		int startY,
		double startZ,
		int height,
		double startRadius,
		double waveAngleStart,
		double waveAngleSpeed,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		super(startX, startY, startZ, height, startRadius, requireValidGround, canGenerateInLiquid);
		this.waveAngleStart = waveAngleStart;
		this.waveAngleSpeed = waveAngleSpeed;
	}

	public static TwistedTrunkConfig create(
		BlockPos origin,
		int height,
		double startRadius,
		RandomGenerator random,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return new TwistedTrunkConfig(
			origin.getX() + random.nextDouble() - 0.5D,
			origin.getY(),
			origin.getZ() + random.nextDouble() - 0.5D,
			height,
			startRadius,
			random.nextDouble(BigGlobeMath.TAU),
			randomTwistSpeed(random),
			requireValidGround,
			canGenerateInLiquid
		);
	}

	public static double randomTwistSpeed(RandomGenerator random) {
		double speed = Interpolator.smooth(random.nextDouble()) * 16.0D - 8.0D;
		return random.nextBoolean() ? -speed : speed;
	}

	@Override
	public void setFrac(double fracY) {
		super.setFrac(fracY);
		double bulge = fracY * (1.0D - fracY);
		double angle = fracY * this.waveAngleSpeed + this.waveAngleStart;
		double amplitude = bulge * 3.0D * this.startRadius;
		this.currentX = this.startX + Math.cos(angle) * amplitude;
		this.currentZ = this.startZ + Math.sin(angle) * amplitude;
	}
}