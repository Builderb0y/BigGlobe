package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

public class BlockSegmentList extends SegmentList<BlockState> {

	public BlockSegmentList(int minY, int maxY) {
		super(minY, maxY - 1 /* convert to inclusive */);
	}

	public int minY() {
		return this.minY;
	}

	public int maxY() {
		return this.maxY + 1 /* convert to exclusive */;
	}

	public BlockState getBlockState(int y) {
		return this.getOverlappingObject(y);
	}

	public void setBlockState(int y, BlockState state) {
		this.setBlockStates(y, y + 1, state);
	}

	public void setBlockStates(int minY, int maxY, BlockState state) {
		this.addSegment(minY, maxY - 1 /* convert to inclusive */, state);
	}

	public BlockSegmentList split(int minY, int maxY) {
		return new BlockSegmentList(Math.max(this.minY, minY), Math.min(this.maxY, maxY - 1 /* convert to inclusive */) + 1 /* back to exclusive */);
	}

	public void mergeAndKeepEverywhere(BlockSegmentList that) {
		this.addAllSegments(that);
	}

	public void mergeAndKeepWhereThereAreBlocks(BlockSegmentList that) {
		that.retainFrom(this);
		this.addAllSegments(that);
	}

	public void mergeAndKeepWhereThereArentBlocks(BlockSegmentList that) {
		that.removeFrom(this);
		this.addAllSegments(that);
	}

	public void reset() {
		this.clear();
	}
}