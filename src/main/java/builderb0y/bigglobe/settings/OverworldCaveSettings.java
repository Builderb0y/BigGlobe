package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public record OverworldCaveSettings(
	VoronoiDiagram2D placement,
	VariationsList<LocalOverworldCaveSettings> templates,
	@Hidden int maxDepth
) {

	public OverworldCaveSettings(VoronoiDiagram2D placement, VariationsList<LocalOverworldCaveSettings> templates) {
		this(placement, templates, templates.elements.stream().mapToInt(LocalOverworldCaveSettings::depth).max().orElse(0));
	}

	@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
	public static record LocalOverworldCaveSettings(
		double weight,
		Grid3D noise,
		@VerifyNullable Grid3D ledge_noise,
		ColumnYToDoubleScript.Holder width,
		@VerifyIntRange(min = 0, minInclusive = false) int depth,
		@VerifyNullable Grid2D surface_depth_noise,
		@VerifyNullable CaveSurfaceBlocks floor_blocks,
		@VerifyNullable CaveSurfaceBlocks ceiling_blocks,
		@VerifyNullable SortedFeatureTag floor_decorator,
		@VerifyNullable SortedFeatureTag ceiling_decorator
	)
	implements IWeightedListElement {

		public static <T_Encoded> void verify(VerifyContext<T_Encoded, LocalOverworldCaveSettings> context) throws VerifyException {
			LocalOverworldCaveSettings settings = context.object;
			if (settings != null) {
				if (settings.surface_depth_noise == null && (settings.floor_blocks != null || settings.ceiling_blocks != null)) {
					throw new VerifyException("Must specify " + context.pathToString() + " when floor_state or ceiling_state are present.");
				}
			}
		}

		public double getValue(OverworldColumn column, int y) {
			double noise = this.noise.getValue(column.getCaveSeed(), column.x, y, column.z);
			if (this.ledge_noise != null) {
				noise += (
					this.ledge_noise.getValue(column.getCaveSeed(), column.x, y, column.z)
					* this.getWidthSquared(column, y)
				);
			}
			return noise;
		}

		public void getBulkY(OverworldColumn column) {
			int depth = this.depth;
			double[] samples = column.caveNoise;
			if (samples == null) samples = column.caveNoise = new double[depth];
			int startY = column.getFinalTopHeightI() - depth;
			this.noise.getBulkY(column.getCaveSeed(), column.x, startY, column.z, samples, depth);
			if (this.ledge_noise != null) {
				double[] scratch = Grid.getScratchArray(depth);
				try {
					this.ledge_noise.getBulkY(column.getCaveSeed(), column.x, startY, column.z, scratch, depth);
					for (int index = 0; index < depth; index++) {
						samples[index] += scratch[index] * this.getWidthSquared(column, index + startY);
					}
				}
				finally {
					Grid.reclaimScratchArray(scratch);
				}
			}
		}

		public double getWidth(OverworldColumn column, double y) {
			return this.width.evaluate(column, y);
		}

		public double getWidthSquared(OverworldColumn column, double y) {
			double width = this.width.evaluate(column, y);
			return width > 0.0D ? width * width : 0.0D;
		}

		@Override
		public double getWeight() {
			return this.weight;
		}
	}

	public static record CaveSurfaceBlocks(
		@VerifyNormal BlockState surface,
		@VerifyNormal BlockState subsurface
	) {}
}