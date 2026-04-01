package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ReceiverMapperInsnTree implements InsnTree {

	public InsnTree receiver, mapper; //mapper should get from stack.

	public ReceiverMapperInsnTree(InsnTree receiver, InsnTree mapper) {
		if (receiver.getTypeInfo().isVoid()) {
			throw new IllegalArgumentException("Void receiver");
		}
		this.receiver = receiver;
		this.mapper = mapper;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.receiver.emitBytecode(method);
		method.node.visitInsn(this.receiver.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		this.mapper.emitBytecode(method);
		switch (this.mapper.getTypeInfo().getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.receiver.getTypeInfo();
	}
}