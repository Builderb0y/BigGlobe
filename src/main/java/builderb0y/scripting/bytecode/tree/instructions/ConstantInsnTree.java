package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantInsnTree implements InsnTree {

	public ConstantValue value;

	public ConstantInsnTree(ConstantValue value) {
		if (!value.isConstantOrDynamic()) {
			throw new IllegalArgumentException("Non-constant constant value: " + value);
		}
		this.value = value;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.value.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.value.getTypeInfo();
	}

	@Override
	public ConstantValue getConstantValue() {
		return this.value;
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return nextStatement;
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		if (this.value.isConstant() && this.value.asJavaObject() == null && type.isObject()) {
			return ldc(null, type);
		}
		TypeInfo from = this.getTypeInfo();
		if (from.isPrimitiveValue() && type.isPrimitive()) {
			if (type.isVoid()) return noop;
			if (mode.implicit) {
				if (type.getSort() == Sort.BOOLEAN) {
					if (from.getSort() != Sort.BOOLEAN) {
						return mode.handleFailure(from, type);
					}
				}
				else if (type.isNumber()) {
					if (!from.isNumber() || type.getSort().ordinal() < from.getSort().ordinal()) {
						return mode.handleFailure(from, type);
					}
				}
			}
			return switch (type.getSort()) {
				case BYTE    -> ldc(this.value.asByte   ());
				case SHORT   -> ldc(this.value.asShort  ());
				case INT     -> ldc(this.value.asInt    ());
				case LONG    -> ldc(this.value.asLong   ());
				case FLOAT   -> ldc(this.value.asFloat  ());
				case DOUBLE  -> ldc(this.value.asDouble ());
				case CHAR    -> ldc(this.value.asChar   ());
				case BOOLEAN -> ldc(this.value.asBoolean());
				default      -> throw new AssertionError(type);
			};
		}
		return InsnTree.super.doCast(parser, type, mode);
	}
}