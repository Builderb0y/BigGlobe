package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PostUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PreUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.VoidUpdateInsnTree;

public abstract class StaticGetterSetterUpdateInsnTree implements UpdateInsnTree {

	public MethodInfo getter, setter;
	public InsnTree updater;

	public StaticGetterSetterUpdateInsnTree(MethodInfo getter, MethodInfo setter, InsnTree updater) {
		this.getter  = getter;
		this.setter  = setter;
		this.updater = updater;
	}

	public static class StaticGetterSetterVoidUpdateInsnTree extends StaticGetterSetterUpdateInsnTree implements VoidUpdateInsnTree {

		public StaticGetterSetterVoidUpdateInsnTree(MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.getter.emit(method, INVOKESTATIC);
			this.updater.emitBytecode(method);
			this.setter.emit(method, INVOKESTATIC);
		}
	}

	public static class StaticGetterSetterPreUpdateInsnTree extends StaticGetterSetterUpdateInsnTree implements PreUpdateInsnTree {

		public StaticGetterSetterPreUpdateInsnTree(MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.getter.emit(method, INVOKESTATIC);
			method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2 : DUP);
			this.updater.emitBytecode(method);
			this.setter.emit(method, INVOKESTATIC);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.getter.returnType;
		}

		@Override
		public InsnTree asStatement() {
			return new StaticGetterSetterVoidUpdateInsnTree(this.getter, this.setter, this.updater);
		}
	}

	public static class StaticGetterSetterPostUpdateInsnTree extends StaticGetterSetterUpdateInsnTree implements PostUpdateInsnTree {

		public StaticGetterSetterPostUpdateInsnTree(MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.getter.emit(method, INVOKESTATIC);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2 : DUP);
			this.setter.emit(method, INVOKESTATIC);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new StaticGetterSetterVoidUpdateInsnTree(this.getter, this.setter, this.updater);
		}
	}

	public static class StaticGetterSetterAssignVoidUpdateInsnTree extends StaticGetterSetterUpdateInsnTree implements VoidUpdateInsnTree {

		public StaticGetterSetterAssignVoidUpdateInsnTree(MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.updater.emitBytecode(method);
			this.setter.emit(method, INVOKESTATIC);
		}
	}

	public static class StaticGetterSetterAssignPreUpdateInsnTree extends StaticGetterSetterUpdateInsnTree implements PreUpdateInsnTree {

		public StaticGetterSetterAssignPreUpdateInsnTree(MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.getter.emit(method, INVOKESTATIC);
			this.updater.emitBytecode(method);
			this.setter.emit(method, INVOKESTATIC);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.getter.returnType;
		}

		@Override
		public InsnTree asStatement() {
			return new StaticGetterSetterAssignVoidUpdateInsnTree(this.getter, this.setter, this.updater);
		}
	}

	public static class StaticGetterSetterAssignPostUpdateInsnTree extends StaticGetterSetterUpdateInsnTree implements PostUpdateInsnTree {

		public StaticGetterSetterAssignPostUpdateInsnTree(MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2 : DUP);
			this.setter.emit(method, INVOKESTATIC);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new StaticGetterSetterAssignVoidUpdateInsnTree(this.getter, this.setter, this.updater);
		}
	}
}