package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.IncrementInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PostUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.PreUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.UpdateInsnTrees.VoidUpdateInsnTree;
import builderb0y.scripting.util.TypeInfos;

public abstract class VariableUpdateInsnTree implements UpdateInsnTree {

	public VarInfo variable;
	public InsnTree updater;

	public VariableUpdateInsnTree(VarInfo variable, InsnTree updater) {
		this.variable = variable;
		this.updater = updater;
	}

	public static class VariableVoidUpdateInsnTree extends VariableUpdateInsnTree implements VoidUpdateInsnTree {

		public VariableVoidUpdateInsnTree(VarInfo variable, InsnTree updater) {
			super(variable, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.variable.emitLoad(method);
			this.updater.emitBytecode(method);
			this.variable.emitStore(method);
		}
	}

	public static class VariablePreUpdateInsnTree extends VariableUpdateInsnTree implements PreUpdateInsnTree {

		public VariablePreUpdateInsnTree(VarInfo variable, InsnTree updater) {
			super(variable, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.variable.emitLoad(method);
			method.node.visitInsn(this.variable.type.isDoubleWidth() ? DUP2 : DUP);
			this.updater.emitBytecode(method);
			this.variable.emitStore(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.variable.type;
		}

		@Override
		public InsnTree asStatement() {
			return new VariableVoidUpdateInsnTree(this.variable, this.updater);
		}
	}

	public static class VariablePostUpdateInsnTree extends VariableUpdateInsnTree implements PostUpdateInsnTree {

		public VariablePostUpdateInsnTree(VarInfo variable, InsnTree updater) {
			super(variable, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.variable.emitLoad(method);
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.variable.type.isDoubleWidth() ? DUP2 : DUP);
			this.variable.emitStore(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new VariableVoidUpdateInsnTree(this.variable, this.updater);
		}
	}

	public static class VariableAssignVoidUpdateInsnTree extends VariableUpdateInsnTree implements VoidUpdateInsnTree {

		public VariableAssignVoidUpdateInsnTree(VarInfo variable, InsnTree updater) {
			super(variable, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.updater.emitBytecode(method);
			this.variable.emitStore(method);
		}
	}

	public static class VariableAssignPreUpdateInsnTree extends VariableUpdateInsnTree implements PreUpdateInsnTree {

		public VariableAssignPreUpdateInsnTree(VarInfo variable, InsnTree updater) {
			super(variable, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.variable.emitLoad(method);
			this.updater.emitBytecode(method);
			this.variable.emitStore(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.variable.type;
		}

		@Override
		public InsnTree asStatement() {
			return new VariableAssignVoidUpdateInsnTree(this.variable, this.updater);
		}
	}

	public static class VariableAssignPostUpdateInsnTree extends VariableUpdateInsnTree implements PostUpdateInsnTree {

		public VariableAssignPostUpdateInsnTree(VarInfo variable, InsnTree updater) {
			super(variable, updater);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.updater.emitBytecode(method);
			method.node.visitInsn(this.variable.type.isDoubleWidth() ? DUP2 : DUP);
			this.variable.emitStore(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.updater.getTypeInfo();
		}

		@Override
		public InsnTree asStatement() {
			return new VariableAssignVoidUpdateInsnTree(this.variable, this.updater);
		}
	}

	public static class VariableIncrementVoidUpdateInsnTree extends IncrementInsnTree implements VoidUpdateInsnTree {

		public VariableIncrementVoidUpdateInsnTree(VarInfo variable, int amount) {
			super(variable, amount);
		}
	}

	public static class VariableIncrementPreUpdateInsnTree extends IncrementInsnTree implements PreUpdateInsnTree {

		public VariableIncrementPreUpdateInsnTree(VarInfo variable, int amount) {
			super(variable, amount);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.variable.emitLoad(method);
			super.emitBytecode(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.INT;
		}

		@Override
		public InsnTree asStatement() {
			return new VariableIncrementVoidUpdateInsnTree(this.variable, this.amount);
		}
	}

	public static class VariableIncrementPostUpdateInsnTree extends IncrementInsnTree implements PreUpdateInsnTree {

		public VariableIncrementPostUpdateInsnTree(VarInfo variable, int amount) {
			super(variable, amount);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			super.emitBytecode(method);
			this.variable.emitLoad(method);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return TypeInfos.INT;
		}

		@Override
		public InsnTree asStatement() {
			return new VariableIncrementVoidUpdateInsnTree(this.variable, this.amount);
		}
	}
}