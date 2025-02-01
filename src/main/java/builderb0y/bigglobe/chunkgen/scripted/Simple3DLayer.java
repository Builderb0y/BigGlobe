package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBlockStateScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class Simple3DLayer extends Layer {

	public final ColumnToBlockStateScript.Holder state;
	public final ColumnYToBooleanScript.Holder condition;

	public Simple3DLayer(
		@VerifyNullable Valid valid,
		RegistryEntry<Layer> @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder before_children,
		SurfaceScript.@VerifyNullable Holder after_children,
		ColumnToBlockStateScript.Holder state,
		ColumnYToBooleanScript.Holder condition
	) {
		super(valid, children, before_children, after_children);
		this.state = state;
		this.condition = condition;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		BlockState state = this.state.get(column);
		int minY = Math.max(this.validMinY(column), blocks.minY());
		int maxY = Math.min(this.validMaxY(column), blocks.maxY());
		int start = minY;
		boolean haveState = this.condition.get(column, minY);
		for (int y = minY; ++y < maxY;) {
			boolean nextState = this.condition.get(column, y);
			if (haveState != nextState) {
				if (haveState) blocks.setBlockStates(start, y, state);
				haveState = nextState;
				start = y;
			}
		}
		if (haveState) blocks.setBlockStates(start, maxY, state);
	}
}