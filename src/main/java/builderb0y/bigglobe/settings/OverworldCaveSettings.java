package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryWrapper;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.versions.AutoCodecVersions;

public class OverworldCaveSettings {

	public final VoronoiDiagram2D placement;
	public final RegistryWrapper<LocalOverworldCaveSettings> template_registry;
	public final transient IRandomList<LocalOverworldCaveSettings> templates;
	public final transient int maxDepth;

	public OverworldCaveSettings(VoronoiDiagram2D placement, RegistryWrapper<LocalOverworldCaveSettings> template_registry) {
		this.placement = placement;
		this.template_registry = template_registry;
		this.templates = BigGlobeDynamicRegistries.sortAndCollect(template_registry);
		this.maxDepth = this.templates.stream().mapToInt(LocalOverworldCaveSettings::depth).max().orElse(0);
	}

	@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
	public static record LocalOverworldCaveSettings(
		double weight,
		@VerifyIntRange(min = 0, minInclusive = false) int depth,
		Grid3D noise,
		ColumnYToDoubleScript.Holder noise_threshold,
		ColumnYToDoubleScript.Holder effective_width,
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
					throw AutoCodecVersions.newVerifyException(() -> "Must specify " + context.pathToString() + " when floor_blocks or ceiling_blocks are present.");
				}
			}
		}

		public double getValue(OverworldColumn column, int y) {
			return this.noise.getValue(column.getCaveSeed(), column.x, y, column.z);
		}

		public void getBulkY(OverworldColumn column) {
			int depth = this.depth;
			double[] samples = column.caveNoise;
			if (samples == null) samples = column.caveNoise = new double[depth];
			int startY = column.getFinalTopHeightI() - depth;
			this.noise.getBulkY(column.getCaveSeed(), column.x, startY, column.z, samples, depth);
		}

		public double getNoiseThreshold(OverworldColumn column, double y) {
			return this.noise_threshold.evaluate(column, y);
		}

		public double getEffectiveWidth(OverworldColumn column, double y) {
			return this.effective_width.evaluate(column, y);
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