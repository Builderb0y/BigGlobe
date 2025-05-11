package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.bigglobe.scripting.wrappers.tags.ConfiguredFeatureTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class ConfiguredFeatureEntry extends EntryWrapper<ConfiguredFeature<?, ?>, ConfiguredFeatureTag> {

	public static final TypeInfo TYPE = TypeInfo.of(ConfiguredFeatureEntry.class);
	public static final ConfiguredFeature<?, ?> EMPTY = new ConfiguredFeature<>(Feature.NO_OP, DefaultFeatureConfig.INSTANCE);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public ConfiguredFeatureEntry(RegistryEntry<ConfiguredFeature<?, ?>> entry) {
		super(entry);
	}

	public static ConfiguredFeatureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static ConfiguredFeatureEntry of(String id, int flags) {
		RegistryEntry<ConfiguredFeature<?, ?>> entry = ConstantFactory.getEntryServerOnly(RegistryKeys.CONFIGURED_FEATURE, id, flags, EMPTY);
		return entry != null ? new ConfiguredFeatureEntry(entry) : null;
	}

	@Override
	public boolean isIn(ConfiguredFeatureTag entries) {
		return super.isIn(entries);
	}
}