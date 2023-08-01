package builderb0y.scripting.bytecode.tree.instructions.fields;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class PutStaticInsnTree implements InsnTree {

	public InsnTree value;
	public FieldInfo field;

	public PutStaticInsnTree(FieldInfo field, InsnTree value) {
		check(field, value);
		this.field = field;
		this.value = value;
	}

	public static void check(FieldInfo field, InsnTree value) {
		if (!field.isStatic()) {
			throw new IllegalArgumentException("Non-static field: " + field);
		}
		if (!value.getTypeInfo().extendsOrImplements(field.type)) {
			throw new IllegalArgumentException("Can't store " + value.getTypeInfo() + " in field of type " + field.type);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
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