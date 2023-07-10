package builderb0y.bigglobe.features;

import java.util.Comparator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@Wrapper
public class SortedFeatureTag {

	public static final ObjectArrayFactory<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE_ARRAY_FACTORY = new ObjectArrayFactory<>(ConfiguredFeature.class).generic();

	public final RegistryEntryList<ConfiguredFeature<?, ?>> list;
	public transient ConfiguredFeature<?, ?>[] sortedFeatures;

	public SortedFeatureTag(RegistryEntryList<ConfiguredFeature<?, ?>> list) {
		this.list = list;
	}

	public ConfiguredFeature<?, ?>[] getSortedFeatures() {
		if (this.sortedFeatures == null) {
			this.sortedFeatures = (
				this
				.list
				.stream()
				.sorted(
					Comparator.comparing(
						UnregisteredObjectException::getID,
						Comparator
						.comparing(Identifier::getNamespace)
						.thenComparing(Identifier::getPath)
					)
				)
				.map(RegistryEntry::value)
				.toArray(CONFIGURED_FEATURE_ARRAY_FACTORY)
			);
		}
		return this.sortedFeatures;
	}
}