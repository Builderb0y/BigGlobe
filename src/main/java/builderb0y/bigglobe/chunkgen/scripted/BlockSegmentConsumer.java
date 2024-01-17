package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

public interface BlockSegmentConsumer<B extends BlockSegmentConsumer<B>> {

	public abstract void accept(int minY, int maxY, BlockState state);

	public abstract int minY();

	public abstract int maxY();

	public abstract B split(int minY, int maxY);

	public abstract void mergeAndKeepEverywhere(B that);

	public abstract void mergeAndKeepWhereThereAreBlocks(B that);

	public abstract void mergeAndKeepWhereThereArentBlocks(B that);

	public abstract void reset();
}