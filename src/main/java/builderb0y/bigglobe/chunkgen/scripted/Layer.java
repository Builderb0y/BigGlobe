package builderb0y.bigglobe.chunkgen.scripted;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.Valid;

@UseCoder(name = "REGISTRY", in = Layer.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class Layer implements CoderRegistryTyped<Layer> {

	public static final CoderRegistry<Layer> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("scripted_chunk_generator_layer"));
	static {
		REGISTRY.registerAuto(BigGlobeMod.modID("simple_2d"), Simple2DLayer.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("simple_3d"), Simple3DLayer.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("multi_state_3d"), MultiState3DLayer.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("scripted"), ScriptedLayer.class);
	}

	public final @VerifyNullable Valid valid;
	public final Layer @DefaultEmpty [] children;
	public final SurfaceScript.@VerifyNullable Holder before_children, after_children;

	public Layer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder before_children,
		SurfaceScript.@VerifyNullable Holder after_children
	) {
		this.valid = valid;
		this.children = children;
		this.before_children = before_children;
		this.after_children = after_children;
	}

	public abstract void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks);

	public void emitSegments(ScriptedColumn column, ScriptedColumn altX, ScriptedColumn altZ, ScriptedColumn altXZ, BlockSegmentList segments) {
		if (this.validWhere(column)) {
			BlockSegmentList bounded = segments.split(segments.minY(), segments.maxY());
			if (bounded != null) {
				this.emitSelfSegments(column, bounded);
				if (this.before_children != null) {
					this.before_children.generateSurface(column, altX, altZ, altXZ, bounded);
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
				if (this.after_children != null) {
					this.after_children.generateSurface(column, altX, altZ, altXZ, bounded);
				}
				segments.mergeAndKeepEverywhere(bounded);
			}
		}
	}

	public void emitSegments(ScriptedColumn column, BlockSegmentList segments) {
		if (this.validWhere(column)) {
			BlockSegmentList bounded = segments.split(this.validMinY(column), this.validMaxY(column));
			if (bounded != null) {
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
	}

	public boolean validWhere(ScriptedColumn column) {
		return this.valid == null || this.valid.where == null || this.valid.where.get(column);
	}

	public int validMinY(ScriptedColumn column) {
		return this.valid == null || this.valid.min_y == null ? Integer.MIN_VALUE : this.valid.min_y.get(column);
	}

	public int validMaxY(ScriptedColumn column) {
		return this.valid == null || this.valid.max_y == null ? Integer.MAX_VALUE : this.valid.max_y.get(column);
	}

	public static record Valid(
		ColumnToBooleanScript.@VerifyNullable Holder where,
		ColumnToIntScript.@VerifyNullable Holder min_y,
		ColumnToIntScript.@VerifyNullable Holder max_y
	) {}
}