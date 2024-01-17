package builderb0y.bigglobe.chunkgen.scripted;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public interface SegmentEmitter {

	public abstract <B extends BlockSegmentConsumer<B>> void emitSegments(ScriptedColumn column, B consumer);
}