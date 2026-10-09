package builderb0y.bigglobe.chunkgen.scripted;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blockdefs.BlockStates;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.BlockStateProvider;
import builderb0y.bigglobe.columns.ColumnScript.ColumnYToBlockStateScript;
import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class BlockSegmentList extends AbstractObjectSegmentList<BlockStateProvider, BlockSegmentList.LitSegment> {

	public BlockSegmentList(int minY, int maxY) {
		super(minY, maxY - 1 /* convert to inclusive */);
	}

	public BlockState[] flattenBlockStates(ScriptedColumn column) {
		int arraySize = this.maxY - this.minY;
		//worst case scenario: Integer.MAX_VALUE - Integer.MIN_VALUE = -1.
		//adding 1 would make this 0 again, and the overflow would become undetectable.
		//that's why we have to split this into 2 conditions.
		if (arraySize < 0 || ++arraySize < 0) {
			throw new OutOfMemoryError("SegmentList covers too big of a Y range for flattening.");
		}
		BlockState[] array = new BlockState[arraySize];
		for (int segmentIndex = 0, size = this.size(); segmentIndex < size; segmentIndex++) {
			LitSegment segment = this.get(segmentIndex);
			int minIndex = segment.minY - this.minY;
			int maxIndex = segment.maxY - this.minY;
			for (int objectIndex = minIndex; objectIndex <= maxIndex; objectIndex++) {
				array[objectIndex] = segment.getBlockState(column, objectIndex + this.minY);
			}
		}
		return array;
	}

	public int minY() {
		return this.minY;
	}

	public int maxY() {
		return this.maxY + 1 /* convert to exclusive */;
	}

	public @Nullable BlockState getBlockState(ScriptedColumn column, int y) {
		BlockStateProvider provider = this.getOverlappingObject(y);
		return provider == null ? null : provider.getBlockState(column, y);
	}

	public void setBlockState(int y, BlockState state) {
		if (state != null) this.setBlockStates(y, y + 1, state);
		else this.removeSegment(y, y + 1);
	}

	public void setBlockStates(int minY, int maxY, BlockState state) {
		if (state != null) this.addSegment(minY, maxY - 1 /* convert to inclusive */, new FixedBlockStateProvider(state));
		else this.removeSegment(minY, maxY - 1);
	}

	public void setDynamicBlockStates(int minY, int maxY, ColumnYToBlockStateScript.Catcher script, EnumMap<StandardBlockStatePredicate, Boolean> guarantees) {
		if (script != null) this.addSegment(minY, maxY - 1 /* convert to inclusive */, new DynamicBlockStateProvider(script, guarantees));
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

	public void computeLightLevels(ScriptedColumn column, byte topLightLevel) {
		byte lightLevel = topLightLevel;
		for (int index = this.size(); --index >= 0;) {
			LitSegment segment = this.get(index);
			segment.skylightLevel = lightLevel;
			if (lightLevel > 0) lightLevel = (byte)(Math.max(lightLevel - BlockStateVersions.getOpacity(segment.getBlockState(column, segment.maxY), EmptyBlockGetter.INSTANCE, BlockPos.ZERO) * (segment.maxY() - segment.minY()), 0));
		}
	}

	public static class LitSegment extends ObjectSegment<BlockStateProvider> {

		public byte skylightLevel = -1;

		public LitSegment(int minY, int maxY) {
			super(minY, maxY);
		}

		public void forEach(ScriptedColumn column, SegmentConsumer action) {
			this.value.forEach(column, this.minY, this.maxY, action);
		}

		public int getSkyLight(ScriptedColumn column, int y, int lod) {
			return Mth.clamp(this.skylightLevel - ((this.maxY - y) << lod) * BlockStateVersions.getOpacity(this.getBlockState(column, y), EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 0, 15);
		}

		public BlockState getBlockState(ScriptedColumn column, int y) {
			return this.value.getBlockState(column, y);
		}

		public BlockState getFixedBlockState() {
			return ((FixedBlockStateProvider)(this.value)).state;
		}

		public int getBlockLight(ScriptedColumn column, int y) {
			return this.getBlockState(column, y).getLightEmission();
		}

		public int minY() {
			return this.minY;
		}

		public int maxY() {
			return this.maxY + 1; //convert to exclusive.
		}

		@Override
		public boolean canMergeWith(Segment that) {
			return this.value.equals(((LitSegment)(that)).value);
		}

		@Override
		public String toString() {
			return super.toString() + ", skylight: " + this.skylightLevel;
		}
	}

	public static interface SegmentConsumer {

		public abstract void accept(int minY, int maxY, BlockState state);
	}

	public static sealed interface BlockStateProvider {

		public abstract BlockState getBlockState(ScriptedColumn column, int y);

		public abstract int query(StandardBlockStatePredicate predicate);

		public abstract void forEach(ScriptedColumn column, int minY, int maxY, SegmentConsumer action);
	}

	public static record FixedBlockStateProvider(BlockState state) implements BlockStateProvider {

		public FixedBlockStateProvider {
			Objects.requireNonNull(state, "state");
		}

		@Override
		public BlockState getBlockState(ScriptedColumn column, int y) {
			return this.state;
		}

		@Override
		public int query(StandardBlockStatePredicate predicate) {
			return predicate.test(this.state) ? 1 : -1;
		}

		@Override
		public void forEach(ScriptedColumn column, int minY, int maxY, SegmentConsumer action) {
			action.accept(minY, maxY, this.state);
		}
	}

	public static record DynamicBlockStateProvider(ColumnYToBlockStateScript.Catcher script, EnumMap<StandardBlockStatePredicate, Boolean> guarantees) implements BlockStateProvider {

		public DynamicBlockStateProvider {
			Objects.requireNonNull(script, "script");
		}

		@Override
		public BlockState getBlockState(ScriptedColumn column, int y) {
			BlockState state = this.script.get(column, y);
			return state != null ? state : BlockStates.AIR;
		}

		@Override
		public int query(StandardBlockStatePredicate predicate) {
			Boolean nullable = this.guarantees.get(predicate);
			return nullable == null ? 0 : (nullable.booleanValue() ? 1 : -1);
		}

		@Override
		public void forEach(ScriptedColumn column, int minY, int maxY, SegmentConsumer action) {
			int previousY = minY;
			BlockState previous = this.getBlockState(column, previousY);
			for (int y = minY; ++y <= maxY;) {
				BlockState next = this.getBlockState(column, y);
				if (next != previous) {
					action.accept(previousY, y - 1, previous);
					previous = next;
					previousY = y;
				}
			}
			action.accept(previousY, maxY, previous);
		}
	}

	public static enum StandardBlockStatePredicate implements Predicate<BlockState>, StringRepresentable {
		AIR,
		SOLID,
		IS_FLUID,
		HAS_FLUID;

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		@Override
		public String getSerializedName() {
			return this.lowerCaseName;
		}

		@Override
		public boolean test(BlockState state) {
			return switch (this) {
				case AIR -> state.isAir();
				case SOLID -> state.isSolidRender();
				case IS_FLUID -> state.getBlock() instanceof LiquidBlock;
				case HAS_FLUID -> !state.getFluidState().isEmpty();
			};
		}
	}
}