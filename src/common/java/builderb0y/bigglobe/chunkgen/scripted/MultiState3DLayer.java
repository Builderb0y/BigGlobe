package builderb0y.bigglobe.chunkgen.scripted;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted2.ColumnScript.ColumnYToBlockStateScript;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;

public class MultiState3DLayer extends Layer {

	public final ColumnYToBlockStateScript.Catcher state;

	public MultiState3DLayer(
		@VerifyNullable Valid valid,
		Holder<Layer> @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Catcher before_children,
		SurfaceScript.@VerifyNullable Catcher after_children,
		ColumnYToBlockStateScript.Catcher state
	) {
		super(valid, children, before_children, after_children);
		this.state = state;
	}

	@Override
	public void buildDependencyStream(Stream.Builder<Holder<? extends DependencyView>> builder) {
		this.state.streamDirectDependencies().forEach(builder);
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		int minY = Math.max(this.validMinY(column), blocks.minY());
		int maxY = Math.min(this.validMaxY(column), blocks.maxY());
		int start = minY;
		BlockState state = this.state.get(column, minY);
		for (int y = minY; ++y < maxY; ) {
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