package builderb0y.bigglobe.trees.trunks;

import builderb0y.bigglobe.math.Interpolator;

public abstract class TrunkConfig {

	public static final double MIN_RADIUS = 0.7071067811865476D; //sqrt(0.5 ^ 2 + 0.5 ^ 2)

	public final int startY, height;
	public final double startX, startZ, startRadius;
	/**
	if true, the tree will abort generation if it
	attempts to place a trunk block above a block which
	isn't in TreeSpecialCases.getGroundReplacements().
	this flag is set to true for natural trees,
	and false for trees grown from saplings.
	*/
	public boolean requireValidGround;
	/** true for mangrove trees. */
	public boolean canGenerateInLiquid;
	public double currentFracY, currentRadius, currentX, currentY, currentZ;

	public TrunkConfig(
		double startX,
		int startY,
		double startZ,
		int height,
		double startRadius,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		this.startX = startX;
		this.startY = startY;
		this.startZ = startZ;
		this.height = height;
		this.startRadius = startRadius;
		this.requireValidGround = requireValidGround;
		this.canGenerateInLiquid = canGenerateInLiquid;
	}

	public void setOffset(int offsetY) {
		this.setFrac(((double)(offsetY)) / ((double)(this.height)));
	}

	public void setFrac(double fracY) {
		this.currentFracY = fracY;
		this.currentY = this.startY + this.height * fracY;
		this.currentRadius = Interpolator.mixLinear(this.startRadius, MIN_RADIUS, fracY);
	}
}