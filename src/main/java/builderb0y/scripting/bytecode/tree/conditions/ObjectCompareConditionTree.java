package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.ArrayExtensions;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ObjectCompareConditionTree implements ConditionTree {

	public static final MethodInfo
		/*
		OBJECT_EQUALS          = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Objects.class), "equals", TypeInfos.BOOLEAN, TypeInfos.OBJECT, TypeInfos.OBJECT),
		EQUALS                 = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "equals",               TypeInfos.BOOLEAN, TypeInfos.OBJECT, TypeInfos.OBJECT),
		ARRAY_EQUALS           = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "arrayEquals",          TypeInfos.BOOLEAN, TypeInfos.OBJECT, TypeInfos.OBJECT),
		OBJECT_ARRAY_EQUALS    = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "objectArrayEquals",    TypeInfos.BOOLEAN, type(Object[].class), type(Object[].class)),
		PRIMITIVE_ARRAY_EQUALS = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "primitiveArrayEquals", TypeInfos.BOOLEAN, TypeInfos.OBJECT, TypeInfos.OBJECT),
		BYTE_ARRAY_EQUALS      = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(byte   [].class), type(byte   [].class)),
		SHORT_ARRAY_EQUALS     = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(short  [].class), type(short  [].class)),
		INT_ARRAY_EQUALS       = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(int    [].class), type(int    [].class)),
		LONG_ARRAY_EQUALS      = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(long   [].class), type(long   [].class)),
		FLOAT_ARRAY_EQUALS     = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(float  [].class), type(float  [].class)),
		DOUBLE_ARRAY_EQUALS    = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(double [].class), type(double [].class)),
		CHAR_ARRAY_EQUALS      = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(char   [].class), type(char   [].class)),
		BOOLEAN_ARRAY_EQUALS   = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(Arrays.class), "equals", TypeInfos.BOOLEAN, type(boolean[].class), type(boolean[].class)),
		*/
		COMPARE_G              = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "compareG", TypeInfos.INT, TypeInfos.COMPARABLE, TypeInfos.COMPARABLE),
		COMPARE_L              = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "compareL", TypeInfos.INT, TypeInfos.COMPARABLE, TypeInfos.COMPARABLE);

	public final InsnTree loader;
	public final int opcode;

	public ObjectCompareConditionTree(InsnTree loader, int opcode) {
		this.loader = loader;
		this.opcode = opcode;
	}

	public static ConditionTree equal(ExpressionParser parser, InsnTree left, InsnTree right) {
		return tryNull(parser, left, right, true);
	}

	public static ConditionTree notEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		return tryNull(parser, left, right, false);
	}

	public static ConditionTree lessThan(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_G, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFLT);
	}

	public static ConditionTree greaterThan(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_L, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFGT);
	}

	public static ConditionTree lessThanOrEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_G, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFLE);
	}

	public static ConditionTree greaterThanOrEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_L, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFGE);
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.loader.emitBytecode(method);
		if (ifTrue != null) {
			method.node.visitJumpInsn(this.opcode, ifTrue);
			if (ifFalse != null) {
				method.node.visitJumpInsn(GOTO, ifFalse);
			}
		}
		else {
			method.node.visitJumpInsn(ConditionTree.negateOpcode(this.opcode), ifFalse);
		}
	}

	public static ConditionTree tryNull(ExpressionParser parser, InsnTree left, InsnTree right, boolean equal) {
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && leftConstant.asJavaObject() == null) {
			if (rightConstant.isConstant() && rightConstant.asJavaObject() == null) {
				return new ConstantConditionTree(equal);
			}
			else {
				return new ObjectCompareConditionTree(right, equal ? IFNULL : IFNONNULL);
			}
		}
		else {
			if (rightConstant.isConstant() && rightConstant.asJavaObject() == null) {
				return new ObjectCompareConditionTree(left, equal ? IFNULL : IFNONNULL);
			}
			else {
				return new ObjectCompareConditionTree(equalLoader(parser, left, right), equal ? IFNE : IFEQ);
			}
		}
	}

	public static InsnTree equalLoader(ExpressionParser parser, InsnTree left, InsnTree right) {
		return ArrayExtensions.computeEquals(parser, left, right);
		/*
		TypeInfo leftType = left.getTypeInfo(), rightType = right.getTypeInfo();
		if (leftType.isArray() && rightType.isArray()) {
			TypeInfo leftComponent = leftType.componentType, rightComponent = rightType.componentType;
			if (!leftComponent.isPrimitiveValue() && !rightComponent.isPrimitiveValue()) { //both types are primitive arrays.
				if (leftComponent.equals(rightComponent)) { //same type of primitive arrays.
					return switch (leftComponent.getSort()) {
						case BYTE    -> invokeStatic(   BYTE_ARRAY_EQUALS, left, right);
						case SHORT   -> invokeStatic(  SHORT_ARRAY_EQUALS, left, right);
						case INT     -> invokeStatic(    INT_ARRAY_EQUALS, left, right);
						case LONG    -> invokeStatic(   LONG_ARRAY_EQUALS, left, right);
						case FLOAT   -> invokeStatic(  FLOAT_ARRAY_EQUALS, left, right);
						case DOUBLE  -> invokeStatic( DOUBLE_ARRAY_EQUALS, left, right);
						case CHAR    -> invokeStatic(   CHAR_ARRAY_EQUALS, left, right);
						case BOOLEAN -> invokeStatic(BOOLEAN_ARRAY_EQUALS, left, right);
						default      -> throw new AssertionError(leftComponent);
					};
				}
				else { //different types of primitive arrays.
					return left.then(parser, right).then(parser, ldc(0));
				}
			}
			else { //at least one type was not a primitive array.
				return invokeStatic(ARRAY_EQUALS, left, right);
			}
		}
		else { //at least one type was not an array.
			if (
				leftType.isArray() ||
				rightType.isArray() ||
				leftType.equals(TypeInfos.OBJECT) ||
				rightType.equals(TypeInfos.OBJECT)
			) { //at least one type could be an array.
				return invokeStatic(EQUALS, left, right);
			}
			else { //neither type could be an array.
				return invokeStatic(OBJECT_EQUALS, left, right);
			}
		}
		*/
	}

	/*
	public static boolean equals(Object a, Object b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		if (a.getClass().isArray()) {
			if (b.getClass().isArray()) {
				return arrayEquals(a, b);
			}
			else {
				return false;
			}
		}
		else {
			if (b.getClass().isArray()) {
				return false;
			}
			else {
				return a.equals(b);
			}
		}
	}

	public static boolean arrayEquals(Object a, Object b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		Class<?> ac = a.getClass().getComponentType(), bc = b.getClass().getComponentType();
		if (ac.isPrimitive()) {
			if (bc.isPrimitive()) {
				return primitiveArrayEquals(a, b);
			}
			else {
				return false;
			}
		}
		else {
			if (bc.isPrimitive()) {
				return false;
			}
			else {
				return objectArrayEquals((Object[])(a), (Object[])(b));
			}
		}
	}

	public static boolean primitiveArrayEquals(Object a, Object b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		Class<?> ac = a.getClass(), bc = b.getClass();
		if (ac != bc) return false;
		return switch (ac.getName().charAt(1)) {
			case 'B' -> Arrays.equals((byte   [])(a), (byte   [])(b));
			case 'S' -> Arrays.equals((short  [])(a), (short  [])(b));
			case 'I' -> Arrays.equals((int    [])(a), (int    [])(b));
			case 'J' -> Arrays.equals((long   [])(a), (long   [])(b));
			case 'F' -> Arrays.equals((float  [])(a), (float  [])(b));
			case 'D' -> Arrays.equals((double [])(a), (double [])(b));
			case 'C' -> Arrays.equals((char   [])(a), (char   [])(b));
			case 'Z' -> Arrays.equals((boolean[])(a), (boolean[])(b));
			default -> throw new AssertionError(ac);
		};
	}

	public static boolean objectArrayEquals(Object[] a, Object[] b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		int length = a.length;
		if (b.length != length) return false;
		for (int index = 0; index < length; index++) {
			if (!equals(a[index], b[index])) return false;
		}
		return true;
	}
	*/

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int compareG(Comparable a, Comparable b) {
		return a == null || b == null ? 1 : a.compareTo(b);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int compareL(Comparable a, Comparable b) {
		return a == null || b == null ? -1 : a.compareTo(b);
	}
}