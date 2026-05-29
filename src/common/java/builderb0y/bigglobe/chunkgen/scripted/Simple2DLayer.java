package builderb0y.bigglobe.chunkgen.scripted;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBlockStateScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;

public class Simple2DLayer extends Layer {

	public final ColumnToBlockStateScript.Catcher state;

	public Simple2DLayer(
		@VerifyNullable Valid valid,
		Holder<Layer> @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Catcher before_children,
		SurfaceScript.@VerifyNullable Catcher after_children,
		ColumnToBlockStateScript.Catcher state
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
		blocks.setBlockStates(this.validMinY(column), this.validMaxY(column), this.state.get(column));
	}
}