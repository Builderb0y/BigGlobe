package builderb0y.bigglobe.chunkgen.scripted;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnScripts.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.entries.ColumnScripts.ColumnToIntScript;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

public abstract class Layer implements CoderRegistryTyped<Layer> {

	public static final CoderRegistry<Layer> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("scripted_chunk_generator_layer"));
	static {
		REGISTRY.registerAuto(BigGlobeMod.modID("simple_2d"), Simple2DLayer.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("simple_3d"), Simple3DLayer.class);
	}

	public final @VerifyNullable Valid valid;
	public final Layer @DefaultEmpty [] children;
	public transient ColumnToBooleanScript.@Nullable Holder validWhere;
	public transient ColumnToIntScript.@Nullable Holder validMinY, validMaxY;

	public Layer(@VerifyNullable Valid valid, Layer[] children) {
		this.valid = valid;
		this.children = children;
	}

	public abstract <B extends BlockSegmentConsumer<B>> void emitSelfSegments(ScriptedColumn column, B consumer);

	public <B extends BlockSegmentConsumer<B>> void emitSegments(ScriptedColumn column, B consumer) {
		if (this.validWhere(column)) {
			B bounded = consumer.split(this.validMinY(column), this.validMaxY(column));
			this.emitSelfSegments(column, bounded);
			if (this.children.length != 0) {
				B split = bounded.split(bounded.minY(), bounded.maxY());
				B split2 = split.split(split.minY(), split.maxY());
				for (Layer child : this.children) {
					child.emitSegments(column, split2);
					split.mergeAndKeepWhereThereArentBlocks(split2);
					split2.reset();
				}
				bounded.mergeAndKeepWhereThereAreBlocks(split);
			}
			consumer.mergeAndKeepEverywhere(bounded);
		}
	}

	public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
		Valid valid = this.valid;
		if (valid != null) {
			if (valid.where != null) {
				this.validWhere = new ColumnToBooleanScript.Holder(valid.where, registry);
			}
			if (valid.min_y != null) {
				this.validMinY = new ColumnToIntScript.Holder(valid.min_y, registry);
			}
			if (valid.max_y != null) {
				this.validMaxY = new ColumnToIntScript.Holder(valid.max_y, registry);
			}
		}
		if (this.children != null) {
			for (Layer child : this.children) {
				child.compile(registry);
			}
		}
	}

	public boolean validWhere(ScriptedColumn column) {
		return this.validWhere == null || this.validWhere.get(column);
	}

	public int validMinY(ScriptedColumn column) {
		return this.validMinY == null ? Integer.MIN_VALUE : this.validMinY.get(column);
	}

	public int validMaxY(ScriptedColumn column) {
		return this.validMaxY == null ? Integer.MAX_VALUE : this.validMaxY.get(column);
	}

	public static record Valid(
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y
	) {}
}