package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.branches.BranchConfig;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class BallLeafDecorator implements BranchDecorator, TrunkDecorator {

	public final BlockState innerState;

	public BallLeafDecorator(BlockState innerState) {
		this.innerState = innerState;
	}

	@Override
	public void decorate(TreeGenerator generator, TrunkConfig trunk) {
		trunk.setFrac(1.0D);
		this.decorate(generator, trunk.currentX, trunk.currentY, trunk.currentZ);
	}

	@Override
	public void decorate(TreeGenerator generator, BranchConfig branch) {
		branch.setFracLength(generator, 1.0D);
		this.decorate(generator, branch.currentX, branch.currentY, branch.currentZ);
	}

	public void decorate(TreeGenerator generator, double centerX, double centerY, double centerZ) {
		double radius = generator.random.nextDouble() * generator.trunk.currentRadius + 1.5D;
		double radius2 = radius * radius;
		double lightRadius = Math.max(radius - 1.0D, 0.0D);
		double lightRadius2 = lightRadius * lightRadius;
		int minX = floorI(centerX - radius) + 1;
		int minY = floorI(centerY - radius) + 1;
		int minZ = floorI(centerZ - radius) + 1;
		int maxX =  ceilI(centerX + radius) - 1;
		int maxY =  ceilI(centerY + radius) - 1;
		int maxZ =  ceilI(centerZ + radius) - 1;
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		BlockState leafState = generator.palette.leavesState(generator.random, 1, false, false);
		for (int x = minX; x <= maxX; x++) {
			mutablePos.setX(x);
			double offsetX2 = squareD(x - centerX);
			for (int z = minZ; z <= maxZ; z++) {
				double offsetXZ2 = offsetX2 + squareD(z - centerZ);
				if (offsetXZ2 >= radius2) continue;
				mutablePos.setZ(z);
				boolean placed = false;
				for (int y = maxY; y >= minY; y--) {
					double offsetXYZ2 = offsetXZ2 + squareD(y - centerY);
					if (offsetXYZ2 >= radius2) continue;
					mutablePos.setY(y);
					//noinspection AssignmentUsedAsCondition
					if (placed = generator.canLeavesReplace(generator.worldQueue.getBlockState(mutablePos))) {
						BlockState toPlace = offsetXYZ2 < lightRadius2 ? this.innerState : leafState;
						generator.queueAndDecorateLeaf(mutablePos, toPlace);
					}
				}
				if (placed) while (generator.random.nextBoolean()) {
					mutablePos.setY(mutablePos.getY() - 1);
					if (generator.worldQueue.isOutOfHeightLimit(mutablePos)) break;
					if (generator.canLeavesReplace(generator.worldQueue.getBlockState(mutablePos))) {
						generator.queueAndDecorateLeaf(mutablePos, leafState);
					}
					else {
						break;
					}
				}
			}
		}
	}
}