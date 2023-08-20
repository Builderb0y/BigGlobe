package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;

public class ArrayUpdaterInsnTree extends AbstractUpdaterInsnTree {

	public InsnTree array, index, updater;
	public TypeInfo componentType;

	public ArrayUpdaterInsnTree(CombinedMode mode, InsnTree array, InsnTree index, InsnTree updater) {
		super(mode);
		this.array = array;
		this.index = index;
		this.updater = updater;
		this.componentType = array.getTypeInfo().componentType;
		if (this.componentType == null) {
			throw new InvalidOperandException("Not an array: " + array);
		}
	}

	public ArrayUpdaterInsnTree(UpdateOrder order, boolean isAssignment, InsnTree array, InsnTree index, InsnTree updater) {
		super(order, isAssignment);
		this.array = array;
		this.index = index;
		this.updater = updater;
		this.componentType = array.getTypeInfo().componentType;
		if (this.componentType == null) {
			throw new InvalidOperandException("Not an array: " + array);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.array.emitBytecode(method);
		this.index.emitBytecode(method);
		switch (this.mode) {
			case VOID -> {
				method.node.visitInsn(DUP2);
				method.node.visitInsn(this.componentType.getOpcode(IALOAD));
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.componentType.getOpcode(IASTORE));
			}
			case PRE -> {
				method.node.visitInsn(DUP2);
				method.node.visitInsn(this.componentType.getOpcode(IALOAD));
				method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.componentType.getOpcode(IASTORE));
			}
			case POST -> {
				method.node.visitInsn(DUP2);
				method.node.visitInsn(this.componentType.getOpcode(IALOAD));
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
				method.node.visitInsn(this.componentType.getOpcode(IASTORE));
			}
			case VOID_ASSIGN -> {
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.componentType.getOpcode(IASTORE));
			}
			case PRE_ASSIGN -> {
				method.node.visitInsn(DUP2);
				method.node.visitInsn(this.componentType.getOpcode(IALOAD));
				method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
				method.node.visitInsn(this.componentType.isDoubleWidth() ? POP2 : POP);
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.componentType.getOpcode(IASTORE));
			}
			case POST_ASSIGN -> {
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
				method.node.visitInsn(this.componentType.getOpcode(IASTORE));
			}
		}
	}

	@Override
	public TypeInfo getPreType() {
		return this.componentType;
	}

	@Override
	public TypeInfo getPostType() {
		return this.updater.getTypeInfo();
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new ArrayUpdaterInsnTree(this.mode.asVoid(), this.array, this.index, this.updater);
	}
}