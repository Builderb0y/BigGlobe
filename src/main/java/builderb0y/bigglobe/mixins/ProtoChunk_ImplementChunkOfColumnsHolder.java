package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;

import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.mixinInterfaces.ChunkOfColumnsHolder;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunk_ImplementChunkOfColumnsHolder extends Chunk implements ChunkOfColumnsHolder {

	@Unique
	public ChunkOfColumns<? extends WorldColumn> bigglobe_chunkOfColumns;

	public ProtoChunk_ImplementChunkOfColumnsHolder() {
		super(null, null, null, null, 0L, null, null);
	}

	@Override
	public ChunkOfColumns<? extends WorldColumn> bigglobe_getChunkOfColumns() {
		return this.bigglobe_chunkOfColumns;
	}

	@Override
	public void bigglobe_setChunkOfColumns(ChunkOfColumns<? extends WorldColumn> columns) {
		this.bigglobe_chunkOfColumns = columns;
	}
}