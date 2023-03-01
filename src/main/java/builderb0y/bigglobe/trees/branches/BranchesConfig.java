package builderb0y.bigglobe.trees.branches;

import builderb0y.bigglobe.trees.TreeGenerator;

public class BranchesConfig {

	/**
	fractional height at which branches will start spawning.
	0 represents the bottom of the tree, and 1 represents the top.
	*/
	public final double startFracY;
	/** number of branches which will spawn. */
	public final int branchCount;
	/** angle (in radians) of the first (lowest) branch. */
	public final double startAngle;
	/**
	if true, the branch radius will be used.
	if false, all branches will be one block thick.
	*/
	public final boolean thickBranches;
	/**
	the distribution function for how long branches will be.
	the input is how high the branch is.
	0 as an input represents startY, and 1 represents the top of the tree.
	*/
	public final ScriptedBranchShape.Holder lengthGetter;
	/**
	distribution function for what number to add to the branch's Y level when placing the log blocks.
	the input of this function is how far along the branch we are.
	0 represents the center of the trunk, and 1 represents the very tip of the branch.
	the output is multiplied by lengthGetter's output, and then by heightMultiplierGetter's output.
	*/
	public final ScriptedBranchShape.Holder heightGetter;

	public BranchConfig currentBranch;

	public BranchesConfig(
		double                     startFracY,
		int                        branchCount,
		double                     startAngle,
		boolean                    thickBranches,
		ScriptedBranchShape.Holder lengthGetter,
		ScriptedBranchShape.Holder heightGetter
	) {
		this.startFracY    = startFracY;
		this.branchCount   = branchCount;
		this.startAngle    = startAngle;
		this.thickBranches = thickBranches;
		this.lengthGetter  = lengthGetter;
		this.heightGetter  = heightGetter;
	}

	public static BranchesConfig create(
		double                     startFracY,
		int                        branchCount,
		double                     startAngle,
		double                     trunkStartRadius,
		ScriptedBranchShape.Holder lengthGetter,
		ScriptedBranchShape.Holder heightGetter
	) {
		return new BranchesConfig(
			startFracY,
			branchCount,
			startAngle,
			trunkStartRadius >= ThickBranchConfig.MIN_RADIUS * 3.0D,
			lengthGetter,
			heightGetter
		);
	}

	public void updateBranch(TreeGenerator generator, int index) {
		double angle = this.startAngle;
		double angleIncrement = Math.PI;
		for (int bits = index; true;) {
			if ((bits & 0b1) != 0) angle += angleIncrement;
			if ((bits >>>= 1) == 0) break;
			angleIncrement *= 0.5D;
		}
		double length = (
			this.lengthGetter.evaluate(
				((double)(index)) / ((double)(this.branchCount)),
				generator.centerColumn,
				generator.trunk.currentY,
				generator.random
			)
			* generator.trunk.height
			* (1.0D - this.startFracY)
		);
		long seed = generator.random.nextLong();
		this.currentBranch = (
			this.thickBranches
			? new ThickBranchConfig(angle, length, seed)
			: new      BranchConfig(angle, length, seed)
		);
	}
}