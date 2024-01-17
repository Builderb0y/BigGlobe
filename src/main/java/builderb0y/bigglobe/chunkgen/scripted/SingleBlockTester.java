package builderb0y.bigglobe.chunkgen.scripted;

import java.util.function.Predicate;

import net.minecraft.block.BlockState;

public class SingleBlockTester implements BlockSegmentConsumer<SingleBlockTester> {

	public final int y;
	public final Predicate<BlockState> predicate;
	public boolean haveLayer, haveBlock;

	public SingleBlockTester(int y, Predicate<BlockState> predicate) {
		this.y = y;
		this.predicate = predicate;
	}

	@Override
	public void accept(int minY, int maxY, BlockState state) {
		if (this.y >= minY && this.y < maxY) {
			this.haveLayer = true;
			this.haveBlock = this.predicate.test(state);
		}
	}

	@Override
	public SingleBlockTester split(int minY, int maxY) {
		return new SingleBlockTester(this.y, this.predicate);
	}

	@Override
	public void mergeAndKeepWhereThereAreBlocks(SingleBlockTester that) {
		if (this.haveLayer) this.haveBlock = that.haveBlock;
	}

	@Override
	public void mergeAndKeepWhereThereArentBlocks(SingleBlockTester that) {
		if (!this.haveLayer) {
			this.haveLayer = that.haveLayer;
			this.haveBlock = that.haveBlock;
		}
	}

	@Override
	public void reset() {
		this.haveLayer = this.haveBlock = false;
	}
}