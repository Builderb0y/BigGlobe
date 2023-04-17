package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record ConfiguredFeatureEntry(RegistryEntry<ConfiguredFeature<?, ?>> entry) {

	public static final TypeInfo TYPE = type(ConfiguredFeatureEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static ConfiguredFeatureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static ConfiguredFeatureEntry of(String id) {
		return new ConfiguredFeatureEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(RegistryKeys.CONFIGURED_FEATURE)
			.entryOf(RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(id)))
		);
	}

	public boolean isIn(ConfiguredFeatureTagKey tag) {
		return this.entry.isIn(tag.key());
	}

	@Override
	public int hashCode() {
		return this.entry.getKey().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ConfiguredFeatureEntry that &&
			this.entry.getKey().equals(that.entry.getKey())
		);
	}
}