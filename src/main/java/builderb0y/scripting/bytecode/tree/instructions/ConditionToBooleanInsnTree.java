package builderb0y.scripting.bytecode.tree.instructions;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.util.TypeInfos;

public class ConditionToBooleanInsnTree implements InsnTree {

	public ConditionTree condition;

	public ConditionToBooleanInsnTree(ConditionTree condition) {
		this.condition = condition;
	}

	public static InsnTree create(ConditionTree condition) {
		if (condition instanceof BooleanToConditionTree converter) {
			return converter.condition;
		}
		else if (condition instanceof ConstantConditionTree constant) {
			return InsnTrees.ldc(constant.value);
		}
		else {
			return new ConditionToBooleanInsnTree(condition);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label one = new Label(), end = new Label();
		this.condition.emitBytecode(method, one, null);
		method.node.visitInsn(ICONST_0);
		method.node.visitJumpInsn(GOTO, end);
		method.node.visitLabel(one);
		method.node.visitInsn(ICONST_1);
		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.BOOLEAN;
	}
}