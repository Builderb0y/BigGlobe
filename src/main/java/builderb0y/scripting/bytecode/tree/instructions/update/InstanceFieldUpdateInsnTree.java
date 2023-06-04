package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PostUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PreUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.VoidUpdateInsnTree;

public abstract class InstanceFieldUpdateInsnTree implements UpdateInsnTree {

	public InsnTree object, updater;
	public FieldInfo field;

	public InstanceFieldUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
		this.object = object;
		this.field = field;
		this.updater = updater;
	}

	public static class InstanceFieldVoidUpdateInsnTree extends InstanceFieldUpdateInsnTree implements VoidUpdateInsnTree {

		public InstanceFieldVoidUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.field.emitGet(method);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}
	}

	public static class InstanceFieldPreUpdateInsnTree extends InstanceFieldUpdateInsnTree implements PreUpdateInsnTree {

		public InstanceFieldPreUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.field.emitGet(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceFieldVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}

	public static class InstanceFieldPostUpdateInsnTree extends InstanceFieldUpdateInsnTree implements PostUpdateInsnTree {

		public InstanceFieldPostUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.field.emitGet(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceFieldVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}

	public static class InstanceFieldAssignVoidUpdateInsnTree extends InstanceFieldUpdateInsnTree implements VoidUpdateInsnTree {

		public InstanceFieldAssignVoidUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.object.emitBytecode(method);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}
	}

	public static class InstanceFieldAssignPreUpdateInsnTree extends InstanceFieldUpdateInsnTree implements PreUpdateInsnTree {

		public InstanceFieldAssignPreUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.field.emitGet(method);
			if (this.field.type.isDoubleWidth()) {
				method.node.visitInsn(DUP2_X1);
				method.node.visitInsn(POP2);
			}
			else {
				method.node.visitInsn(SWAP);
			}
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceFieldAssignVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}

	public static class InstanceFieldAssignPostUpdateInsnTree extends InstanceFieldUpdateInsnTree implements PostUpdateInsnTree {

		public InstanceFieldAssignPostUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.object.emitBytecode(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceFieldAssignVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}
}