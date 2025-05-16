package builderb0y.bigglobe.columns.scripted.types;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.StringData;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.scripting.wrappers.entries.ConfiguredFeatureEntry;
import builderb0y.scripting.bytecode.ConstantFactory;
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
	public InsnTree createConstant(Data data, ColumnCompileContext context) {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		StringData stringData = data.tryAsString();
		if (stringData == null) throw new ClassCastException("Not a String: " + data);
		RegistryEntry<ConfiguredFeature<?, ?>> entry = ConstantFactory.getEntryServerOnly(
			RegistryKeys.CONFIGURED_FEATURE,
			stringData.value,
			context.registry.constantFlags(),
			ConfiguredFeatureEntry.EMPTY
		);
		return ldc(new ConfiguredFeatureEntry(entry), ConfiguredFeatureEntry.TYPE);
	}

	@Override
	public String toString() {
		return "configured_feature";
	}
}