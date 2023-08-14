package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class VariableUpdaterInsnTree extends AbstractUpdaterInsnTree {

	public VariableUpdaterEmitters emitters;

	public VariableUpdaterInsnTree(CombinedMode mode, VariableUpdaterEmitters emitters) {
		super(mode);
		this.emitters = emitters;
	}

	public VariableUpdaterInsnTree(UpdateOrder order, boolean isAssignment, VariableUpdaterEmitters emitters) {
		super(order, isAssignment);
		this.emitters = emitters;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		switch (this.mode) {
			case VOID -> {
				this.emitGet(method);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE -> {
				this.emitGet(method);
				method.node.visitInsn(this.getPreType().isDoubleWidth() ? DUP2 : DUP);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST -> {
				this.emitGet(method);
				this.emitUpdate(method);
				method.node.visitInsn(this.getPostType().isDoubleWidth() ? DUP2 : DUP);
				this.emitSet(method);
			}
			case VOID_ASSIGN -> {
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE_ASSIGN -> {
				this.emitGet(method);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST_ASSIGN -> {
				this.emitUpdate(method);
				method.node.visitInsn(this.getPostType().isDoubleWidth() ? DUP2 : DUP);
				this.emitSet(method);
			}
		}
	}

	public void emitGet(MethodCompileContext method) {
		this.emitters.getter.emitBytecode(method);
	}

	public void emitSet(MethodCompileContext method) {
		this.emitters.setter.emitBytecode(method);
	}

	public void emitUpdate(MethodCompileContext method) {
		this.emitters.updater.emitBytecode(method);
	}

	@Override
	public TypeInfo getPreType() {
		return this.emitters.preType;
	}

	@Override
	public TypeInfo getPostType() {
		return this.emitters.postType;
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new VariableUpdaterInsnTree(this.mode.asVoid(), this.emitters);
	}

	public static record VariableUpdaterEmitters(
		BytecodeEmitter getter,
		BytecodeEmitter setter,
		BytecodeEmitter updater,
		TypeInfo preType,
		TypeInfo postType
	) {

		public static VariableUpdaterEmitters forVariable(VarInfo variable, InsnTree updater) {
			return new VariableUpdaterEmitters(variable::emitLoad, variable::emitStore, updater, variable.type, updater.getTypeInfo());
		}

		public static VariableUpdaterEmitters forField(FieldInfo field, InsnTree updater) {
			if (!field.isStatic()) {
				throw new IllegalArgumentException("Non-static field: " + field);
			}
			return new VariableUpdaterEmitters(field::emitGet, field::emitPut, updater, field.type, updater.getTypeInfo());
		}
	}
}