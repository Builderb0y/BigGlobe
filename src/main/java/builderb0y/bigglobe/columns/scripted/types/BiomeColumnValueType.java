package builderb0y.bigglobe.columns.scripted.types;

import com.mojang.datafixers.util.Unit;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.scripting.wrappers.BiomeEntry;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
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
		if (object == Unit.INSTANCE) return ldc(null, this.getTypeInfo());
		String string = (String)(object);
		//create the entry early so that if it doesn't exist, the world will fail to load.
		context.registry.registries.getRegistry(RegistryKeyVersions.biome()).getOrCreateEntry(RegistryKey.of(RegistryKeyVersions.biome(), IdentifierVersions.create(string)));
		return BiomeEntry.CONSTANT_FACTORY.createConstant(constant(string));
	}

	@Override
	public String toString() {
		return "biome";
	}
}