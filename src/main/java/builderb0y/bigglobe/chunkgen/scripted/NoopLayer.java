package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

@RecordLike({})
public class NoopLayer extends Layer {

	@SuppressWarnings("unchecked")
	public static final RegistryEntry<Layer>[] EMPTY_CHILDREN = new RegistryEntry[0];

	public NoopLayer() {
		super(null, EMPTY_CHILDREN, null, null);
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