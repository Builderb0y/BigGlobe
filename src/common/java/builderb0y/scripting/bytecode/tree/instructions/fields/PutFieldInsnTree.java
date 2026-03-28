package builderb0y.scripting.bytecode.tree.instructions.fields;

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
		check(object, field, value);
		this.object = object;
		this.value = value;
		this.field = field;
	}

	public static void check(InsnTree object, FieldInfo field, InsnTree value) {
		if (field.isStatic()) {
			throw new IllegalArgumentException("Static field: " + field);
		}
		if (!object.getTypeInfo().extendsOrImplements(field.owner)) {
			throw new IllegalArgumentException("Can't put field " + field + " in object of type " + object.getTypeInfo());
		}
		if (!value.getTypeInfo().extendsOrImplements(field.type)) {
			throw new IllegalArgumentException("Can't put " + value.getTypeInfo() + " in field of type " + field.type);
		}
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