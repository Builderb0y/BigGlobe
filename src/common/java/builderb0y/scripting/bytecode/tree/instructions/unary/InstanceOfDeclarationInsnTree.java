package builderb0y.scripting.bytecode.tree.instructions.unary;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class InstanceOfDeclarationInsnTree extends UnaryInsnTree {

	public final TypeInfo type;
	public final String name;

	public InstanceOfDeclarationInsnTree(InsnTree operand, TypeInfo type, String name) {
		super(operand);
		this.type = type;
		this.name = name;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		LazyVarInfo info = method.scopes.addVariable(this.name, this.type);
		this.operand.emitBytecode(method);
		method.node.visitInsn(DUP); //object object
		method.node.visitTypeInsn(INSTANCEOF, this.type.getInternalName()); //object boolean
		method.node.visitInsn(DUP_X1); //boolean object boolean
		Label nope = label(), end = label();
		method.node.visitJumpInsn(IFEQ, nope); //boolean object
		method.node.visitTypeInsn(CHECKCAST, this.type.getInternalName()); //boolean object
		method.node.visitJumpInsn(GOTO, end);
		method.node.visitLabel(nope); //boolean object
		method.node.visitInsn(POP); //boolean
		method.node.visitInsn(ACONST_NULL); //boolean null
		method.node.visitLabel(end); //boolean object
		info.emitStore(method); //boolean
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.BOOLEAN;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}