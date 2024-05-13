package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToBlockStateScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class MultiState3DLayer extends Layer {

	public final ColumnYToBlockStateScript.Holder state;

	public MultiState3DLayer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder before_children,
		SurfaceScript.@VerifyNullable Holder after_children,
		ColumnYToBlockStateScript.Holder state
	) {
		super(valid, children, before_children, after_children);
		this.state = state;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		int minY = Math.max(this.validMinY(column), blocks.minY());
		int maxY = Math.min(this.validMaxY(column), blocks.maxY());
		int start = minY;
		BlockState state = this.state.get(column, minY);
		for (int y = minY; ++y < maxY;) {
			BlockState nextState = this.state.get(column, y);
			if (state != nextState) {
				if (state != null) blocks.setBlockStates(start, y, state);
				state = nextState;
				start = y;
			}
		}
		if (state != null) blocks.setBlockStates(start, maxY, state);
	}
}