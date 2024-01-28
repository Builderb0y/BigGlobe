package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class RootLayer extends Layer {

	public final BlockState state;

	public RootLayer(Layer @DefaultEmpty [] children, BlockState state) {
		super(null, children, null, null);
		this.state = state;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList consumer) {
		consumer.setBlockStates(consumer.minY(), consumer.maxY(), this.state);
	}
}