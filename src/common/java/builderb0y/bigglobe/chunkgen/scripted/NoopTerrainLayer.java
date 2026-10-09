package builderb0y.bigglobe.chunkgen.scripted;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.columns.dependencies.DependencyView;

@RecordLike({})
public class NoopTerrainLayer extends TerrainLayer {

	@SuppressWarnings("unchecked")
	public static final Holder<TerrainLayer>[] EMPTY_CHILDREN = new Holder[0];

	public NoopTerrainLayer() {
		super(null, EMPTY_CHILDREN, null, null);
	}

	@Override
	public void buildDependencyStream(Stream.Builder<Holder<? extends DependencyView>> builder) {
		//no-op.
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		//no-op.
	}

	@Override
	public void emitSegments(ScriptedColumn column, ScriptedColumn altX, ScriptedColumn altZ, ScriptedColumn altXZ, BlockSegmentList parentSegments) {
		//no-op.
	}

	@Override
	public void emitSegments(ScriptedColumn column, BlockSegmentList parentSegments) {
		//no-op.
	}
}