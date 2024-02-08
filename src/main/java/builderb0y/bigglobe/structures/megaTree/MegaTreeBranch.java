package builderb0y.bigglobe.structures.megaTree;

import org.joml.Vector3d;

import net.minecraft.world.Heightmap;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.megaTree.MegaTreeStructure.MegaTreeContext;
import builderb0y.bigglobe.util.Vectors;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class MegaTreeBranch {

	public MegaTreeContext context;
	public int totalSteps;
	public int currentStep;
	public int stepsUntilNextSplit;
	public double startRadius;

	public Vector3d velocity;
	public Vector3d acceleration;
	public MegaTreeBall lastBall;

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
		this.context             = context;
		this.totalSteps          = totalSteps;
		this.currentStep         = 0;
		this.stepsUntilNextSplit = stepsUntilNextSplit;
		this.startRadius         = startRadius;
		this.velocity            = velocity;
		this.acceleration        = acceleration;
		this.lastBall            = new MegaTreeBall(
			BigGlobeStructures.MEGA_TREE_BALL_TYPE,
			context.structure,
			x,
			y,
			z,
			startRadius,
			0,
			totalSteps
		);
	}

	public void generate() {
		MegaTreeContext context = this.context;
		context.addBall(this.lastBall);
		Vector3d scratchPos = new Vector3d();
		Vector3d shyness = new Vector3d();
		ScriptedColumn column = context.column;
		ColumnToDoubleScript.Holder surfaceYGetter = context.structure.data.surface_y();
		while (this.currentStep < this.totalSteps) {
			this.currentStep++;
			this.stepsUntilNextSplit--;
			double progress = ((double)(this.currentStep)) / ((double)(this.totalSteps));
			double currentRadius = Interpolator.mixLinear(this.startRadius, 0.5D, progress);

			Vector3d position = this.lastBall.data.position();
			MegaTreeBall closestBall = context.octree.findClosestBall(this.lastBall);
			if (closestBall != null) {
				shyness
				.set(this.lastBall.data.position())
				.sub(closestBall.data.position())
				.mul(4.0D / squareD(Math.max(1.0D, shyness.length() - this.lastBall.data.radius() - closestBall.data.radius())));
			}
			else {
				shyness.set(0.0D);
			}
			if (column != null) column.setPos(floorI(position.x), floorI(position.z));
			double surfaceY = column != null && surfaceYGetter != null ? surfaceYGetter.get(column) : context.structureContext.chunkGenerator().getHeightOnGround(floorI(position.x), floorI(position.z), Heightmap.Type.OCEAN_FLOOR_WG, context.structureContext.world(), context.structureContext.noiseConfig());
			Vectors.setInSphere(scratchPos, context.permuter, 0.25D)
			.add(0.0D, scratchPos.y + exp2((surfaceY - position.y) * 0.125D + 2.0D), 0.0D)
			.add(shyness)
			.add(this.acceleration)
			.mul(0.125D / this.startRadius);
			Vector3d prevVelocity = this.velocity;
			Vector3d nextVelocity = new Vector3d(prevVelocity).add(scratchPos).normalize();
			this.acceleration = new Vector3d(nextVelocity).sub(prevVelocity).normalize();
			this.velocity = nextVelocity;

			this.lastBall = new MegaTreeBall(BigGlobeStructures.MEGA_TREE_BALL_TYPE, this.context.structure, this, position.add(nextVelocity), currentRadius);
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
				this.stepsUntilNextSplit = Permuter.roundRandomlyI(context.permuter.nextLong(), currentRadius * context.permuter.nextDouble(2.0D, 3.0D) + context.foliageFactor(context.structure.data.branch_sparsity()));
			}
		}
	}

	@Override
	public String toString() {
		return "MegaTreeBranch@" + Integer.toHexString(System.identityHashCode(this)) + ": { step: " + this.currentStep + " / " + this.totalSteps + ", velocity: " + this.velocity + ", acceleration: " + this.acceleration + ", at: " + this.lastBall + " }";
	}
}