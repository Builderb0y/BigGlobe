package builderb0y.bigglobe.mixinInterfaces;

import net.minecraft.world.chunk.ProtoChunk;

import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.mixins.ProtoChunk_ImplementChunkOfColumnsHolder;

/**
implemented by {@link ProtoChunk}, see {@link ProtoChunk_ImplementChunkOfColumnsHolder}

it is expensive to compute some column values,
so I'd rather only do it once, not twice or potentially more.
unfortunately, they are needed in several stages of terrain gen,
including biomes, raw terrain, and features.
so, instead of computing column values once per stage,
I have opted to store the columns on the chunk itself,
to be re-used later when needed.
*/
public interface ChunkOfColumnsHolder {

	public abstract ChunkOfColumns<? extends WorldColumn> bigglobe_getChunkOfColumns();

	public abstract void bigglobe_setChunkOfColumns(ChunkOfColumns<? extends WorldColumn> columns);
}