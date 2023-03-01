package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;

public class NewArrayWithLengthInsnTree implements InsnTree {

	public TypeInfo arrayType;
	public InsnTree length;

	public NewArrayWithLengthInsnTree(TypeInfo arrayType, InsnTree length) {
		this.arrayType = arrayType;
		this.length = length;
	}

	public static InsnTree create(TypeInfo arrayType, InsnTree length) {
		if (!length.getTypeInfo().isSingleWidthInt()) {
			throw new InvalidOperandException("Length must be single-width int");
		}
		if (!arrayType.isArray()) {
			throw new InvalidOperandException("Not an array: " + arrayType);
		}
		if (arrayType.componentType.isVoid()) {
			throw new InvalidOperandException("Can't allocate an array of voids");
		}
		return new NewArrayWithLengthInsnTree(arrayType, length);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.length.emitBytecode(method);
		switch (this.arrayType.componentType.getSort()) {
			case BYTE    -> method.node.visitIntInsn(NEWARRAY, T_BYTE);
			case SHORT   -> method.node.visitIntInsn(NEWARRAY, T_SHORT);
			case INT     -> method.node.visitIntInsn(NEWARRAY, T_INT);
			case LONG    -> method.node.visitIntInsn(NEWARRAY, T_LONG);
			case FLOAT   -> method.node.visitIntInsn(NEWARRAY, T_FLOAT);
			case DOUBLE  -> method.node.visitIntInsn(NEWARRAY, T_DOUBLE);
			case CHAR    -> method.node.visitIntInsn(NEWARRAY, T_CHAR);
			case BOOLEAN -> method.node.visitIntInsn(NEWARRAY, T_BOOLEAN);
			case OBJECT, ARRAY -> method.node.visitTypeInsn(ANEWARRAY, this.arrayType.componentType.getInternalName());
			case VOID -> throw new IllegalStateException("Can't allocate an array of voids");
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.arrayType;
	}
}