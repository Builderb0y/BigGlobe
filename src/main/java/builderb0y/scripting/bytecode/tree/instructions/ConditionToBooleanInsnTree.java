package builderb0y.scripting.bytecode.tree.instructions;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConditionToBooleanInsnTree implements InsnTree {

	public ConditionTree condition;
	/**
	most common case: ifTrue will be ICONST_1 and ifFalse will be ICONST_0.
	second most common case: ifTrue will be GETSTATIC Boolean.TRUE and ifFalse will be GETSTATIC Boolean.FALSE.
	*/
	public InsnTree ifTrue, ifFalse;

	public ConditionToBooleanInsnTree(ConditionTree condition) {
		this(condition, ldc(true), ldc(false));
	}

	public ConditionToBooleanInsnTree(ConditionTree condition, InsnTree ifTrue, InsnTree ifFalse) {
		this.condition = condition;
		this.ifTrue = ifTrue;
		this.ifFalse = ifFalse;
	}

	public static InsnTree create(ConditionTree condition) {
		if (condition instanceof BooleanToConditionTree converter) {
			return converter.condition;
		}
		else if (condition instanceof ConstantConditionTree constant) {
			return ldc(constant.value);
		}
		else {
			return new ConditionToBooleanInsnTree(condition);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label zero = label(), end = label();
		this.condition.emitBytecode(method, null, zero);
		this.ifTrue.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, end);
		method.node.visitLabel(zero);
		this.ifFalse.emitBytecode(method);
		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.ifTrue.getTypeInfo();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree ifTrue = this.ifTrue.cast(parser, type, mode);
		if (ifTrue == null) return null;
		InsnTree ifFalse = this.ifFalse.cast(parser, type, mode);
		if (ifFalse == null) return null;
		return new ConditionToBooleanInsnTree(this.condition, ifTrue, ifFalse);
	}
}