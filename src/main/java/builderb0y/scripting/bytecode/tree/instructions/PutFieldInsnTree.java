package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class PutFieldInsnTree implements InsnTree {

	public InsnTree object;
	public FieldInfo field;
	public InsnTree value;

	public PutFieldInsnTree(InsnTree object, FieldInfo field, InsnTree value) {
		this.object = object;
		this.value = value;
		this.field = field;
	}

	public static PutFieldInsnTree create(InsnTree receiver, FieldInfo field, InsnTree value) {
		if (field.isStatic()) {
			throw new IllegalArgumentException("Static field: " + field);
		}
		if (!receiver.getTypeInfo().extendsOrImplements(field.owner)) {
			throw new IllegalArgumentException("Can't put field " + field + " in object of type " + receiver.getTypeInfo());
		}
		if (!value.getTypeInfo().extendsOrImplements(field.type)) {
			throw new IllegalArgumentException("Can't put " + value.getTypeInfo() + " in field of type " + field.type);
		}
		return new PutFieldInsnTree(receiver, field, value);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.object.emitBytecode(method);
		this.value.emitBytecode(method);
		this.field.emitPut(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}