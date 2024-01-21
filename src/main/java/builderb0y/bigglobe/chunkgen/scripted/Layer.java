package builderb0y.bigglobe.chunkgen.scripted;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

@UseCoder(name = "REGISTRY", in = Layer.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
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
	public final @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> top_surface, bottom_surface;
	public transient SurfaceScript.@VerifyNullable Holder topSurface, bottomSurface;

	public Layer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> top_surface,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> bottom_surface
	) {
		this.valid = valid;
		this.children = children;
		this.top_surface = top_surface;
		this.bottom_surface = bottom_surface;
	}

	public abstract void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks);

	public void emitSegments(ScriptedColumn column, ScriptedColumn altX, ScriptedColumn altZ, ScriptedColumn altXZ, BlockSegmentList segments) {
		if (this.validWhere(column)) {
			BlockSegmentList bounded = segments.split(this.validMinY(column), this.validMaxY(column));
			this.emitSelfSegments(column, bounded);

			if (this.bottomSurface != null) {
				for (int segmentIndex = 0, segmentCount = bounded.size(); segmentIndex < segmentCount; segmentIndex++) {
					this.bottomSurface.generateSurface(
						column,
						altX,
						altZ,
						altXZ,
						bounded.get(segmentIndex).minY,
						bounded
					);
				}
			}

			if (this.topSurface != null) {
				for (int segmentIndex = 0, segmentCount = bounded.size(); segmentIndex < segmentCount; segmentIndex++) {
					this.topSurface.generateSurface(
						column,
						altX,
						altZ,
						altXZ,
						bounded.get(segmentIndex).maxY,
						bounded
					);
				}
			}

			if (this.children.length != 0) {
				BlockSegmentList split = bounded.split(bounded.minY(), bounded.maxY());
				BlockSegmentList split2 = split.split(split.minY(), split.maxY());
				for (Layer child : this.children) {
					child.emitSegments(column, altX, altZ, altXZ, split2);
					split.mergeAndKeepWhereThereArentBlocks(split2);
					split2.reset();
				}
				bounded.mergeAndKeepWhereThereAreBlocks(split);
			}
			segments.mergeAndKeepEverywhere(bounded);
		}
	}

	public void emitSegments(ScriptedColumn column, BlockSegmentList segments) {
		if (this.validWhere(column)) {
			BlockSegmentList bounded = segments.split(this.validMinY(column), this.validMaxY(column));
			this.emitSelfSegments(column, bounded);

			if (this.children.length != 0) {
				BlockSegmentList split = bounded.split(bounded.minY(), bounded.maxY());
				BlockSegmentList split2 = split.split(split.minY(), split.maxY());
				for (Layer child : this.children) {
					child.emitSegments(column, split2);
					split.mergeAndKeepWhereThereArentBlocks(split2);
					split2.reset();
				}
				bounded.mergeAndKeepWhereThereAreBlocks(split);
			}
			segments.mergeAndKeepEverywhere(bounded);
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
		if (this.top_surface != null) {
			this.topSurface = new SurfaceScript.Holder(this.top_surface, registry);
		}
		if (this.bottom_surface != null) {
			this.bottomSurface = new SurfaceScript.Holder(this.bottom_surface, registry);
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