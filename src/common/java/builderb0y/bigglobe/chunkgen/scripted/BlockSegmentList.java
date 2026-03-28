package builderb0y.bigglobe.chunkgen.scripted;

import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.versions.BlockStateVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSegmentList extends AbstractObjectSegmentList<BlockState, BlockSegmentList.LitSegment> {

	public BlockSegmentList(int minY, int maxY) {
		super(minY, maxY - 1 /* convert to inclusive */);
	}

	public int minY() {
		return this.minY;
	}

	public int maxY() {
		return this.maxY + 1 /* convert to exclusive */;
	}

	public @Nullable BlockState getBlockState(int y) {
		return this.getOverlappingObject(y);
	}

	public void setBlockState(int y, BlockState state) {
		if (state != null) this.setBlockStates(y, y + 1, state);
		else this.removeSegment(y, y + 1);
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
		else return new BlockSegmentList(this.get(0).minY(), this.get(this.size() - 1).maxY());
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
	public LitSegment addSegment(LitSegment segment) {
		LitSegment result = super.addSegment(segment);
		if (result != null) result.skylightLevel = segment.skylightLevel;
		return result;
	}

	@Override
	public LitSegment newSegment(int minY, int maxY) {
		return new LitSegment(minY, maxY);
	}

	public void computeLightLevels(byte topLightLevel) {
		byte lightLevel = topLightLevel;
		for (int index = this.size(); --index >= 0; ) {
			LitSegment segment = this.get(index);
			segment.skylightLevel = lightLevel;
			if (lightLevel > 0) lightLevel = (byte)(Math.max(lightLevel - BlockStateVersions.getOpacity(segment.value, EmptyBlockGetter.INSTANCE, BlockPos.ZERO) * (segment.maxY() - segment.minY()), 0));
		}
	}

	public static class LitSegment extends ObjectSegment<BlockState> {

		public byte skylightLevel = -1;

		public LitSegment(int minY, int maxY) {
			super(minY, maxY);
		}

		public int getLightLevel(int y, int lod) {
			return Mth.clamp(this.skylightLevel - ((this.maxY - y) << lod) * BlockStateVersions.getOpacity(this.value, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 0, 15);
		}

		public int getBlockLight() {
			return this.value.getLightEmission();
		}

		public int minY() {
			return this.minY;
		}

		public int maxY() {
			return this.maxY + 1; //convert to exclusive.
		}

		@Override
		public String toString() {
			return super.toString() + ", skylight: " + this.skylightLevel;
		}
	}
}