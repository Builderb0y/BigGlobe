package builderb0y.scripting.bytecode.tree.flow;

import java.util.Iterator;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.util.TypeInfos;

public abstract class AbstractForIteratorInsnTree implements InsnTree {

	public static final MethodInfo
		ITERATOR      = MethodInfo.getMethod(Iterable .class, "iterator"    ),
		HAS_NEXT      = MethodInfo.getMethod(Iterator .class, "hasNext"     ),
		NEXT          = MethodInfo.getMethod(Iterator .class, "next"        ),
		BYTE_VALUE    = MethodInfo.getMethod(Byte     .class, "byteValue"   ),
		SHORT_VALUE   = MethodInfo.getMethod(Short    .class, "shortValue"  ),
		INT_VALUE     = MethodInfo.getMethod(Integer  .class, "intValue"    ),
		LONG_VALUE    = MethodInfo.getMethod(Long     .class, "longValue"   ),
		FLOAT_VALUE   = MethodInfo.getMethod(Float    .class, "floatValue"  ),
		DOUBLE_VALUE  = MethodInfo.getMethod(Double   .class, "doubleValue" ),
		CHAR_VALUE    = MethodInfo.getMethod(Character.class, "charValue"   ),
		BOOLEAN_VALUE = MethodInfo.getMethod(Boolean  .class, "booleanValue");

	public String loopName;

	public AbstractForIteratorInsnTree(String loopName) {
		this.loopName = loopName;
	}

	/** assumes the value to be stored is already on the stack, and is of type Object. */
	public static void castAndstore(VariableDeclarationInsnTree variable, MethodCompileContext method) {
		if (!variable.variable.type.equals(TypeInfos.OBJECT)) {
			method.node.visitTypeInsn(CHECKCAST, variable.variable.type.box().getInternalName());
			switch (variable.variable.type.getSort()) {
				case BYTE    ->    BYTE_VALUE.emit(method);
				case SHORT   ->   SHORT_VALUE.emit(method);
				case INT     ->     INT_VALUE.emit(method);
				case LONG    ->    LONG_VALUE.emit(method);
				case FLOAT   ->   FLOAT_VALUE.emit(method);
				case DOUBLE  ->  DOUBLE_VALUE.emit(method);
				case CHAR    ->    CHAR_VALUE.emit(method);
				case BOOLEAN -> BOOLEAN_VALUE.emit(method);
				case OBJECT, ARRAY, VOID -> {}
			}
		}
		variable.variable.emitStore(method);
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}
}