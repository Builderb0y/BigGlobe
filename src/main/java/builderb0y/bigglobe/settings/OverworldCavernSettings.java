package builderb0y.bigglobe.settings;

import java.util.List;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;

@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
public record OverworldCavernSettings(
	VoronoiDiagram2D placement,
	VariationsList<LocalCavernSettings> templates
) {

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, OverworldCavernSettings> context) throws VerifyException {
		OverworldCavernSettings settings = context.object;
		if (settings != null) {
			List<LocalCavernSettings> elements = settings.templates.elements;
			for (int index = 0, size = elements.size(); index < size; index++) {
				LocalCavernSettings template = elements.get(index);
				if (template.padding > settings.placement.distance * 0.5D) {
					final int index_ = index;
					throw new VerifyException(() -> context.pathToStringBuilder().append(".templates[").append(index_).append("].padding must be at most half of placement.distance.").toString());
				}
			}
		}
	}

	public static record LocalCavernSettings(
		@DefaultDouble(1.0D) double weight,
		@VerifyFloatRange(min = 0.0D) double padding,
		RandomSource average_center,
		Grid2D center,
		Grid2D thickness,
		@Hidden double sqrtMaxThickness,
		@VerifyNullable BlockState fluid,
		@VerifyNullable SortedFeatureTag floor_decorator,
		@VerifyNullable SortedFeatureTag ceiling_decorator,
		@DefaultBoolean(false) boolean has_ancient_cities
	)
	implements IWeightedListElement {

		public LocalCavernSettings(
			double weight,
			double padding,
			RandomSource center,
			Grid2D center_variation,
			Grid2D thickness,
			@VerifyNullable BlockState fluids,
			@VerifyNullable SortedFeatureTag floor_decorator,
			@VerifyNullable SortedFeatureTag ceiling_decorator,
			boolean has_ancient_cities
		) {
			this(
				weight,
				padding,
				center,
				center_variation,
				thickness,
				Math.sqrt(thickness.maxValue()),
				fluids,
				floor_decorator,
				ceiling_decorator,
				has_ancient_cities
			);
		}

		@Override
		public double getWeight() {
			return this.weight;
		}
	}
}