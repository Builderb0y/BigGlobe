package builderb0y.bigglobe.chunkgen.scripted;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public abstract class Layer implements CoderRegistryTyped<Layer>, SegmentEmitter {

	public final @VerifyNullable Valid valid;
	public final Layer @DefaultEmpty [] children;

	public Layer(@VerifyNullable Valid valid, Layer[] children) {
		this.valid = valid;
		this.children = children;
	}

	public abstract <B extends BlockSegmentConsumer<B>> void emitSelfSegments(ScriptedColumn column, B consumer);

	@Override
	public <B extends BlockSegmentConsumer<B>> void emitSegments(ScriptedColumn column, B consumer) {
		this.emitSelfSegments(column, consumer);
		if (this.children.length != 0) {
			B split = consumer.split();
			for (Layer child : this.children) {
				B split2 = split.split();
				child.emitSegments(column, split2);
				split.mergeAndKeepWhereThereArentBlocks(split2);
			}
			consumer.mergeAndKeepWhereThereAreBlocks(split);
		}
	}

	public static record Valid(
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y
	) {}
}