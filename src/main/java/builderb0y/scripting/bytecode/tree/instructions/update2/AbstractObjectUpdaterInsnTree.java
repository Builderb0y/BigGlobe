package builderb0y.scripting.bytecode.tree.instructions.update2;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public abstract class AbstractObjectUpdaterInsnTree extends AbstractUpdaterInsnTree {

	public ObjectUpdaterEmitters emitters;

	public AbstractObjectUpdaterInsnTree(CombinedMode mode, ObjectUpdaterEmitters emitters) {
		super(mode);
		this.emitters = emitters;
	}

	public AbstractObjectUpdaterInsnTree(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters) {
		super(order, isAssignment);
		this.emitters = emitters;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return switch (this.mode) {
			case VOID, VOID_ASSIGN -> TypeInfos.VOID;
			case PRE,   PRE_ASSIGN -> this.getPreType();
			case POST, POST_ASSIGN -> this.getPostType();
		};
	}

	public void emitObject(MethodCompileContext method) {
		this.emitters.object.emitBytecode(method);
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

	public static record ObjectUpdaterEmitters(
		BytecodeEmitter object,
		BytecodeEmitter getter,
		BytecodeEmitter setter,
		BytecodeEmitter updater,
		TypeInfo objectType,
		TypeInfo preType,
		TypeInfo postType
	) {

		public static ObjectUpdaterEmitters forField(InsnTree object, FieldInfo field, InsnTree updater) {
			return new ObjectUpdaterEmitters(object, field::emitGet, field::emitPut, updater, object.getTypeInfo(), field.type, updater.getTypeInfo());
		}

		public static ObjectUpdaterEmitters forGetterSetter(InsnTree object, MethodInfo getter, MethodInfo setter, InsnTree updater) {
			return new ObjectUpdaterEmitters(object, getter, setter, updater, object.getTypeInfo(), getter.returnType, updater.getTypeInfo());
		}
	}
}