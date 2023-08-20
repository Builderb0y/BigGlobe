package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ReceiverListMapUpdaterInsnTree extends ListMapUpdaterInsnTree {

	public ReceiverListMapUpdaterInsnTree(CombinedMode mode, InsnTree receiver, InsnTree key, InsnTree value, MethodInfo replacer) {
		super(mode, receiver, key, value, replacer);
	}

	public ReceiverListMapUpdaterInsnTree(UpdateOrder order, boolean isAssignment, InsnTree receiver, InsnTree key, InsnTree value, MethodInfo replacer) {
		super(order, isAssignment, receiver, key, value, replacer);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.receiver.emitBytecode(method);
		method.node.visitInsn(DUP);
		this.key.emitBytecode(method);
		this.value.emitBytecode(method);
		switch (this.mode) {
			case VOID, PRE, POST -> {
				throw new IllegalStateException("Updating List or Map not implemented yet.");
			}
			case VOID_ASSIGN -> {
				this.replacer.emitBytecode(method);
				method.node.visitInsn(POP);
			}
			case PRE_ASSIGN -> {
				this.replacer.emitBytecode(method);
			}
			case POST_ASSIGN -> {
				method.node.visitInsn(DUP_X2);
				this.replacer.emitBytecode(method);
				method.node.visitInsn(POP);
			}
		}
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new ReceiverListMapUpdaterInsnTree(this.mode.asVoid(), this.receiver, this.key, this.value, this.replacer);
	}
}