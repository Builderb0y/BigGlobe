package builderb0y.scripting.parsing;

import builderb0y.scripting.bytecode.TypeInfo;

public class InnerMethodExpressionParser extends ExpressionParser {

	public final TypeInfo returnType;

	public InnerMethodExpressionParser(ExpressionParser from, TypeInfo returnType) {
		super(from);
		this.returnType = returnType;
	}

	public InnerMethodExpressionParser(ExpressionParser from, String newInput, TypeInfo returnType) {
		super(from, newInput);
		this.returnType = returnType;
	}

	@Override
	public TypeInfo getMainReturnType() {
		return this.returnType;
	}
}