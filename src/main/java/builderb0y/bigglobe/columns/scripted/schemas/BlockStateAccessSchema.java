package builderb0y.bigglobe.columns.scripted.schemas;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.codecs.BlockStateCoder.BlockProperties;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockStateAccessSchema extends AbstractAccessSchema {

	public static final TypeInfo BLOCK_STATE_TYPE = type(BlockState.class);

	public BlockStateAccessSchema(boolean is_3d) {
		super(is_3d, BLOCK_STATE_TYPE);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		BetterRegistry<Block> registry = context.registry.registries.getRegistry(RegistryKeyVersions.block());
		BlockProperties blockProperties = BlockStateCoder.decodeState(registry, (String)(object));
		Set<Property<?>> missing = blockProperties.missing();
		if (!missing.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Missing properties for block " + blockProperties.id() + ": " + missing);
		}
		return ldc(blockProperties.state(), BLOCK_STATE_TYPE);
	}
}