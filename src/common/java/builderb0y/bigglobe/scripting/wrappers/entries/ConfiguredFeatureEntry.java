package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import org.jetbrains.annotations.UnknownNullability;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.NoOpFeature;

import builderb0y.bigglobe.classes.spec.BuiltinType;
import builderb0y.bigglobe.scripting.wrappers.tags.ConfiguredFeatureTag;
import builderb0y.bigglobe.scripting.wrappers.tags.TagWrapper;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
	import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
#endif

public class ConfiguredFeatureEntry extends EntryWrapper<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif, ConfiguredFeatureTag> {

	public static final TypeInfo TYPE = TypeInfo.of(ConfiguredFeatureEntry.class);
	public static final @UnknownNullability #if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif EMPTY = (
		BuiltinType.CODER != null ?
		#if MC_VERSION >= MC_26_3_0
			new NoOpFeature()
		#else
			new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE)
		#endif
		: null
	);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public ConfiguredFeatureEntry(Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> entry) {
		super(entry);
	}

	public static ConfiguredFeatureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static ConfiguredFeatureEntry of(String id, int flags) {
		Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> entry = ConstantFactory.getEntryServerOnly(Registries.#if MC_VERSION >= MC_26_3_0 FEATURE #else CONFIGURED_FEATURE #endif, id, flags, EMPTY);
		return entry != null ? new ConfiguredFeatureEntry(entry) : null;
	}

	@Override
	public boolean isIn(ConfiguredFeatureTag entries) {
		return super.isIn(entries);
	}
}