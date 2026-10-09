package builderb0y.bigglobe.chunkgen.scripted;

import java.util.EnumMap;
import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.StandardBlockStatePredicate;
import builderb0y.bigglobe.columns.ColumnScript.ColumnYToBlockStateScript;
import builderb0y.bigglobe.columns.ColumnScript.ColumnYToBooleanScript;
import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.columns.dependencies.DependencyView;

public class Dynamic3DTerrainLayer extends TerrainLayer {

	public final ColumnYToBlockStateScript.Catcher state;
	public final ColumnYToBooleanScript.Catcher condition;
	public final @DefaultEmpty EnumMap<StandardBlockStatePredicate, Boolean> guarantees;

	public Dynamic3DTerrainLayer(
		@VerifyNullable Valid valid,
		Holder<TerrainLayer> @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Catcher before_children,
		SurfaceScript.@VerifyNullable Catcher after_children,
		ColumnYToBlockStateScript.Catcher state,
		ColumnYToBooleanScript.Catcher condition,
		@DefaultEmpty EnumMap<StandardBlockStatePredicate, Boolean> guarantees
	) {
		super(valid, children, before_children, after_children);
		this.state = state;
		this.condition = condition;
		this.guarantees = guarantees;
	}

	@Override
	public void buildDependencyStream(Stream.Builder<Holder<? extends DependencyView>> builder) {
		this.state.streamDirectDependencies().forEach(builder);
		this.condition.streamDirectDependencies().forEach(builder);
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		int minY = Math.max(this.validMinY(column), blocks.minY());
		int maxY = Math.min(this.validMaxY(column), blocks.maxY());
		int start = minY;
		boolean haveState = this.condition.get(column, minY);
		for (int y = minY; ++y < maxY;) {
			boolean nextState = this.condition.get(column, y);
			if (haveState != nextState) {
				if (haveState) blocks.setDynamicBlockStates(start, y, this.state, this.guarantees);
				haveState = nextState;
				start = y;
			}
		}
		if (haveState) blocks.setDynamicBlockStates(start, maxY, this.state, this.guarantees);
	}
}