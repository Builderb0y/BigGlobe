package builderb0y.bigglobe.columns.scripted.types;

import net.minecraft.registry.RegistryKeys;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class BiomeColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return BiomeEntry.TYPE;
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		if (object == null) return ldc(null, this.getTypeInfo());
		String string = (String)(object);
		//create the entry early so that if it doesn't exist, the world will fail to load.
		context.registry.registries.getRegistry(RegistryKeys.BIOME).requireByName(string);
		return BiomeEntry.CONSTANT_FACTORY.createConstant(constant(string), context.registry.constantFlags());
	}

	@Override
	public String toString() {
		return "biome";
	}
}