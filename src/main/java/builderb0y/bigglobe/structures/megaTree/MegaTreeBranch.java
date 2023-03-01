package builderb0y.bigglobe.structures.megaTree;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.megaTree.MegaTreeStructure.MegaTreeContext;
import builderb0y.bigglobe.util.Dvec3;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class MegaTreeBranch {

	public MegaTreeContext context;
	public int totalSteps;
	public int currentStep;
	public int stepsUntilNextSplit;
	public double startRadius;

	public Dvec3 velocity;
	public Dvec3 acceleration;
	public MegaTreeBall lastBall;

	public MegaTreeBranch(
		MegaTreeContext context,
		double x,
		double y,
		double z,
		double startRadius,
		int totalSteps,
		int stepsUntilNextSplit,
		Dvec3 velocity,
		Dvec3 acceleration
	) {
		this.context             = context;
		this.totalSteps          = totalSteps;
		this.currentStep         = 0;
		this.stepsUntilNextSplit = stepsUntilNextSplit;
		this.startRadius         = startRadius;
		this.velocity            = velocity;
		this.acceleration        = acceleration;
		this.lastBall            = new MegaTreeBall(
			BigGlobeStructures.MEGA_TREE_BALL_TYPE,
			x,
			y,
			z,
			startRadius,
			0,
			totalSteps,
			context.data.palette()
		);
	}

	public void generate() {
		MegaTreeContext context = this.context;
		context.addBall(this.lastBall);
		Dvec3 scratchPos = new Dvec3();
		Dvec3 shyness = new Dvec3();
		WorldColumn column = context.column;
		while (this.currentStep < this.totalSteps) {
			this.currentStep++;
			this.stepsUntilNextSplit--;
			double progress = ((double)(this.currentStep)) / ((double)(this.totalSteps));
			double currentRadius = Interpolator.mixLinear(this.startRadius, 0.5D, progress);

			Dvec3 position = this.lastBall.data.position();
			column.setPos(floorI(position.x), floorI(position.z));
			MegaTreeBall closestBall = context.octree.findClosestBall(this.lastBall);
			if (closestBall != null) {
				shyness
				.subtract(this.lastBall.data.position(), closestBall.data.position())
				.multiply(4.0D / squareD(Math.max(1.0D, shyness.length() - this.lastBall.data.radius() - closestBall.data.radius())));
			}
			else {
				shyness.set(0.0D);
			}
			scratchPos
				.setInSphere(context.permuter, 0.25D)
				.y(scratchPos.y + exp2((column.getFinalTopHeightD() - position.y) * 0.125D + 2.0D))
				.add(shyness)
				.add(this.acceleration)
				.multiply(0.125D / this.startRadius);
			Dvec3 prevVelocity = this.velocity;
			Dvec3 nextVelocity = new Dvec3().add(prevVelocity, scratchPos).normalize();
			this.acceleration = new Dvec3().subtract(nextVelocity, prevVelocity).normalize();
			this.velocity = nextVelocity;

			this.lastBall = new MegaTreeBall(BigGlobeStructures.MEGA_TREE_BALL_TYPE, this, position.add(nextVelocity), currentRadius);
			context.addBall(this.lastBall);

			if (this.stepsUntilNextSplit <= 0 && this.totalSteps - this.currentStep >= 4) {
				double sizeFactor = context.permuter.nextDouble() * 0.5D + 0.5D;
				scratchPos.setInSphere(context.permuter, 1.0D);
				Dvec3 cross = new Dvec3().cross(this.velocity, scratchPos).normalize();
				Dvec3 splitPosition = position.clone().add(cross);
				MegaTreeBranch split = new MegaTreeBranch(
					context,
					splitPosition.x,
					splitPosition.y,
					splitPosition.z,
					currentRadius * sizeFactor,
					Permuter.roundRandomlyI(context.permuter.nextLong(), (this.totalSteps - this.currentStep) * sizeFactor),
					Permuter.roundRandomlyI(context.permuter.nextLong(), currentRadius * 4.0D),
					nextVelocity,
					cross
				);
				context.addBranch(split);
				this.stepsUntilNextSplit = Permuter.roundRandomlyI(context.permuter.nextLong(), currentRadius * context.permuter.nextDouble(2.0D, 3.0D) + context.foliageFactor(context.data.branch_sparsity()));
			}
		}
	}

	@Override
	public String toString() {
		return "MegaTreeBranch@" + Integer.toHexString(System.identityHashCode(this)) + ": { step: " + this.currentStep + " / " + this.totalSteps + ", velocity: " + this.velocity + ", acceleration: " + this.acceleration + ", at: " + this.lastBall + " }";
	}
}