package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.UpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.UpdateInsnTrees.PostUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.UpdateInsnTrees.PreUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.UpdateInsnTrees.VoidUpdateInsnTree;

public abstract class ArrayUpdateInsnTree implements UpdateInsnTree {

	public InsnTree array, index, updater;
	public TypeInfo componentType;

	public ArrayUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
		this.array = array;
		this.index = index;
		this.updater = updater;
		this.componentType = array.getTypeInfo().componentType;
		if (this.componentType == null) {
			throw new IllegalArgumentException("Not an array: " + array);
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

	public static class ArrayVoidUpdateInsnTree extends ArrayUpdateInsnTree implements VoidUpdateInsnTree {

		public ArrayVoidUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
			super(array, index, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.array.emitBytecode(method);
			this.index.emitBytecode(method);
			method.node.visitInsn(DUP2);
			method.node.visitInsn(this.componentType.getOpcode(IALOAD));
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.componentType.getOpcode(IASTORE));
		}
	}

	public static class ArrayPreUpdateInsnTree extends ArrayUpdateInsnTree implements PreUpdateInsnTree {

		public ArrayPreUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
			super(array, index, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.array.emitBytecode(method);
			this.index.emitBytecode(method);
			method.node.visitInsn(DUP2);
			method.node.visitInsn(this.componentType.getOpcode(IALOAD));
			method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.componentType.getOpcode(IASTORE));
		}

		@Override
		public InsnTree asStatement() {
			return new ArrayVoidUpdateInsnTree(this.array, this.index, this.updater);
		}
	}

	public static class ArrayPostUpdateInsnTree extends ArrayUpdateInsnTree implements PostUpdateInsnTree {

		public ArrayPostUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
			super(array, index, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.array.emitBytecode(method);
			this.index.emitBytecode(method);
			method.node.visitInsn(DUP2);
			method.node.visitInsn(this.componentType.getOpcode(IALOAD));
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
			method.node.visitInsn(this.componentType.getOpcode(IASTORE));
		}

		@Override
		public InsnTree asStatement() {
			return new ArrayVoidUpdateInsnTree(this.array, this.index, this.updater);
		}
	}

	public static class ArrayAssignVoidUpdateInsnTree extends ArrayUpdateInsnTree implements VoidUpdateInsnTree {

		public ArrayAssignVoidUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
			super(array, index, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.array.emitBytecode(method);
			this.index.emitBytecode(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.componentType.getOpcode(IASTORE));
		}
	}

	public static class ArrayAssignPreUpdateInsnTree extends ArrayUpdateInsnTree implements PreUpdateInsnTree {

		public ArrayAssignPreUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
			super(array, index, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.array.emitBytecode(method);
			this.index.emitBytecode(method);
			method.node.visitInsn(DUP2);
			method.node.visitInsn(this.componentType.getOpcode(IALOAD));
			method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
			method.node.visitInsn(this.componentType.isDoubleWidth() ? POP2 : POP);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.componentType.getOpcode(IASTORE));
		}

		@Override
		public InsnTree asStatement() {
			return new ArrayAssignVoidUpdateInsnTree(this.array, this.index, this.updater);
		}
	}

	public static class ArrayAssignPostUpdateInsnTree extends ArrayUpdateInsnTree implements PostUpdateInsnTree {

		public ArrayAssignPostUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
			super(array, index, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.array.emitBytecode(method);
			this.index.emitBytecode(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.componentType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
			method.node.visitInsn(this.componentType.getOpcode(IASTORE));
		}

		@Override
		public InsnTree asStatement() {
			return new ArrayAssignVoidUpdateInsnTree(this.array, this.index, this.updater);
		}
	}
}