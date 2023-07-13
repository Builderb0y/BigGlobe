package builderb0y.bigglobe.settings;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public abstract class DecoratorTagHolder {

	public final RegistryEntryLookup<ConfiguredFeature<?, ?>> configured_feature_lookup;

	public DecoratorTagHolder(RegistryEntryLookup<ConfiguredFeature<?, ?>> configured_feature_lookup) {
		this.configured_feature_lookup = configured_feature_lookup;
	}

	public abstract String getDecoratorTagPrefix();

	public SortedFeatureTag createDecoratorTag(Identifier baseKey, String type) {
		return new SortedFeatureTag(
			this.configured_feature_lookup.getOrThrow(
				TagKey.of(
					RegistryKeyVersions.configuredFeature(),
					new Identifier(
						baseKey.getNamespace(),
						this.getDecoratorTagPrefix() + '/' + baseKey.getPath() + '/' + type
					)
				)
			)
		);
	}
}