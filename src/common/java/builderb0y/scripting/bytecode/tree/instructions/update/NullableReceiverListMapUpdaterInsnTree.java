package builderb0y.scripting.bytecode.tree.instructions.update;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableReceiverListMapUpdaterInsnTree extends ListMapUpdaterInsnTree {

	public NullableReceiverListMapUpdaterInsnTree(CombinedMode mode, InsnTree receiver, InsnTree key, InsnTree value, MethodInfo replacer) {
		super(mode, receiver, key, value, replacer);
	}

	public NullableReceiverListMapUpdaterInsnTree(UpdateOrder order, boolean isAssignment, InsnTree receiver, InsnTree key, InsnTree value, MethodInfo replacer) {
		super(order, isAssignment, receiver, key, value, replacer);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label update = label(), end = label();

		this.receiver.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, update);
		method.node.visitInsn(POP);
		method.node.visitJumpInsn(GOTO, end);
		method.node.visitLabel(update);
		this.key.emitBytecode(method);
		this.value.emitBytecode(method);
		switch (this.mode) {
			case VOID, PRE, POST -> {
				throw new IllegalStateException("Updating List or Map not yet implemented.");
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
		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.receiver.getTypeInfo();
	}

	@Override
	public InsnTree asStatement() {
		return new NullableListMapUpdaterInsnTree(this.mode.asVoid(), this.receiver, this.key, this.value, this.replacer);
	}
}