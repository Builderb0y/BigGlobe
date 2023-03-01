package builderb0y.bigglobe.chunkgen;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.chunk.ProtoChunk;

import builderb0y.bigglobe.columns.ChunkOfColumns;
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