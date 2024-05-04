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
		double waveAngleStart,
		double waveAngleSpeed,
		TrunkThicknessScript thicknessScript,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		super(startX, startY, startZ, height, thicknessScript, requireValidGround, canGenerateInLiquid);
		this.waveAngleStart = waveAngleStart;
		this.waveAngleSpeed = waveAngleSpeed;
	}

	public static TwistedTrunkConfig create(
		BlockPos origin,
		int height,
		double startSize,
		RandomGenerator random,
		TrunkThicknessScript thicknessScript,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return new TwistedTrunkConfig(
			origin.getX() + random.nextDouble() - 0.5D,
			origin.getY(),
			origin.getZ() + random.nextDouble() - 0.5D,
			height,
			random.nextDouble(BigGlobeMath.TAU),
			randomTwistSpeed(random),
			thicknessScript,
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
		double amplitude = bulge * this.baseRadius * 3.0D;
		this.currentX = this.startX + Math.cos(angle) * amplitude;
		this.currentZ = this.startZ + Math.sin(angle) * amplitude;
	}
}