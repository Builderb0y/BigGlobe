package builderb0y.bigglobe.columns.scripted.types;

import net.minecraft.block.Block;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class BlockColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return type(Block.class);
	}

	@Override
	public String toString() {
		return "block";
	}
}