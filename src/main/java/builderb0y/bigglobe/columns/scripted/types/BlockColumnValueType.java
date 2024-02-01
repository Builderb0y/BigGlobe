package builderb0y.bigglobe.columns.scripted.types;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class BlockColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return type(Block.class);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		String string = (String)(object);
		Identifier identifier = new Identifier(string);
		RegistryKey<Block> key = RegistryKey.of(RegistryKeyVersions.block(), identifier);
		RegistryEntry<Block> blockEntry = context.registry.registries.getRegistry(RegistryKeyVersions.block()).getOrCreateEntry(key);
		return ldc(blockEntry.value(), type(Block.class));
	}

	@Override
	public String toString() {
		return "block";
	}
}