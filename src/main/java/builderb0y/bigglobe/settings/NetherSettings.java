package builderb0y.bigglobe.settings;

import java.util.Comparator;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.randomLists.ContainedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYRandomToDoubleScript;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@UseVerifier(name = "verify", usage = MemberUsage.METHOD_IS_HANDLER)
public class NetherSettings {
	public final VoronoiDiagram2D biome_placement;
	public final RegistryWrapper<LocalNetherSettings> local_settings_registry;
	public final transient IRandomList<LocalNetherSettings> local_settings;
	public final @VerifyDivisibleBy16 int min_y;
	public final @VerifyDivisibleBy16 int max_y;

	public NetherSettings(
		VoronoiDiagram2D biome_placement,
		RegistryWrapper<LocalNetherSettings> local_settings_registry,
		int min_y,
		int max_y
	) {
		this.biome_placement = biome_placement;
		this.local_settings_registry = local_settings_registry;
		this.local_settings = new ContainedRandomList<>();
		local_settings_registry
		.streamEntries()
		.sorted(
			Comparator.comparing(
				(RegistryEntry<LocalNetherSettings> entry) -> (
					UnregisteredObjectException.getKey(entry).getValue()
				),
				Comparator
				.comparing(Identifier::getNamespace)
				.thenComparing(Identifier::getPath)
			)
		)
		.map(RegistryEntry::value)
		.forEachOrdered(this.local_settings::add);
		this.min_y = min_y;
		this.max_y = max_y;
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, NetherSettings> context) throws VerifyException {
		NetherSettings settings = context.object;
		if (settings != null && settings.local_settings.isEmpty()) {
			throw new VerifyException("worldgen/bigglobe_nether_biome registry is empty!");
		}
	}

	public int height() {
		return this.max_y - this.min_y;
	}

	public static record LocalNetherSettings(
		double weight,
		RegistryEntry<Biome> biome,
		NetherCavernSettings caverns,
		NetherCaveSettings caves,
		BlockState fluid_state,
		RandomSource fluid_level,
		SortedFeatureTag fluid_decorator,
		@VerifyNormal BlockState filler
	)
	implements IWeightedListElement {

		@Override
		public double getWeight() {
			return this.weight;
		}
	}

	public static record NetherCavernSettings(
		int min_y,
		int max_y,
		int lower_padding,
		int upper_padding,
		int edge_padding,
		Grid3D noise,
		@VerifyNullable NetherSurfaceSettings floor_surface,
		@VerifyNullable NetherSurfaceSettings ceiling_surface,
		SortedFeatureTag floor_decorator,
		SortedFeatureTag ceiling_decorator
	) {}

	public static record NetherCaveSettings(
		Grid3D noise,
		ColumnYToDoubleScript.Holder width,
		@VerifyNullable Integer lower_padding,
		@VerifyNullable NetherSurfaceSettings floor_surface,
		@VerifyNullable NetherSurfaceSettings ceiling_surface,
		SortedFeatureTag floor_decorator,
		SortedFeatureTag ceiling_decorator
	) {}

	public static record NetherSurfaceSettings(
		BlockState top_state,
		BlockState under_state,
		ColumnYRandomToDoubleScript.Holder depth
	) {}
}