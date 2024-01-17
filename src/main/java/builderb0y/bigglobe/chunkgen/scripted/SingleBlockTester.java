package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

public class SingleBlockTester implements BlockSegmentConsumer<SingleBlockTester> {

	public final int y;
	public BlockState state;

	public SingleBlockTester(int y) {
		this.y = y;
	}

	@Override
	public int minY() {
		return this.y;
	}

	@Override
	public int maxY() {
		return this.y + 1;
	}

	@Override
	public void accept(int minY, int maxY, BlockState state) {
		if (this.y >= minY && this.y < maxY) {
			this.state = state;
		}
	}

	@Override
	public SingleBlockTester split(int minY, int maxY) {
		return new SingleBlockTester(this.y);
	}

	@Override
	public void mergeAndKeepEverywhere(SingleBlockTester that) {
		if (that.state != null) this.state = that.state;
	}

	@Override
	public void mergeAndKeepWhereThereAreBlocks(SingleBlockTester that) {
		if (this.state != null && that.state != null) this.state = that.state;
	}

	@Override
	public void mergeAndKeepWhereThereArentBlocks(SingleBlockTester that) {
		if (this.state == null && that.state != null) this.state = that.state;
	}

	@Override
	public void reset() {
		this.state = null;
	}
}