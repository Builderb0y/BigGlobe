package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ArgumentedObjectUpdateInsnTree extends AbstractUpdaterInsnTree {

	public final ArgumentedObjectUpdateEmitters emitters;

	public ArgumentedObjectUpdateInsnTree(CombinedMode mode, ArgumentedObjectUpdateEmitters emitters) {
		super(mode);
		this.emitters = emitters;
	}

	public ArgumentedObjectUpdateInsnTree(UpdateOrder order, boolean isAssignment, ArgumentedObjectUpdateEmitters emitters) {
		super(order, isAssignment);
		this.emitters = emitters;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.emitObject(method);
		this.emitArgument(method);
		switch (this.mode) {
			case VOID -> {
				method.node.visitInsn(DUP2);
				this.emitGet(method);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE -> {
				method.node.visitInsn(DUP2);
				this.emitGet(method);
				method.node.visitInsn(this.getPreType().isDoubleWidth() ? DUP2_X2 : DUP_X2);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST -> {
				method.node.visitInsn(DUP2);
				this.emitGet(method);
				this.emitUpdate(method);
				method.node.visitInsn(this.getPostType().isDoubleWidth() ? DUP2_X2 : DUP_X2);
				this.emitSet(method);
			}
			case VOID_ASSIGN -> {
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case PRE_ASSIGN -> {
				method.node.visitInsn(DUP2);
				this.emitGet(method);
				method.node.visitInsn(this.getPreType().isDoubleWidth() ? DUP2_X2 : DUP_X2);
				method.node.visitInsn(this.getPreType().isDoubleWidth() ? POP2 : POP);
				this.emitUpdate(method);
				this.emitSet(method);
			}
			case POST_ASSIGN -> {
				this.emitUpdate(method);
				method.node.visitInsn(this.getPostType().isDoubleWidth() ? DUP2_X2 : DUP_X2);
				this.emitSet(method);
			}
		}
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new ArgumentedObjectUpdateInsnTree(this.mode, this.emitters);
	}

	public void emitObject(MethodCompileContext method) {
		this.emitters.object.emitBytecode(method);
	}

	public void emitArgument(MethodCompileContext method) {
		this.emitters.argument.emitBytecode(method);
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

	public static record ArgumentedObjectUpdateEmitters(
		BytecodeEmitter object,
		BytecodeEmitter argument,
		BytecodeEmitter getter,
		BytecodeEmitter setter,
		BytecodeEmitter updater,
		TypeInfo objectType,
		TypeInfo preType,
		TypeInfo postType
	) {

		public static ArgumentedObjectUpdateEmitters forField(InsnTree object, InsnTree argument, FieldInfo field, InsnTree updater) {
			return new ArgumentedObjectUpdateEmitters(object, argument, field::emitGet, field::emitPut, updater, object.getTypeInfo(), field.type, updater.getTypeInfo());
		}

		public static ArgumentedObjectUpdateEmitters forGetterSetter(InsnTree object, InsnTree argument, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			return new ArgumentedObjectUpdateEmitters(object, argument, getter, setter, updater, object.getTypeInfo(), getter.returnType, updater.getTypeInfo());
		}
	}
}