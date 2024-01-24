package builderb0y.bigglobe.columns.scripted.schemas;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockAccessSchema extends AbstractAccessSchema {

	public static final TypeInfo BLOCK_TYPE = type(Block.class);

	public BlockAccessSchema(boolean is_3d) {
		super(is_3d, BLOCK_TYPE);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		Identifier identifier = new Identifier((String)(object));
		Block block = (
			context
			.registry
			.registries
			.getRegistry(RegistryKeyVersions.block())
			.getOrCreateEntry(RegistryKey.of(RegistryKeyVersions.block(), identifier))
			.value()
		);
		return ldc(block, BLOCK_TYPE);
	}
}