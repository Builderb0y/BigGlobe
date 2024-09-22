package builderb0y.bigglobe.chunkgen.scripted;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBlockStateScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class Simple2DLayer extends Layer {

	public final ColumnToBlockStateScript.Holder state;

	public Simple2DLayer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder before_children,
		SurfaceScript.@VerifyNullable Holder after_children,
		ColumnToBlockStateScript.Holder state
	) {
		super(valid, children, before_children, after_children);
		this.state = state;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		blocks.setBlockStates(this.validMinY(column), this.validMaxY(column), this.state.get(column));
	}
}