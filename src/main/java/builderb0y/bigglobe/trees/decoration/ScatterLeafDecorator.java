package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.util.Dvec3;

public class ScatterLeafDecorator extends ConfiguredLeafDecorator {

	public ScatterLeafDecorator(boolean is_trunk, @VerifyNullable RandomList<BlockState> leaf_states) {
		super(is_trunk, leaf_states);
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		if (this.is_trunk && generator.trunk.currentFracY < generator.branches.startFracY) return;
		double radius = generator.trunk.currentRadius * 0.5D + 2.0D;
		int blocks = Permuter.roundRandomlyI(generator.random, radius * radius * radius * 2.0D);
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		Dvec3 vec = new Dvec3();
		for (int block = 1; block <= blocks; block++) {
			vec.setOnSphere(generator.random, generator.random.nextDouble(1.0D, radius)).add(pos.getX(), pos.getY(), pos.getZ());
			this.placeAt(
				generator,
				mutablePos.set(vec.x, vec.y, vec.z),
				+ Math.abs(mutablePos.getX() - pos.getX())
				+ Math.abs(mutablePos.getY() - pos.getY())
				+ Math.abs(mutablePos.getZ() - pos.getZ())
			);
		}
	}
}