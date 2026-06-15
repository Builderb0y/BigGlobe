package builderb0y.bigglobe.classes;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

public abstract class BorderedValue {

	public static final TypeInfo
		TYPE = TypeInfo.of(BorderedValue.class);
	public static final FieldInfo
		BORDER = FieldInfo.inCaller("border");
	public static final MethodInfo
		GET_VALUE    = MethodInfo.inCaller("getValue"),
		SET_VALUE    = MethodInfo.inCaller("setValue"),
		APPLY_BORDER = MethodInfo.inCaller("applyBorder"),
		APPLY_VALUE  = MethodInfo.inCaller("applyValue");

	public double border = 1.0D;

	//synthetic subclasses have:
	//	public T value;
	//
	//	public non-abstract Object getValue() {
	//		return this.value;
	//	}
	//
	//	public non-abstract void setValue(Object value) {
	//		this.value = (T)(value);
	//	}

	public abstract Object getValue();

	public abstract void setValue(Object object);

	public boolean applyBorder(double border) {
		this.border *= border;
		return border > 0.0D;
	}

	public void applyValue(Object value) {
		this.setValue(value);
		this.border = Math.abs(this.border);
	}

	@Override
	public String toString() {
		return this.getValue() + " @ " + this.border;
	}
}