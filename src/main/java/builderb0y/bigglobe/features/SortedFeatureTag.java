package builderb0y.bigglobe.features;

import java.util.Comparator;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@Wrapper
public class SortedFeatureTag {

	public static final ObjectArrayFactory<RegistryEntry<ConfiguredFeature<?, ?>>> CONFIGURED_FEATURE_REGISTRY_ENTRY_ARRAY_FACTORY = new ObjectArrayFactory<>(RegistryEntry.class).generic();

	public final RegistryEntryList<ConfiguredFeature<?, ?>> list;
	public transient RegistryEntry<ConfiguredFeature<?, ?>>[] sortedFeatures;

	public SortedFeatureTag(RegistryEntryList<ConfiguredFeature<?, ?>> list) {
		this.list = list;
	}

	public RegistryEntry<ConfiguredFeature<?, ?>>[] getSortedFeatures() {
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
				.toArray(CONFIGURED_FEATURE_REGISTRY_ENTRY_ARRAY_FACTORY)
			);
		}
		return this.sortedFeatures;
	}
}