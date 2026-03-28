package builderb0y.bigglobe.columns.scripted.types;

import builderb0y.autocodec.data.Data;
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

	public static <T> T requireNonNull(T value, Data data, String type) {
		if (value != null) return value;
		else throw new IllegalStateException("Not a " + type + ": " + data);
	}

	@Override
	public InsnTree createConstant(Data data, ColumnCompileContext context) {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		return switch (this.getTypeInfo().getSort()) {
			case VOID -> throw new IllegalStateException("Attempting to create void constant");
			case BOOLEAN -> ldc(requireNonNull(data.tryAsBoolean(), data, "boolean").value);
			case BYTE -> ldc(requireNonNull(data.tryAsNumber(), data, "byte").byteValue());
			case CHAR -> ldc(requireNonNull(data.tryAsString(), data, "char").value.charAt(0));
			case SHORT -> ldc(requireNonNull(data.tryAsNumber(), data, "short").shortValue());
			case INT -> ldc(requireNonNull(data.tryAsNumber(), data, "int").intValue());
			case LONG -> ldc(requireNonNull(data.tryAsNumber(), data, "long").longValue());
			case FLOAT -> ldc(requireNonNull(data.tryAsNumber(), data, "float").floatValue());
			case DOUBLE -> ldc(requireNonNull(data.tryAsNumber(), data, "double").doubleValue());
			case OBJECT, ARRAY -> throw new IllegalStateException("Sub-classes must implement handling for type " + this.getTypeInfo());
		};
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