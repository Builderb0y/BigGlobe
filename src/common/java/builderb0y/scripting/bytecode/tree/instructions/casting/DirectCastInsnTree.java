package builderb0y.scripting.bytecode.tree.instructions.casting;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.UnaryInsnTree;

public class DirectCastInsnTree extends UnaryInsnTree {

	public TypeInfo type;
	public boolean nullable;

	public DirectCastInsnTree(InsnTree operand, TypeInfo type, boolean nullable) {
		super(operand);
		this.type = type;
		this.nullable = nullable;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		if (this.nullable) {
			Label notInstance = new Label(), end = new Label();
			method.node.visitInsn(DUP);
			method.node.visitTypeInsn(INSTANCEOF, this.type.getInternalName());
			method.node.visitJumpInsn(IFEQ, notInstance);
			method.node.visitTypeInsn(CHECKCAST, this.type.getInternalName());
			method.node.visitJumpInsn(GOTO, end);
			method.node.visitLabel(notInstance);
			method.node.visitInsn(POP);
			method.node.visitInsn(ACONST_NULL);
			method.node.visitLabel(end);
		}
		else {
			method.node.visitTypeInsn(CHECKCAST, this.type.getInternalName());
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}
}