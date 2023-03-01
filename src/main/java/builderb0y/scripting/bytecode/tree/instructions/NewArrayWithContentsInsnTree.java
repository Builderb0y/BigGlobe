package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NewArrayWithContentsInsnTree implements InsnTree {

	public TypeInfo arrayType;
	public InsnTree[] elements;

	public NewArrayWithContentsInsnTree(TypeInfo arrayType, InsnTree... elements) {
		this.arrayType = arrayType;
		this.elements = elements;
	}

	public static InsnTree create(ExpressionParser parser, TypeInfo arrayType, InsnTree... elements) {
		if (!arrayType.isArray()) {
			throw new IllegalArgumentException("Not an array: " + arrayType);
		}
		if (arrayType.componentType.isVoid()) {
			throw new InvalidOperandException("Can't allocate an array of voids");
		}
		InsnTree[] castElements = elements;
		TypeInfo componentType = arrayType.componentType;
		for (int index = 0, length = elements.length; index < length; index++) {
			InsnTree old = elements[index];
			InsnTree cast = old.cast(parser, componentType, CastMode.IMPLICIT_THROW);
			if (old != cast) {
				if (castElements == elements) castElements = castElements.clone();
				castElements[index] = cast;
			}
		}
		return new NewArrayWithContentsInsnTree(arrayType, castElements);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		InsnTree[] elements = this.elements;
		int length = elements.length;
		constant(length).emitBytecode(method);

		TypeInfo type = this.arrayType.componentType;
		switch (type.getSort()) {
			case BYTE    -> method.node.visitIntInsn(NEWARRAY, T_BYTE);
			case SHORT   -> method.node.visitIntInsn(NEWARRAY, T_SHORT);
			case INT     -> method.node.visitIntInsn(NEWARRAY, T_INT);
			case LONG    -> method.node.visitIntInsn(NEWARRAY, T_LONG);
			case FLOAT   -> method.node.visitIntInsn(NEWARRAY, T_FLOAT);
			case DOUBLE  -> method.node.visitIntInsn(NEWARRAY, T_DOUBLE);
			case CHAR    -> method.node.visitIntInsn(NEWARRAY, T_CHAR);
			case BOOLEAN -> method.node.visitIntInsn(NEWARRAY, T_BOOLEAN);
			case OBJECT, ARRAY -> method.node.visitTypeInsn(ANEWARRAY, type.getInternalName());
			case VOID -> throw new IllegalStateException("Can't allocate an array of voids");
		}
		for (int index = 0; index < length; index++) {
			method.node.visitInsn(DUP);
			constant(index).emitBytecode(method);
			elements[index].emitBytecode(method);
			method.node.visitInsn(type.getOpcode(IASTORE));
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.arrayType;
	}
}