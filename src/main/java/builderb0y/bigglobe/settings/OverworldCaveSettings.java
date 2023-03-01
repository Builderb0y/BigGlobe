package builderb0y.bigglobe.settings;

import java.util.List;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.randomLists.IWeightedListElement;

@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
public record OverworldCaveSettings(
	VoronoiDiagram2D placement,
	VariationsList<LocalOverworldCaveSettings> templates,
	@Hidden int maxDepth
) {

	public OverworldCaveSettings(VoronoiDiagram2D placement, VariationsList<LocalOverworldCaveSettings> templates) {
		this(placement, templates, templates.elements.stream().mapToInt(LocalOverworldCaveSettings::depth).max().orElse(0));
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, OverworldCaveSettings> context) throws VerifyException {
		OverworldCaveSettings settings = context.object;
		if (settings != null) {
			List<LocalOverworldCaveSettings> elements = settings.templates.elements;
			double limit = settings.placement.distance * 0.5D;
			for (int index = 0, size = elements.size(); index < size; index++) {
				LocalOverworldCaveSettings template = elements.get(index);
				if (template.lower_width > limit) {
					throw new VerifyException(context.pathToStringBuilder().append(".templates[").append(index).append("].lower_limit must be at most half of placement.distance.").toString());
				}
				if (template.upper_width > limit) {
					throw new VerifyException(context.pathToStringBuilder().append(".templates[").append(index).append("].upper_limit must be at most half of placement.distance.").toString());
				}
			}
		}
	}

	@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
	public static record LocalOverworldCaveSettings(
		double weight,
		Grid3D noise,
		@VerifyNullable Grid3D ledge_noise,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double upper_width,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double lower_width,
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
					* this.getWidthSquared(column.getFinalTopHeightD(), y)
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
				double height = column.getFinalTopHeightD();
				double[] scratch = Grid.getScratchArray(depth);
				try {
					this.ledge_noise.getBulkY(column.getCaveSeed(), column.x, startY, column.z, scratch, depth);
					for (int index = 0; index < depth; index++) {
						samples[index] += scratch[index] * this.getWidthSquared(height, index + startY);
					}
				}
				finally {
					Grid.reclaimScratchArray(scratch);
				}
			}
		}

		public double getWidth(double height, double y) {
			double inverseFraction = (height - y) / this.depth;
			return Interpolator.mixLinear(this.upper_width, this.lower_width, inverseFraction);
		}

		public double getWidthSquared(double height, double y) {
			double width = this.getWidth(height, y);
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