package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InstanceGetterSetterUpdateInsnTree extends UpdateInsnTree {

	public InsnTree receiver;
	public int opcode;
	public MethodInfo getter, setter;

	public InstanceGetterSetterUpdateInsnTree(
		InsnTree receiver,
		int opcode,
		MethodInfo getter,
		MethodInfo setter,
		InsnTree updater
	) {
		super(updater);
		this.receiver = receiver;
		this.opcode   = opcode;
		this.getter   = getter;
		this.setter   = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.receiver.emitBytecode(method);
		method.node.visitInsn(DUP);
		this.getter.emit(method, this.opcode);
		this.updater.emitBytecode(method);
		this.setter.emit(method, this.opcode);
	}
}