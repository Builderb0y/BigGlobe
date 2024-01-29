package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class Simple3DLayer extends Layer {

	public final BlockState state;
	public final ColumnYToBooleanScript.Holder condition;

	public Simple3DLayer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder top_surface,
		SurfaceScript.@VerifyNullable Holder bottom_surface,
		BlockState state,
		ColumnYToBooleanScript.Holder condition
	) {
		super(valid, children, top_surface, bottom_surface);
		this.state = state;
		this.condition = condition;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList consumer) {
		int minY = Math.max(this.validMinY(column), consumer.minY());
		int maxY = Math.min(this.validMaxY(column), consumer.maxY());
		int start = minY;
		boolean haveState = this.condition.get(column, minY);
		for (int y = minY; ++y < maxY;) {
			boolean nextState = this.condition.get(column, y);
			if (haveState != nextState) {
				if (haveState) consumer.setBlockStates(start, y, this.state);
				haveState = nextState;
				start = y;
			}
		}
		if (haveState) consumer.setBlockStates(start, maxY, this.state);
	}
}