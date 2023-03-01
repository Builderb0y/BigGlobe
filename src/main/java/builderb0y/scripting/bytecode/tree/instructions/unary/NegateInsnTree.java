package builderb0y.scripting.bytecode.tree.instructions.unary;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NegateInsnTree extends UnaryInsnTree {

	public NegateInsnTree(InsnTree value) {
		super(value);
	}

	public static InsnTree create(InsnTree value) {
		TypeInfo type = TypeInfos.widenToInt(value.getTypeInfo());
		ConstantValue constant = value.getConstantValue();
		if (constant.isConstant()) {
			return switch (type.getSort()) {
				case INT    -> ldc(-constant.   asInt());
				case LONG   -> ldc(-constant.  asLong());
				case FLOAT  -> ldc(-constant. asFloat());
				case DOUBLE -> ldc(-constant.asDouble());
				default -> throw new InvalidOperandException("Can't negate non-number.");
			};
		}
		else {
			if (!value.getTypeInfo().isNumber()) {
				throw new InvalidOperandException("Can't negate non-number.");
			}
			return new NegateInsnTree(value);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		method.node.visitInsn(this.operand.getTypeInfo().getOpcode(INEG));
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.widenToInt(this.operand.getTypeInfo());
	}
}