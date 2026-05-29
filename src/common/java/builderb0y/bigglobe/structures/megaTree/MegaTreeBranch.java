package builderb0y.bigglobe.structures.megaTree;

import org.joml.Vector3d;

import net.minecraft.world.level.levelgen.Heightmap;

import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript.Catcher;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.megaTree.MegaTreeStructure.MegaTreeContext;
import builderb0y.bigglobe.util.Vectors;

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;
import static builderb0y.bigglobe.math.BigGlobeMath.squareD;

public class MegaTreeBranch {

	public MegaTreeContext context;
	public int totalSteps;
	public int currentStep;
	public int stepsUntilNextSplit;
	public double startRadius;

	public Vector3d velocity;
	public Vector3d acceleration;
	public Ball lastBall;

	public MegaTreeBranch(
		MegaTreeContext context,
		double x,
		double y,
		double z,
		double startRadius,
		int totalSteps,
		int stepsUntilNextSplit,
		Vector3d velocity,
		Vector3d acceleration
	) {
		this.context = context;
		this.totalSteps = totalSteps;
		this.currentStep = 0;
		this.stepsUntilNextSplit = stepsUntilNextSplit;
		this.startRadius = startRadius;
		this.velocity = velocity;
		this.acceleration = acceleration;
		this.lastBall = new Ball(x, y, z, startRadius);
	}

	public void generate() {
		MegaTreeContext context = this.context;
		context.addBall(this.lastBall);
		double size = context.size;
		Vector3d scratchPos = new Vector3d();
		Vector3d shyness = new Vector3d();
		ScriptedColumn column = context.column;
		Catcher surfaceYGetter = context.structure.surface_y;
		while (this.currentStep < this.totalSteps) {
			this.currentStep++;
			this.stepsUntilNextSplit--;
			double progress = ((double)(this.currentStep)) / ((double)(this.totalSteps));
			double currentRadius = Interpolator.mixLinear(this.startRadius, 0.5D, progress);

			Vector3d position = this.lastBall.position();
			Ball closestBall = context.octree.findClosestBall(this.lastBall);
			if (closestBall != null) {
				shyness
					.set(this.lastBall.position())
					.sub(closestBall.position())
					.mul(4.0D / squareD(Math.max(1.0D, shyness.length() - this.lastBall.radius() - closestBall.radius())));
			}
			else {
				shyness.set(0.0D);
			}
			column.setParams(column.params.at(floorI(position.x), floorI(position.z)));
			int surfaceY = (
				surfaceYGetter != null
					? surfaceYGetter.get(column)
					: context.structureContext.chunkGenerator().getFirstFreeHeight(
					floorI(position.x),
					floorI(position.z),
					Heightmap.Types.OCEAN_FLOOR_WG,
					context.structureContext.heightAccessor(),
					context.structureContext.randomState()
				)
			);
			Vectors.setInSphere(scratchPos, context.permuter, 0.25D);
			scratchPos.y += FastMath.Exp.fastExp2((surfaceY - position.y) * 0.125D + size * 0.015625D);
			scratchPos.add(shyness)
				.add(this.acceleration)
				.mul(0.125D / this.startRadius);
			Vector3d prevVelocity = this.velocity;
			Vector3d nextVelocity = new Vector3d(prevVelocity).add(scratchPos).normalize();
			this.acceleration = new Vector3d(nextVelocity).sub(prevVelocity).normalize();
			this.velocity = nextVelocity;
			position.add(nextVelocity);

			this.lastBall = new Ball(
				position.x,
				position.y,
				position.z,
				currentRadius
			);
			context.addBall(this.lastBall);

			if (this.stepsUntilNextSplit <= 0 && this.totalSteps - this.currentStep >= 4) {
				double sizeFactor = context.permuter.nextDouble() * 0.5D + 0.5D;
				Vectors.setInSphere(scratchPos, context.permuter, 1.0D);
				Vector3d cross = new Vector3d(this.velocity).cross(scratchPos).normalize();
				Vector3d splitPosition = new Vector3d(position).add(cross);
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
				this.stepsUntilNextSplit = Permuter.roundRandomlyI(context.permuter.nextLong(), currentRadius * context.permuter.nextDouble(2.0D, 3.0D) + context.branchSparsity);
			}
		}
	}

	@Override
	public String toString() {
		return "MegaTreeBranch@" + Integer.toHexString(System.identityHashCode(this)) + ": { step: " + this.currentStep + " / " + this.totalSteps + ", velocity: " + this.velocity + ", acceleration: " + this.acceleration + ", at: " + this.lastBall + " }";
	}
}