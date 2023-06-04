package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record ConfiguredFeatureTagKey(TagKey<ConfiguredFeature<?, ?>> key) implements TagWrapper<ConfiguredFeature<?, ?>, ConfiguredFeatureEntry> {

	public static final TypeInfo TYPE = type(ConfiguredFeatureTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static ConfiguredFeatureTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static ConfiguredFeatureTagKey of(String id) {
		return new ConfiguredFeatureTagKey(TagKey.of(Registry.CONFIGURED_FEATURE_KEY, new Identifier(id)));
	}

	@Override
	public ConfiguredFeatureEntry wrap(RegistryEntry<ConfiguredFeature<?, ?>> entry) {
		return new ConfiguredFeatureEntry(entry);
	}

	@Override
	public ConfiguredFeatureEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}
}