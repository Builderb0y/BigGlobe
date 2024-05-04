package builderb0y.bigglobe.trees.trunks;

import java.util.random.RandomGenerator;

import net.minecraft.util.math.BlockPos;

public class StraightTrunkConfig extends TrunkConfig {

	public StraightTrunkConfig(
		double startX, int startY, double startZ,
		int height,
		TrunkThicknessScript thicknessScript,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		super(startX, startY, startZ, height, thicknessScript, requireValidGround, canGenerateInLiquid);
		this.currentX = startX;
		this.currentZ = startZ;
	}

	public static StraightTrunkConfig create(
		BlockPos origin,
		int height,
		RandomGenerator random,
		TrunkThicknessScript thicknessScript,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		return new StraightTrunkConfig(
			origin.getX() + random.nextDouble() - 0.5D,
			origin.getY(),
			origin.getZ() + random.nextDouble() - 0.5D,
			height,
			thicknessScript,
			requireValidGround,
			canGenerateInLiquid
		);
	}

	//don't need to override updateFrac() because our currentX and currentZ values will never change.
}