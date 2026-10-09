package builderb0y.bigglobe.chunkgen.scripted;

import java.util.EnumMap;
import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.StandardBlockStatePredicate;
import builderb0y.bigglobe.columns.ColumnScript.ColumnYToBlockStateScript;
import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.columns.dependencies.DependencyView;

public class Dynamic2DTerrainLayer extends TerrainLayer {

	public final ColumnYToBlockStateScript.Catcher state;
	public final @DefaultEmpty EnumMap<StandardBlockStatePredicate, Boolean> guarantees;

	public Dynamic2DTerrainLayer(
		@VerifyNullable Valid valid,
		Holder<TerrainLayer> @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Catcher before_children,
		SurfaceScript.@VerifyNullable Catcher after_children,
		ColumnYToBlockStateScript.Catcher state,
		@DefaultEmpty EnumMap<StandardBlockStatePredicate, Boolean> guarantees
	) {
		super(valid, children, before_children, after_children);
		this.state = state;
		this.guarantees = guarantees;
	}

	@Override
	public void buildDependencyStream(Stream.Builder<Holder<? extends DependencyView>> builder) {
		this.state.streamDirectDependencies().forEach(builder);
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		blocks.setDynamicBlockStates(this.validMinY(column), this.validMaxY(column), this.state, this.guarantees);
	}
}