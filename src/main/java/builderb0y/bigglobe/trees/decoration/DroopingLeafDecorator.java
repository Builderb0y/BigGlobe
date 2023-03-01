package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.trees.TreeGenerator;

public class DroopingLeafDecorator extends ScatterLeafDecorator {

	public DroopingLeafDecorator(boolean is_trunk, @VerifyNullable RandomList<BlockState> leaf_states) {
		super(is_trunk, leaf_states);
	}

	@Override
	public boolean placeAt(TreeGenerator generator, BlockPos.Mutable pos, int distance) {
		int topY = pos.getY();
		for (
			int bits = generator.random.nextInt() | 1;
			(bits & 1) != 0 && super.placeAt(generator, pos, distance);
			bits >>>= 1, pos.setY(pos.getY() - 1), distance++
		) {}
		pos.setY(topY);
		return true;
	}
}