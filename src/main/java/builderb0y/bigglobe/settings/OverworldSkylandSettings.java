package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYRandomToDoubleScript;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public record OverworldSkylandSettings(
	VoronoiDiagram2D placement,
	VariationsList<LocalSkylandSettings> templates
) {

	public static record LocalSkylandSettings(
		@DefaultDouble(1.0D) double weight,
		@VerifyFloatRange(min = 0.0D) double padding,
		RandomSource average_center,
		Grid2D center,
		Grid2D thickness,
		@VerifyNullable Grid2D auxiliary_noise,
		ColumnYToDoubleScript.Holder min_y,
		ColumnYToDoubleScript.Holder max_y,
		SkylandSurfaceSettings surface,
		@VerifyNullable SortedFeatureTag floor_decorator,
		@VerifyNullable SortedFeatureTag ceiling_decorator
	)
	implements IWeightedListElement {

		@Override
		public double getWeight() {
			return this.weight;
		}
	}

	public static record SkylandSurfaceSettings(
		BlockState top_state,
		BlockState under_state,
		ColumnYRandomToDoubleScript.Holder depth
	) {}
}