package builderb0y.bigglobe.columns.scripted.types;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.TypeInfos;

@RecordLike({})
public class DoubleColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.DOUBLE;
	}

	@Override
	public String toString() {
		return "double";
	}
}