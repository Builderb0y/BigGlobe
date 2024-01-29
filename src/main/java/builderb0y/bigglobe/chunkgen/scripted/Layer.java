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

@UseCoder(name = "REGISTRY", in = Layer.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class Layer implements CoderRegistryTyped<Layer> {

	public static final CoderRegistry<Layer> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("scripted_chunk_generator_layer"));
	static {
		REGISTRY.registerAuto(BigGlobeMod.modID("simple_2d"), Simple2DLayer.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("simple_3d"), Simple3DLayer.class);
	}

	public final @VerifyNullable Valid valid;
	public final Layer @DefaultEmpty [] children;
	public final SurfaceScript.@VerifyNullable Holder top_surface, bottom_surface;

	public Layer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder top_surface,
		SurfaceScript.@VerifyNullable Holder bottom_surface
	) {
		this.valid = valid;
		this.children = children;
		this.top_surface = top_surface;
		this.bottom_surface = bottom_surface;
	}

	public abstract void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks);

	public void emitSegments(ScriptedColumn column, ScriptedColumn altX, ScriptedColumn altZ, ScriptedColumn altXZ, BlockSegmentList segments) {
		if (this.validWhere(column)) {
			BlockSegmentList bounded = segments.split(segments.minY(), segments.maxY());
			this.emitSelfSegments(column, bounded);

			if (this.bottom_surface != null || this.top_surface != null) {
				int[] bottomSurfaces = null, topSurfaces = null;

				if (this.bottom_surface != null) {
					int size = bounded.size();
					bottomSurfaces = new int[size];
					for (int index = 0; index < size; index++) {
						bottomSurfaces[index] = bounded.get(index).minY;
					}
				}

				if (this.top_surface != null) {
					int size = bounded.size();
					topSurfaces = new int[size];
					for (int index = 0; index < size; index++) {
						topSurfaces[index] = bounded.get(index).maxY;
					}
				}

				if (this.bottom_surface != null) {
					for (int segmentIndex = 0, segmentCount = bottomSurfaces.length; segmentIndex < segmentCount; segmentIndex++) {
						this.bottom_surface.generateSurface(
							column,
							altX,
							altZ,
							altXZ,
							bottomSurfaces[segmentIndex],
							bounded
						);
					}
				}

				if (this.top_surface != null) {
					for (int segmentIndex = 0, segmentCount = topSurfaces.length; segmentIndex < segmentCount; segmentIndex++) {
						this.top_surface.generateSurface(
							column,
							altX,
							altZ,
							altXZ,
							topSurfaces[segmentIndex],
							bounded
						);
					}
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

	public boolean validWhere(ScriptedColumn column) {
		ColumnToBooleanScript.Holder where = this.valid.where;
		return where == null || where.get(column);
	}

	public int validMinY(ScriptedColumn column) {
		ColumnToIntScript.Holder minY = this.valid.min_y;
		return minY == null ? Integer.MIN_VALUE : minY.get(column);
	}

	public int validMaxY(ScriptedColumn column) {
		ColumnToIntScript.Holder maxY = this.valid.max_y;
		return maxY == null ? Integer.MAX_VALUE : maxY.get(column);
	}

	public static record Valid(
		ColumnToBooleanScript.@VerifyNullable Holder where,
		ColumnToIntScript.@VerifyNullable Holder min_y,
		ColumnToIntScript.@VerifyNullable Holder max_y
	) {}
}