package builderb0y.bigglobe.columns.scripted.types;

import com.mojang.datafixers.util.Unit;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.scripting.wrappers.ConfiguredFeatureEntry;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class ConfiguredFeatureColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return type(ConfiguredFeatureEntry.class);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		if (object == Unit.INSTANCE) return ldc(null, this.getTypeInfo());
		String string = (String)(object);
		Identifier identifier = IdentifierVersions.create(string);
		RegistryKey<ConfiguredFeature<?, ?>> key = RegistryKey.of(RegistryKeyVersions.configuredFeature(), identifier);
		RegistryEntry<ConfiguredFeature<?, ?>> entry = context.registry.registries.getRegistry(RegistryKeyVersions.configuredFeature()).getOrCreateEntry(key);
		return ldc(new ConfiguredFeatureEntry(entry), this.getTypeInfo());
	}

	@Override
	public String toString() {
		return "configured_feature";
	}
}