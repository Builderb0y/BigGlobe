package builderb0y.scripting.bytecode.tree.instructions;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableReceiverMapperInsnTree implements InsnTree {

	public InsnTree object, mapper; //mapper should get from stack.

	public NullableReceiverMapperInsnTree(InsnTree object, InsnTree mapper) {
		this.object = object;
		this.mapper = mapper;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label apply = label(), end = label();

		this.object.emitBytecode(method);
		method.node.visitInsn(this.object.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		ElvisInsnTree.jumpIfNonNull(method, this.object.getTypeInfo(), apply);
		method.node.visitInsn(this.object.getTypeInfo().isDoubleWidth() ? POP2 : POP);
		if (this.getTypeInfo().isValue()) {
			constantAbsent(this.getTypeInfo()).emitBytecode(method);
		}
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(apply);
		method.node.visitInsn(this.object.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		this.mapper.emitBytecode(method);
		switch (this.mapper.getTypeInfo().getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.object.getTypeInfo();
	}
}