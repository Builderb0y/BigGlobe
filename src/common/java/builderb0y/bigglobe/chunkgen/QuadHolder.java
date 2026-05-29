package builderb0y.bigglobe.chunkgen;

import java.lang.invoke.MethodHandle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.LiquidBlock;

import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ColumnValueInfo;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.Params;
import builderb0y.bigglobe.overriders.ColumnValueOverrider.Catcher;
import builderb0y.bigglobe.structures.ScriptStructures;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class QuadHolder<T> {

	public T object00, object01, object10, object11;

	public QuadHolder() {}

	public QuadHolder(T object00, T object01, T object10, T object11) {
		this.set(object00, object01, object10, object11);
	}

	public void set(T object00, T object01, T object10, T object11) {
		this.object00 = object00;
		this.object01 = object01;
		this.object10 = object10;
		this.object11 = object11;
	}

	public void loadFromArray(T[] array, int baseIndex, int zSkip) {
		this.object00 = array[baseIndex];
		this.object01 = array[baseIndex + 1];
		this.object10 = array[baseIndex + zSkip];
		this.object11 = array[baseIndex + zSkip + 1];
	}

	public void storeInArray(T[] array, int baseIndex, int zSkip) {
		array[baseIndex] = this.object00;
		array[baseIndex + 1] = this.object01;
		array[baseIndex + zSkip] = this.object10;
		array[baseIndex + zSkip + 1] = this.object11;
	}

	public void fillNullsFrom(QuadHolder<T> that) {
		if (this.object00 == null) this.object00 = that.object00;
		if (this.object01 == null) this.object01 = that.object01;
		if (this.object10 == null) this.object10 = that.object10;
		if (this.object11 == null) this.object11 = that.object11;
	}

	public boolean allNull() {
		return this.object00 == null && this.object01 == null && this.object10 == null && this.object11 == null;
	}

	public boolean anyNull() {
		return this.object00 == null || this.object01 == null || this.object10 == null || this.object11 == null;
	}

	public boolean allNotNull() {
		return this.object00 != null && this.object01 != null && this.object10 != null && this.object11 != null;
	}

	public boolean anyNotNull() {
		return this.object00 != null || this.object01 != null || this.object10 != null || this.object11 != null;
	}

	public static class QuadColumn extends QuadHolder<ScriptedColumn> {

		public QuadColumn() {}

		public QuadColumn(ScriptedColumn object00, ScriptedColumn object01, ScriptedColumn object10, ScriptedColumn object11) {
			super(object00, object01, object10, object11);
		}

		public void at(Params params, int x, int z, int step) {
			this.object00.setParamsUnchecked(params.at(x, z));
			this.object01.setParamsUnchecked(params.at(x + step, z));
			this.object10.setParamsUnchecked(params.at(x, z + step));
			this.object11.setParamsUnchecked(params.at(x + step, z + step));
		}

		public void preComputeColumnValue(ColumnValueInfo info) throws Throwable {
			//I checked the bytecode and *not* having a manual cast here is fine,
			//but I don't want this to break if javac ever changes its generics handling.
			info.preComputer().invokeExact((ScriptedColumn)(this.object00));
			info.preComputer().invokeExact((ScriptedColumn)(this.object01));
			info.preComputer().invokeExact((ScriptedColumn)(this.object10));
			info.preComputer().invokeExact((ScriptedColumn)(this.object11));
		}

		public void override(Catcher overrider, ScriptStructures structures) {
			overrider.override(this.object00, structures);
			overrider.override(this.object01, structures);
			overrider.override(this.object10, structures);
			overrider.override(this.object11, structures);
		}
	}

	public static class QuadList extends QuadHolder<BlockSegmentList> {

		public QuadList() {}

		public QuadList(BlockSegmentList object00, BlockSegmentList object01, BlockSegmentList object10, BlockSegmentList object11) {
			super(object00, object01, object10, object11);
		}

		public void createNew(int minY, int maxY) {
			this.object00 = new BlockSegmentList(minY, maxY);
			this.object01 = new BlockSegmentList(minY, maxY);
			this.object10 = new BlockSegmentList(minY, maxY);
			this.object11 = new BlockSegmentList(minY, maxY);
		}

		public void computeLightLevels(byte topLightLevel) {
			this.object00.computeLightLevels(topLightLevel);
			this.object01.computeLightLevels(topLightLevel);
			this.object10.computeLightLevels(topLightLevel);
			this.object11.computeLightLevels(topLightLevel);
		}

		public void downscale(int deltaLod) {
			this.object00 = downscaleColumn(this.object00, deltaLod);
			this.object01 = downscaleColumn(this.object01, deltaLod);
			this.object10 = downscaleColumn(this.object10, deltaLod);
			this.object11 = downscaleColumn(this.object11, deltaLod);
		}

		public void downscaleKeepAir(int deltaLod) {
			this.object00 = downscaleColumnKeepAir(this.object00, deltaLod);
			this.object01 = downscaleColumnKeepAir(this.object01, deltaLod);
			this.object10 = downscaleColumnKeepAir(this.object10, deltaLod);
			this.object11 = downscaleColumnKeepAir(this.object11, deltaLod);
		}

		public static BlockSegmentList downscaleColumn(BlockSegmentList list, int deltaLod) {
			if (list == null) return null;
			BlockSegmentList newList = new BlockSegmentList(list.minY >> deltaLod, (list.maxY >> deltaLod) + 1);
			for (LitSegment segment : list) {
				int minY = segment.minY >> deltaLod, maxY = segment.maxY >> deltaLod;
				if (segment.value.isAir()) {
					LitSegment existing = newList.getOverlappingSegment(minY);
					if (existing != null && !existing.value.isAir()) {
						minY = Math.max(minY, existing.maxY + 1);
					}
				}
				//ensure liquids can't overwrite normal blocks.
				else if (segment.value.getBlock() instanceof LiquidBlock) {
					LitSegment existing = newList.getOverlappingSegment(minY);
					if (existing != null && !existing.value.isAir() && existing.value.getFluidState().isEmpty()) {
						minY = Math.min(Math.max(minY, existing.maxY + 1), maxY);
					}
				}
				newList.addSegment(minY, maxY, segment.value);
			}
			return newList;
		}

		public BlockSegmentList merge() {
			if (this.allNull()) return null;
			BlockSegmentList result = new BlockSegmentList(this.object00.minY(), this.object00.maxY());
			if (this.object00 != null) result.addAllSegments(this.object00);
			if (this.object01 != null) copyAir(this.object01, result);
			if (this.object10 != null) copyAir(this.object10, result);
			if (this.object11 != null) copyAir(this.object11, result);
			return result;
		}

		public static void copyAir(BlockSegmentList source, BlockSegmentList destination) {
			for (LitSegment segment : source) {
				if (!BlockStateVersions.isOpaqueFullCube(segment.value, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
					destination.addSegment(segment.minY, segment.maxY, segment.value);
					LitSegment newSegment = destination.getOverlappingSegment(segment.minY);
					newSegment.skylightLevel = (byte)(Math.max(newSegment.skylightLevel, segment.skylightLevel));
				}
			}
		}

		public static BlockSegmentList downscaleColumnKeepAir(BlockSegmentList list, int deltaLod) {
			if (list == null) return null;
			BlockSegmentList newList = new BlockSegmentList(list.minY >> deltaLod, (list.maxY >> deltaLod) + 1);
			for (LitSegment segment : list) {
				int minY = segment.minY >> deltaLod, maxY = segment.maxY >> deltaLod;
				//ensure culling blocks can't overwrite non-culling blocks.
				if (BlockStateVersions.isOpaqueFullCube(segment.value, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
					LitSegment existing = newList.getOverlappingSegment(minY);
					if (existing != null) {
						minY = Math.max(minY, existing.maxY + 1);
					}
				}
				LitSegment newSegment = newList.addSegment(minY, maxY, segment.value);
				if (newSegment != null) newSegment.skylightLevel = segment.skylightLevel;
			}
			return newList;
		}
	}

	public static void generate(QuadColumn columns, QuadList lists, Layer layer) {
		layer.emitSegments(columns.object00, columns.object01, columns.object10, columns.object11, lists.object00);
		layer.emitSegments(columns.object01, columns.object00, columns.object11, columns.object10, lists.object01);
		layer.emitSegments(columns.object10, columns.object11, columns.object00, columns.object01, lists.object10);
		layer.emitSegments(columns.object11, columns.object10, columns.object01, columns.object00, lists.object11);
	}
}