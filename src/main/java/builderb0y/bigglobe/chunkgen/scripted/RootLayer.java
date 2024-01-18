package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class RootLayer extends Layer {

	public final BlockState state;

	public RootLayer(BlockState state, Layer[] children) {
		super(null, children);
		this.state = state;
	}

	@Override
	public <B extends BlockSegmentConsumer<B>> void emitSelfSegments(ScriptedColumn column, B consumer) {
		consumer.accept(consumer.minY(), consumer.maxY(), this.state);
	}
}