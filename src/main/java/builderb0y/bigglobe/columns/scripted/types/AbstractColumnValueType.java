package builderb0y.bigglobe.columns.scripted.types;

import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class AbstractColumnValueType implements ColumnValueType {

	public abstract TypeInfo getTypeInfo();

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		return new TypeContext(this.getTypeInfo(), null);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		return ldc(object, this.getTypeInfo());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int hashCode() {
		return this.getTypeInfo().hashCode() ^ this.getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj != null &&
			obj.getClass() == this.getClass() &&
			((AbstractColumnValueType)(obj)).getTypeInfo().equals(this.getTypeInfo())
		);
	}
}