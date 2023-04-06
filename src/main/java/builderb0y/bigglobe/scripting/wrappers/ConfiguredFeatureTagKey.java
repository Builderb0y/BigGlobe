package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList.Named;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record ConfiguredFeatureTagKey(TagKey<ConfiguredFeature<?, ?>> key) implements TagWrapper<ConfiguredFeatureEntry> {

	public static final TypeInfo TYPE = type(ConfiguredFeatureTagKey.class);
	public static final MethodInfo
		RANDOM = method(ACC_PUBLIC, ConfiguredFeatureTagKey.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class),
		ITERATOR = method(ACC_PUBLIC, ConfiguredFeatureTagKey.class, "iterator", Iterator.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(ConfiguredFeatureTagKey.class, "of", String.class, ConfiguredFeatureTagKey.class);

	public static ConfiguredFeatureTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static ConfiguredFeatureTagKey of(String id) {
		return new ConfiguredFeatureTagKey(TagKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(id)));
	}

	@Override
	public ConfiguredFeatureEntry random(RandomGenerator random) {
		Optional<Named<ConfiguredFeature<?, ?>>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
		Optional<RegistryEntry<ConfiguredFeature<?, ?>>> feature = list.get().getRandom(new MojangPermuter(random.nextLong()));
		if (feature.isEmpty()) throw new RuntimeException("Biome tag is empty: " + this.key.id());
		return new ConfiguredFeatureEntry(feature.get());
	}

	@Override
	public Iterator<ConfiguredFeatureEntry> iterator() {
		Optional<Named<ConfiguredFeature<?, ?>>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
		return list.get().stream().map(ConfiguredFeatureEntry::new).iterator();
	}
}