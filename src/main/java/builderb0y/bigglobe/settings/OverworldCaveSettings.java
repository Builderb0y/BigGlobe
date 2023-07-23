package builderb0y.bigglobe.settings;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

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
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.AutoCodecVersions;

public class OverworldCaveSettings extends DecoratorTagHolder {

	public final VoronoiDiagram2D placement;
	public final RegistryWrapper<LocalOverworldCaveSettings> template_registry;
	public final transient IRandomList<RegistryEntry<LocalOverworldCaveSettings>> templates;
	public final transient int maxDepth;

	public OverworldCaveSettings(
		VoronoiDiagram2D placement,
		RegistryWrapper<LocalOverworldCaveSettings> template_registry,
		RegistryEntryLookup<ConfiguredFeature<?, ?>> configured_feature_lookup
	) {
		super(configured_feature_lookup);
		this.placement = placement;
		this.template_registry = template_registry;
		this.templates = BigGlobeDynamicRegistries.sortAndCollect(template_registry);
		this.maxDepth = this.templates.stream().mapToInt((RegistryEntry<LocalOverworldCaveSettings> settings) -> settings.value().depth).max().orElse(0);
		template_registry.streamEntries().sequential().forEach((RegistryEntry<LocalOverworldCaveSettings> entry) -> {
			Identifier baseKey = UnregisteredObjectException.getKey(entry).getValue();
			LocalOverworldCaveSettings settings = entry.value();
			settings.floor_decorator   = this.createDecoratorTag(baseKey, "floor");
			settings.ceiling_decorator = this.createDecoratorTag(baseKey, "ceiling");
		});
	}

	@Override
	public String getDecoratorTagPrefix() {
		return "overworld/caves";
	}

	@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
	public static class LocalOverworldCaveSettings implements IWeightedListElement {

		public final double weight;
		public final @VerifyIntRange(min = 0, minInclusive = false) int depth;
		public final Grid3D noise;
		public final ColumnYToDoubleScript.Holder noise_threshold;
		public final ColumnYToDoubleScript.Holder effective_width;
		public final @VerifyNullable Grid2D surface_depth_noise;
		public final @VerifyNullable CaveSurfaceBlocks floor_blocks;
		public final @VerifyNullable CaveSurfaceBlocks ceiling_blocks;
		public transient SortedFeatureTag floor_decorator, ceiling_decorator;

		public LocalOverworldCaveSettings(
			double weight,
			@VerifyIntRange(min = 0, minInclusive = false) int depth,
			Grid3D noise,
			ColumnYToDoubleScript.Holder noise_threshold,
			ColumnYToDoubleScript.Holder effective_width,
			@VerifyNullable Grid2D surface_depth_noise,
			@VerifyNullable CaveSurfaceBlocks floor_blocks,
			@VerifyNullable CaveSurfaceBlocks ceiling_blocks
		) {
			this.weight = weight;
			this.depth = depth;
			this.noise = noise;
			this.noise_threshold = noise_threshold;
			this.effective_width = effective_width;
			this.surface_depth_noise = surface_depth_noise;
			this.floor_blocks = floor_blocks;
			this.ceiling_blocks = ceiling_blocks;
		}

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