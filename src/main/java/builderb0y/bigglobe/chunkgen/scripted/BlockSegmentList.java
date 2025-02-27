package builderb0y.bigglobe.chunkgen.scripted;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

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
		if (state != null) this.addSegment(minY, maxY - 1 /* convert to inclusive */, state);
		else this.removeSegment(minY, maxY - 1);
	}

	public BlockSegmentList split() {
		return new BlockSegmentList(this.minY(), this.maxY());
	}

	public @Nullable BlockSegmentList split(int minY, int maxY) {
		minY = Math.max(this.minY(), minY);
		maxY = Math.min(this.maxY(), maxY);
		return maxY > minY ? new BlockSegmentList(minY, maxY) : null;
	}

	public @Nullable BlockSegmentList splitAtPlacedRange() {
		if (this.isEmpty()) return null;
		else return new BlockSegmentList(this.getLit(0).minY(), this.getLit(this.size() - 1).maxY());
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

	public int getTopOfSegment(int y) {
		return this.getTopOrBottomOfSegment(y, true, Integer.MAX_VALUE - 1) + 1;
	}

	public int getBottomOfSegment(int y) {
		return this.getTopOrBottomOfSegment(y, false, Integer.MIN_VALUE);
	}

	@Override
	public Segment<BlockState> newSegment(int minY, int maxY, BlockState value) {
		return new LitSegment(minY, maxY, value);
	}

	public LitSegment getLit(int index) {
		return (LitSegment)(this.get(index));
	}

	public void computeLightLevels() {
		byte lightLevel = 15;
		for (int index = this.size(); --index >= 0;) {
			LitSegment segment = this.getLit(index);
			segment.lightLevel = lightLevel;
			if (lightLevel > 0) lightLevel = (byte)(Math.max(lightLevel - segment.value.getOpacity(#if MC_VERSION < MC_1_21_2 EmptyBlockView.INSTANCE, BlockPos.ORIGIN #endif) * (segment.maxY() - segment.minY()), 0));
		}
	}

	public static class LitSegment extends Segment<BlockState> {

		public byte lightLevel = -1;

		public LitSegment(int minY, int maxY, BlockState value) {
			super(minY, maxY, value);
		}

		public int minY() {
			return this.minY;
		}

		public int maxY() {
			return this.maxY + 1; //convert to exclusive.
		}
	}
}