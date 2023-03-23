package builderb0y.scripting.bytecode.tree.instructions.casting;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.UnaryInsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class I2ZInsnTree extends UnaryInsnTree {

	public I2ZInsnTree(InsnTree operand) {
		super(operand);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		Label one = label();
		Label end = label();
		method.node.visitJumpInsn(Opcodes.IFNE, one);
		method.node.visitInsn(Opcodes.ICONST_0);
		method.node.visitJumpInsn(Opcodes.GOTO, end);
		method.node.visitLabel(one);
		method.node.visitInsn(Opcodes.ICONST_1);
		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.BOOLEAN;
	}
}