package builderb0y.bigglobe.columns.scripted.types;

import java.util.Set;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Property;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.StringData;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.codecs.BlockStateCoder.BlockProperties;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class BlockStateColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return type(BlockState.class);
	}

	@Override
	public InsnTree createConstant(Data data, ColumnCompileContext context) {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		StringData stringData = data.tryAsString();
		if (stringData == null) throw new ClassCastException("Not a String: " + data);
		BlockProperties blockProperties = BlockStateCoder.decodeState(context.registry.registries.getRegistry(RegistryKeys.BLOCK), stringData.value);
		Set<Property<?>> missing = blockProperties.missing();
		if (!missing.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Missing properties for " + stringData.value + ": " + missing);
		}
		return ldc(blockProperties.state(), type(BlockState.class));
	}

	@Override
	public String toString() {
		return "block_state";
	}
}