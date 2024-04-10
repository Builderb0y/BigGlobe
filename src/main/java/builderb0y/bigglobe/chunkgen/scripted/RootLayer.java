package builderb0y.bigglobe.chunkgen.scripted;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBlockStateScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class RootLayer extends Layer {

	public final ColumnToBlockStateScript.Holder state;

	public RootLayer(Layer @DefaultEmpty [] children, ColumnToBlockStateScript.Holder state) {
		super(null, children, null, null);
		this.state = state;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList consumer) {
		consumer.setBlockStates(consumer.minY(), consumer.maxY(), this.state.get(column));
	}
}