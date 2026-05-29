package builderb0y.bigglobe.columns.scripted.types;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.StringData;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.columns.scripted2.compile.ColumnCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;

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
		return ldc(
			BlockStateCoder
				.decodeStateWithMissingErrors(
					context.registry.registries.getRegistry(Registries.BLOCK),
					stringData.value
				)
				.unwrapEager(
					BigGlobeMod.LOGGER::warn,
					IllegalArgumentException::new
				)
				.state(),
			type(BlockState.class)
		);
	}

	@Override
	public String toString() {
		return "block_state";
	}
}