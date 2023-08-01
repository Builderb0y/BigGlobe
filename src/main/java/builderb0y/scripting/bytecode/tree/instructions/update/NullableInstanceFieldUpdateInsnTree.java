package builderb0y.scripting.bytecode.tree.instructions.update;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PostUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PreUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.VoidUpdateInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class NullableInstanceFieldUpdateInsnTree implements UpdateInsnTree {

	public InsnTree object, updater;
	public FieldInfo field;

	public NullableInstanceFieldUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
		this.object = object;
		this.field = field;
		this.updater = updater;
	}

	public static class NullableInstanceFieldVoidUpdateInsnTree extends NullableInstanceFieldUpdateInsnTree implements VoidUpdateInsnTree {

		public NullableInstanceFieldVoidUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			Label update = label(), end = label();

			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			method.node.visitJumpInsn(IFNONNULL, update);
			method.node.visitInsn(POP);
			method.node.visitJumpInsn(GOTO, end);

			method.node.visitLabel(update);
			method.node.visitInsn(DUP);
			this.field.emitGet(method);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);

			method.node.visitLabel(end);
		}
	}

	public static class NullableInstanceFieldPreUpdateInsnTree extends NullableInstanceFieldUpdateInsnTree implements PreUpdateInsnTree {

		public NullableInstanceFieldPreUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			Label update = label(), end = label();

			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			method.node.visitJumpInsn(IFNONNULL, update);
			method.node.visitInsn(POP);
			constantAbsent(this.field.type).emitBytecode(method);
			method.node.visitJumpInsn(GOTO, end);

			method.node.visitLabel(update);
			method.node.visitInsn(DUP);
			this.field.emitGet(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);

			method.node.visitLabel(end);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new NullableInstanceFieldVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}

	public static class NullableInstanceFieldPostUpdateInsnTree extends NullableInstanceFieldUpdateInsnTree implements PostUpdateInsnTree {

		public NullableInstanceFieldPostUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			Label update = label(), end = label();

			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			method.node.visitJumpInsn(IFNONNULL, update);
			method.node.visitInsn(POP);
			constantAbsent(this.field.type).emitBytecode(method);
			method.node.visitJumpInsn(GOTO, end);

			method.node.visitLabel(update);
			method.node.visitInsn(DUP);
			this.field.emitGet(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.field.emitPut(method);

			method.node.visitLabel(end);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new NullableInstanceFieldVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}

	public static class NullableInstanceFieldAssignVoidUpdateInsnTree extends NullableInstanceFieldUpdateInsnTree implements VoidUpdateInsnTree {

		public NullableInstanceFieldAssignVoidUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			Label update = label(), end = label();

			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			method.node.visitJumpInsn(IFNONNULL, update);
			method.node.visitInsn(POP);
			method.node.visitJumpInsn(GOTO, end);

			method.node.visitLabel(update);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);

			method.node.visitLabel(end);
		}
	}

	public static class NullableInstanceFieldAssignPreUpdateInsnTree extends NullableInstanceFieldUpdateInsnTree implements PreUpdateInsnTree {

		public NullableInstanceFieldAssignPreUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			Label update = label(), end = label();

			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			method.node.visitJumpInsn(IFNONNULL, update);
			method.node.visitInsn(POP);
			constantAbsent(this.field.type).emitBytecode(method);
			method.node.visitJumpInsn(GOTO, end);

			method.node.visitLabel(update);
			method.node.visitInsn(DUP); //object object
			this.field.emitGet(method); //object value
			if (this.field.type.isDoubleWidth()) {
				method.node.visitInsn(DUP2_X1);
				method.node.visitInsn(POP2);
			}
			else {
				method.node.visitInsn(SWAP);
			}
			//value object
			this.updater.emitBytecode(method);
			this.field.emitPut(method);

			method.node.visitLabel(end);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new NullableInstanceFieldAssignVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}

	public static class NullableInstanceFieldAssignPostUpdateInsnTree extends NullableInstanceFieldUpdateInsnTree implements PostUpdateInsnTree {

		public NullableInstanceFieldAssignPostUpdateInsnTree(InsnTree object, FieldInfo field, InsnTree updater) {
			super(object, field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			Label update = label(), end = label();

			this.object.emitBytecode(method);
			method.node.visitInsn(DUP);
			method.node.visitJumpInsn(IFNONNULL, update);
			method.node.visitInsn(POP);
			constantAbsent(this.field.type).emitBytecode(method);
			method.node.visitJumpInsn(GOTO, end);

			method.node.visitLabel(update);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.field.emitPut(method);

			method.node.visitLabel(end);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new NullableInstanceFieldAssignVoidUpdateInsnTree(this.object, this.field, this.updater);
		}
	}
}