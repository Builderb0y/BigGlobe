package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.scripting.wrappers.entries.ConfiguredFeatureEntry;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
#endif

public class ConfiguredFeatureTag extends TagWrapper<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif, ConfiguredFeatureEntry> {

	public static final TypeInfo TYPE = type(ConfiguredFeatureTag.class);
	public static final TagParser PARSER = new TagParser("ConfiguredFeatureTag", ConfiguredFeatureTag.class, "ConfiguredFeature", MethodInfo.findMethod(ConfiguredFeatureEntry.class, "isIn", boolean.class, ConfiguredFeatureTag.class));

	public ConfiguredFeatureTag(DelayedEntryList<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> list) {
		super(list);
	}

	public static ConfiguredFeatureTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static ConfiguredFeatureTag of(int flags, String... ids) {
		return new ConfiguredFeatureTag(DelayedEntryList.emptyOnClient(Registries.#if MC_VERSION >= MC_26_3_0 FEATURE #else CONFIGURED_FEATURE #endif, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public ConfiguredFeatureEntry wrap(Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> entry) {
		return new ConfiguredFeatureEntry(entry);
	}

	@Override
	public Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> unwrap(ConfiguredFeatureEntry entry) {
		return entry.entry;
	}

	@Override
	public boolean contains(ConfiguredFeatureEntry entry) {
		return super.contains(entry);
	}

	@Override
	public ConfiguredFeatureEntry random(RandomGenerator random) {
		return super.random(random);
	}

	@Override
	public ConfiguredFeatureEntry random(long seed) {
		return super.random(seed);
	}
}