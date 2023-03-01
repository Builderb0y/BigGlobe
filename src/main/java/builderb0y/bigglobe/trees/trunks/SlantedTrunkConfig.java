package builderb0y.bigglobe.trees.trunks;

import java.util.random.RandomGenerator;

import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.math.BigGlobeMath;

import static builderb0y.bigglobe.trees.TreeSpecialCases.*;

public class SlantedTrunkConfig extends TrunkConfig {

	public final double dx, dz;

	public SlantedTrunkConfig(
		double startX,
		int startY,
		double startZ,
		int height,
		double startRadius,
		double dx,
		double dz,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		super(startX, startY, startZ, height, startRadius, requireValidGround, canGenerateInLiquid);
		this.dx = dx;
		this.dz = dz;
	}

	public static SlantedTrunkConfig create(
		double originX,
		int originY,
		double originZ,
		int height,
		double startRadius,
		double slantAngle,
		double slantAmount,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return new SlantedTrunkConfig(
			originX,
			originY,
			originZ,
			height,
			startRadius,
			Math.cos(slantAngle) * slantAmount,
			Math.sin(slantAngle) * slantAmount,
			requireValidGround,
			canGenerateInLiquid
		);
	}

	public static SlantedTrunkConfig createNatural(
		BlockPos origin,
		int height,
		double startRadius,
		RandomGenerator random,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return create(
			origin.getX() + random.nextDouble() - 0.5D,
			origin.getY(),
			origin.getZ() + random.nextDouble() - 0.5D,
			height,
			startRadius,
			random.nextDouble(BigGlobeMath.TAU),
			naturalCharredSlantAmount(random) * height,
			requireValidGround,
			canGenerateInLiquid
		);
	}

	public static SlantedTrunkConfig createArtificial(
		double originX,
		int originY,
		double originZ,
		int height,
		double startRadius,
		RandomGenerator random,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return create(
			originX,
			originY,
			originZ,
			height,
			startRadius,
			random.nextDouble(BigGlobeMath.TAU),
			artificialCharredSlantAmount(random) * height,
			requireValidGround,
			canGenerateInLiquid
		);
	}

	@Override
	public void setFrac(double fracY) {
		super.setFrac(fracY);
		this.currentX = this.startX + this.dx * fracY;
		this.currentZ = this.startZ + this.dz * fracY;
	}
}