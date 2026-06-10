package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import org.jetbrains.annotations.UnknownNullability;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import builderb0y.bigglobe.classes.spec.BuiltinType;
import builderb0y.bigglobe.scripting.wrappers.tags.ConfiguredFeatureTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class ConfiguredFeatureEntry extends EntryWrapper<ConfiguredFeature<?, ?>, ConfiguredFeatureTag> {

	public static final TypeInfo TYPE = TypeInfo.of(ConfiguredFeatureEntry.class);
	public static final @UnknownNullability ConfiguredFeature<?, ?> EMPTY = BuiltinType.Export.EXPORTING ? null : new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public ConfiguredFeatureEntry(Holder<ConfiguredFeature<?, ?>> entry) {
		super(entry);
	}

	public static ConfiguredFeatureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static ConfiguredFeatureEntry of(String id, int flags) {
		Holder<ConfiguredFeature<?, ?>> entry = ConstantFactory.getEntryServerOnly(Registries.CONFIGURED_FEATURE, id, flags, EMPTY);
		return entry != null ? new ConfiguredFeatureEntry(entry) : null;
	}

	@Override
	public boolean isIn(ConfiguredFeatureTag entries) {
		return super.isIn(entries);
	}
}