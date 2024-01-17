package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

public class BlockSegmentList extends SegmentList<BlockState> implements BlockSegmentConsumer<BlockSegmentList> {

	public BlockSegmentList(int minY, int maxY) {
		super(minY, maxY);
	}

	@Override
	public int minY() {
		return this.minY;
	}

	@Override
	public int maxY() {
		return this.maxY + 1;
	}

	@Override
	public void accept(int minY, int maxY, BlockState state) {
		this.addSegment(minY, maxY - 1 /* convert to inclusive */, state);
	}

	@Override
	public BlockSegmentList split(int minY, int maxY) {
		return new BlockSegmentList(Math.max(this.minY, minY), Math.min(this.maxY, maxY - 1 /* convert to inclusive */));
	}

	@Override
	public void mergeAndKeepEverywhere(BlockSegmentList that) {
		this.addAllSegments(that);
	}

	@Override
	public void mergeAndKeepWhereThereAreBlocks(BlockSegmentList that) {
		that.retainFrom(this);
		this.addAllSegments(that);
	}

	@Override
	public void mergeAndKeepWhereThereArentBlocks(BlockSegmentList that) {
		that.removeFrom(this);
		this.addAllSegments(that);
	}

	@Override
	public void reset() {
		this.clear();
	}
}