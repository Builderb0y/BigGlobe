package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PostUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PreUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.VoidUpdateInsnTree;

public abstract class InstanceGetterSetterUpdateInsnTree implements UpdateInsnTree {

	public InsnTree receiver, updater;
	public MethodInfo getter, setter;

	public InstanceGetterSetterUpdateInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree updater) {
		this.receiver = receiver;
		this.getter   = getter;
		this.setter   = setter;
		this.updater  = updater;
	}

	public static class InstanceGetterSetterVoidUpdateInsnTree extends InstanceGetterSetterUpdateInsnTree implements VoidUpdateInsnTree {

		public InstanceGetterSetterVoidUpdateInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(receiver, getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.getter.emit(method, this.getter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
			this.updater.emitBytecode(method);
			this.setter.emit(method, this.setter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
		}
	}

	public static class InstanceGetterSetterPreUpdateInsnTree extends InstanceGetterSetterUpdateInsnTree implements PreUpdateInsnTree {

		public InstanceGetterSetterPreUpdateInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(receiver, getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.getter.emit(method, this.getter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
			method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.updater.emitBytecode(method);
			this.setter.emit(method, this.setter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.getter.returnType;
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceGetterSetterVoidUpdateInsnTree(this.receiver, this.getter, this.setter, this.updater);
		}
	}

	public static class InstanceGetterSetterPostUpdateInsnTree extends InstanceGetterSetterUpdateInsnTree implements PostUpdateInsnTree {

		public InstanceGetterSetterPostUpdateInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(receiver, getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.getter.emit(method, this.getter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.setter.emit(method, this.setter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceGetterSetterVoidUpdateInsnTree(this.receiver, this.getter, this.setter, this.updater);
		}
	}

	public static class InstanceGetterSetterAssignVoidUpdateInsnTree extends InstanceGetterSetterUpdateInsnTree implements VoidUpdateInsnTree {

		public InstanceGetterSetterAssignVoidUpdateInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(receiver, getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			this.updater.emitBytecode(method);
			this.setter.emit(method, this.setter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
		}
	}

	public static class InstanceGetterSetterAssignPreUpdateInsnTree extends InstanceGetterSetterUpdateInsnTree implements PreUpdateInsnTree {

		public InstanceGetterSetterAssignPreUpdateInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(receiver, getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.getter.emit(method, this.getter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
			if (this.getter.returnType.isDoubleWidth()) {
				method.node.visitInsn(DUP2_X1);
				method.node.visitInsn(POP2);
			}
			else {
				method.node.visitInsn(SWAP);
			}
			this.updater.emitBytecode(method);
			this.setter.emit(method, this.setter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.getter.returnType;
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceGetterSetterAssignVoidUpdateInsnTree(this.receiver, this.getter, this.setter, this.updater);
		}
	}

	public static class InstanceGetterSetterAssignPostUpdateInsnTree extends InstanceGetterSetterUpdateInsnTree implements PostUpdateInsnTree {

		public InstanceGetterSetterAssignPostUpdateInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			super(receiver, getter, setter, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.receiver.emitBytecode(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X1 : DUP_X1);
			this.setter.emit(method, this.setter.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new InstanceGetterSetterAssignVoidUpdateInsnTree(this.receiver, this.getter, this.setter, this.updater);
		}
	}
}