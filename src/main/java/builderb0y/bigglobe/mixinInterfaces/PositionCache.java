package builderb0y.bigglobe.mixinInterfaces;

import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.chunk.ProtoChunk;

import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.columns.OverworldColumn;

public interface PositionCache {

	public static int encodeIndex(int x, int z) {
		return ((z & 15) << 4) | (x & 15);
	}

	public static class OverworldPositionCache implements PositionCache {

		public IntList[] caveFloors, caveCeilings;

		public OverworldPositionCache(ChunkOfColumns<OverworldColumn> columns) {
			this.caveFloors     = new IntList[256];
			this.caveCeilings   = new IntList[256];
			for (int index = 0; index < 256; index++) {
				OverworldColumn column = columns.getColumn(index);
				this.caveFloors  [index] = column.caveFloors;
				this.caveCeilings[index] = column.caveCeilings;
			}
		}

		public @Nullable IntList getCaveFloors(int index) {
			return this.caveFloors[index];
		}

		public @Nullable IntList getCaveCeilings(int index) {
			return this.caveCeilings[index];
		}
	}

	public static class NetherPositionCache implements PositionCache {

		public IntList[] caveFloors, caveCeilings, cavernFloors, cavernCeilings;

		public NetherPositionCache(ChunkOfColumns<NetherColumn> columns) {
			this.caveFloors     = new IntList[256];
			this.caveCeilings   = new IntList[256];
			this.cavernFloors   = new IntList[256];
			this.cavernCeilings = new IntList[256];
			for (int index = 0; index < 256; index++) {
				NetherColumn column = columns.getColumn(index);
				this.caveFloors    [index] = column.caveFloors;
				this.caveCeilings  [index] = column.caveCeilings;
				this.cavernFloors  [index] = column.cavernFloors;
				this.cavernCeilings[index] = column.cavernCeilings;
			}
		}
	}

	public static class EndPositionCache implements PositionCache {

		public IntList[]
			lowerRingCloudFloors,
			lowerRingCloudCeilings,
			upperRingCloudFloors,
			upperRingCloudCeilings,
			lowerBridgeCloudFloors,
			lowerBridgeCloudCeilings,
			upperBridgeCloudFloors,
			upperBridgeCloudCeilings;

		public EndPositionCache(ChunkOfColumns<EndColumn> columns) {
			this.lowerRingCloudFloors     = collect(columns, (EndColumn column) -> column.lowerRingCloudFloorLevels);
			this.lowerRingCloudCeilings   = collect(columns, (EndColumn column) -> column.lowerRingCloudCeilingLevels);
			this.upperRingCloudFloors     = collect(columns, (EndColumn column) -> column.upperRingCloudFloorLevels);
			this.upperRingCloudCeilings   = collect(columns, (EndColumn column) -> column.upperRingCloudCeilingLevels);
			this.lowerBridgeCloudFloors   = collect(columns, (EndColumn column) -> column.lowerBridgeCloudFloorLevels);
			this.lowerBridgeCloudCeilings = collect(columns, (EndColumn column) -> column.lowerBridgeCloudCeilingLevels);
			this.upperBridgeCloudFloors   = collect(columns, (EndColumn column) -> column.upperBridgeCloudFloorLevels);
			this.upperBridgeCloudCeilings = collect(columns, (EndColumn column) -> column.upperBridgeCloudCeilingLevels);
		}

		public static IntList[] collect(ChunkOfColumns<EndColumn> columns, Function<EndColumn, IntList> getter) {
			IntList[] array = null;
			for (int index = 0; index < 256; index++) {
				IntList list = getter.apply(columns.getColumn(index));
				if (list != null) {
					if (array == null) array = new IntList[256];
					array[index] = list;
				}
			}
			return array;
		}
	}

	/**
	implemented by {@link ProtoChunk}.

	it is expensive to compute cave noise,
	so I'd rather only do it once, not twice.
	unfortunately, it is needed to place caves
	in the raw terrain generation stage,
	but it is also needed to place decorations
	inside caves in the terrain decoration stage.
	so, instead of computing noise once per stage,
	I have opted to store the data about caves on the
	chunk itself, to be re-used later when needed.
	*/
	public static interface PositionCacheHolder {

		public abstract PositionCache bigglobe_getPositionCache();

		public abstract void bigglobe_setPositionCache(PositionCache cache);
	}
}