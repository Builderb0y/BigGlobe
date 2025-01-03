package builderb0y.bigglobe.trees.trunks;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.trees.TreeGenerator;

public abstract class TrunkConfig {

	public final int startY, height;
	public final double startX, startZ, baseRadius, minThickness;
	public final TrunkThicknessScript thicknessScript;
	/**
	if true, the tree will abort generation if it
	attempts to place a trunk block above a block which
	isn't in {@link TreeGenerator#groundReplacements}.
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
		TrunkThicknessScript thicknessScript,
		boolean requireValidGround,
		boolean canGenerateInLiquid
	) {
		this.startX = startX;
		this.startY = startY;
		this.startZ = startZ;
		this.height = height;
		this.thicknessScript = thicknessScript;
		this.requireValidGround = requireValidGround;
		this.canGenerateInLiquid = canGenerateInLiquid;
		this.minThickness = getMinThickness(startX, startZ);
		this.baseRadius = thicknessScript.getThickness(height, 0.0D, this.minThickness);
	}

	public static double getMinThickness(double x, double z) {
		double closestX = Math.floor(x + 0.5D), closestZ = Math.floor(z + 0.5D);
		return Math.sqrt(BigGlobeMath.squareD(x - closestX) + BigGlobeMath.squareD(z - closestZ)) + 0.001D;
	}

	public void setOffset(int offsetY) {
		this.setFrac(((double)(offsetY)) / ((double)(this.height)));
	}

	public void setFrac(double fracY) {
		this.currentFracY = fracY;
		this.currentY = this.startY + this.height * fracY;
		this.currentRadius = this.thicknessScript.getThickness(this.height, fracY, this.minThickness);
	}
}