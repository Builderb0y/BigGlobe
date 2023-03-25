package builderb0y.bigglobe.features;

import java.util.Comparator;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.RegistryWorldView;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@Wrapper
public class SortedFeatureTag {

	public static final ObjectArrayFactory<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE_ARRAY_FACTORY = new ObjectArrayFactory<>(ConfiguredFeature.class).generic();

	public final TagKey<ConfiguredFeature<?, ?>> key;
	public transient ConfiguredFeature<?, ?>[] sortedFeatures;

	public SortedFeatureTag(TagKey<ConfiguredFeature<?, ?>> key) {
		this.key = key;
	}

	public ConfiguredFeature<?, ?>[] getSortedFeatures(DynamicRegistryManager registries) {
		if (this.sortedFeatures == null) {
			this.sortedFeatures = registries.get(this.key.registry()).getEntryList(this.key).map(list -> {
				return list.stream().sorted(Comparator.comparing(
					UnregisteredObjectException::getID,
					Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath)
				))
				.map(RegistryEntry::value)
				.toArray(CONFIGURED_FEATURE_ARRAY_FACTORY);
			})
			.orElseGet(() -> {
				BigGlobeMod.LOGGER.error("Unknown configured_feature tag: " + this.key);
				return CONFIGURED_FEATURE_ARRAY_FACTORY.empty();
			});
		}
		return this.sortedFeatures;
	}

	public ConfiguredFeature<?, ?>[] getSortedFeatures(MinecraftServer server) {
		return this.getSortedFeatures(server.getRegistryManager());
	}

	public ConfiguredFeature<?, ?>[] getSortedFeatures(RegistryWorldView world) {
		return this.getSortedFeatures(world.getRegistryManager());
	}
}