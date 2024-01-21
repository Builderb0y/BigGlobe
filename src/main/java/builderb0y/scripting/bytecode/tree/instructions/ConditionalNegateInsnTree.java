package builderb0y.scripting.bytecode.tree.instructions;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConditionalNegateInsnTree implements InsnTree {

	public InsnTree value;
	public ConditionTree condition;
	public int opcode;

	public ConditionalNegateInsnTree(InsnTree value, ConditionTree condition, int opcode) {
		this.value = value;
		this.condition = condition;
		this.opcode = opcode;
	}

	public static InsnTree create(ExpressionParser parser, InsnTree value, ConditionTree condition) {
		value = value.cast(parser, TypeInfos.widenToInt(value.getTypeInfo()), CastMode.IMPLICIT_THROW);
		return new ConditionalNegateInsnTree(
			value,
			condition,
			switch (value.getTypeInfo().getSort()) {
				case INT -> INEG;
				case LONG -> LNEG;
				case FLOAT -> FNEG;
				case DOUBLE -> DNEG;
				default -> throw new AssertionError("Cast failed: " + value);
			}
		);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label done = label();
		this.value.emitBytecode(method);
		this.condition.emitBytecode(method, null, done);
		method.node.visitInsn(this.opcode);
		method.node.visitLabel(done);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.value.getTypeInfo();
	}
}