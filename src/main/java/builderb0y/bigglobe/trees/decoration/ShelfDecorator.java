package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList.KnownTotalWeightRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;
import builderb0y.bigglobe.versions.MaterialVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class ShelfDecorator implements TrunkLayerDecorator {

	public static final ShelfDecorator EMPTY = new ShelfDecorator(new RandomList<>(0), 0.0D, 0.0D);

	public final KnownTotalWeightRandomList<ShelfPlacer> placers;
	public final double maxHeightFrac;
	public final double density;

	public double probability;

	public ShelfDecorator(KnownTotalWeightRandomList<ShelfPlacer> placers, double maxHeightFrac, double density) {
		if (placers.contains(null)) throw Util.throwOrPause(new IllegalArgumentException("placers contains null"));
		this.placers = placers;
		this.maxHeightFrac = maxHeightFrac;
		this.density = density;
	}

	@Override
	public void decorate(TreeGenerator generator, TrunkConfig trunk, int y) {
		if (this.placers.isEmptyOrWeightless()) return;
		if (generator.trunk.currentFracY >= this.maxHeightFrac) return;
		double probability = this.probability;
		if (Permuter.nextChancedBoolean(generator.random, probability / (probability + 1.0D))) {
			ShelfPlacer placer = this.placers.getRandomElement(generator.random);
			if (placer == null) throw Util.throwOrPause(new AssertionError("placer is null"));
			double angle = generator.random.nextDouble(TAU);
			double distanceFromCenter = generator.trunk.currentRadius + 1.0D;
			double centerX = Math.cos(angle) * distanceFromCenter + generator.trunk.currentX;
			double centerZ = Math.sin(angle) * distanceFromCenter + generator.trunk.currentZ;
			double shelfRadius = distanceFromCenter * (generator.random.nextDouble() * 0.5D + 0.5D);
			double shelfRadius2 = shelfRadius * shelfRadius;
			int minX = ceilI(centerX - shelfRadius), maxX = floorI(centerX + shelfRadius);
			int minZ = ceilI(centerZ - shelfRadius), maxZ = floorI(centerZ + shelfRadius);
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();
			BlockPos.Mutable  offsetPos = new BlockPos.Mutable();
			mutablePos.setY(y);
			for (int x = minX; x <= maxX; x++) {
				mutablePos.setX(x);
				for (int z = minZ; z <= maxZ; z++) {
					if (squareD(x - centerX, z - centerZ) < shelfRadius2) {
						mutablePos.setZ(z);
						if (canShelfReplace(generator.worldQueue.getBlockState(mutablePos))) {
							placer.queueBlockAt(generator, mutablePos, offsetPos);
						}
					}
				}
			}
			this.probability = 0.0D;
		}
		else {
			this.probability = probability + this.density;
		}
	}

	public static boolean canShelfReplace(BlockState state) {
		if (state.getFluidState().isStill()) return false;
		return MaterialVersions.isReplaceableOrPlant(state) || state.isIn(BlockTags.LEAVES);
	}
}