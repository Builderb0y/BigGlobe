package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PostUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PreUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.VoidUpdateInsnTree;

public abstract class StaticFieldUpdateInsnTree implements UpdateInsnTree {

	public FieldInfo field;
	public InsnTree updater;

	public StaticFieldUpdateInsnTree(FieldInfo field, InsnTree updater) {
		this.field = field;
		this.updater = updater;
	}

	public static class StaticFieldVoidUpdateInsnTree extends StaticFieldUpdateInsnTree implements VoidUpdateInsnTree {

		public StaticFieldVoidUpdateInsnTree(FieldInfo field, InsnTree updater) {
			super(field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.field.emitGet(method);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}
	}

	public static class StaticFieldPreUpdateInsnTree extends StaticFieldUpdateInsnTree implements PreUpdateInsnTree {

		public StaticFieldPreUpdateInsnTree(FieldInfo field, InsnTree updater) {
			super(field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.field.emitGet(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2 : DUP);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new StaticFieldVoidUpdateInsnTree(this.field, this.updater);
		}
	}

	public static class StaticFieldPostUpdateInsnTree extends StaticFieldUpdateInsnTree implements PostUpdateInsnTree {

		public StaticFieldPostUpdateInsnTree(FieldInfo field, InsnTree updater) {
			super(field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.field.emitGet(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2 : DUP);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new StaticFieldVoidUpdateInsnTree(this.field, this.updater);
		}
	}

	public static class StaticFieldAssignVoidUpdateInsnTree extends StaticFieldUpdateInsnTree implements VoidUpdateInsnTree {

		public StaticFieldAssignVoidUpdateInsnTree(FieldInfo field, InsnTree updater) {
			super(field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}
	}

	public static class StaticFieldAssignPreUpdateInsnTree extends StaticFieldUpdateInsnTree implements PreUpdateInsnTree {

		public StaticFieldAssignPreUpdateInsnTree(FieldInfo field, InsnTree updater) {
			super(field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.field.emitGet(method);
			this.updater.emitBytecode(method);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.field.type;
		}

		@Override
		public InsnTree asStatement() {
			return new StaticFieldAssignVoidUpdateInsnTree(this.field, this.updater);
		}
	}

	public static class StaticFieldAssignPostUpdateInsnTree extends StaticFieldUpdateInsnTree implements PostUpdateInsnTree {

		public StaticFieldAssignPostUpdateInsnTree(FieldInfo field, InsnTree updater) {
			super(field, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.field.type.isDoubleWidth() ? DUP2 : DUP);
			this.field.emitPut(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new StaticFieldAssignVoidUpdateInsnTree(this.field, this.updater);
		}
	}
}