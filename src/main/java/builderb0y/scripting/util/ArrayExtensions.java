package builderb0y.scripting.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.DoubleCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.FloatCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.LongCompareConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ArrayExtensions {

	public static final Set<TypeInfo> ARRAY_CLASSES = Set.of(TypeInfos.OBJECT, TypeInfo.of(Cloneable.class), TypeInfo.of(Serializable.class));

	public static InsnTree computeEquals(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo leftType = left.getTypeInfo(), rightType = right.getTypeInfo();
		TypeInfo commonType = TypeMerger.computeMostSpecificType(leftType, rightType);
		left = left.cast(parser, commonType, CastMode.IMPLICIT_THROW);
		right = right.cast(parser, commonType, CastMode.IMPLICIT_THROW);
		return switch (commonType.getSort()) {
			case VOID    -> throw new IllegalArgumentException("Equals on void");
			case BYTE    -> bool(   IntCompareConditionTree.equal(left, right));
			case SHORT   -> bool(   IntCompareConditionTree.equal(left, right));
			case INT     -> bool(   IntCompareConditionTree.equal(left, right));
			case LONG    -> bool(  LongCompareConditionTree.equal(left, right));
			case FLOAT   -> bool( FloatCompareConditionTree.equal(left, right));
			case DOUBLE  -> bool(DoubleCompareConditionTree.equal(left, right));
			case CHAR    -> bool(   IntCompareConditionTree.equal(left, right));
			case BOOLEAN -> bool(   IntCompareConditionTree.equal(left, right));
			case OBJECT  -> {
				if (ARRAY_CLASSES.contains(commonType)) {
					yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, ArrayExtensions.class, "equals", boolean.class, Object.class, Object.class), left, right);
				}
				else {
					yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Objects.class, "equals", boolean.class, Object.class, Object.class), left, right);
				}
			}
			case ARRAY -> {
				TypeInfo componentType = commonType.componentType;
				yield switch (componentType.getSort()) {
					case VOID    -> throw new IllegalArgumentException("Array of voids");
					case BYTE    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, byte   [].class, byte   [].class), left, right);
					case SHORT   -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, short  [].class, short  [].class), left, right);
					case INT     -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, int    [].class, int    [].class), left, right);
					case LONG    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, long   [].class, long   [].class), left, right);
					case FLOAT   -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, float  [].class, float  [].class), left, right);
					case DOUBLE  -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, double [].class, double [].class), left, right);
					case CHAR    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, char   [].class, char   [].class), left, right);
					case BOOLEAN -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, boolean[].class, boolean[].class), left, right);
					case OBJECT  -> {
						if (ARRAY_CLASSES.contains(componentType)) {
							yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, ArrayExtensions.class, "equals", boolean.class, Object[].class, Object[].class), left, right);
						}
						else {
							yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "equals", boolean.class, Object[].class, Object[].class), left, right);
						}
					}
					case ARRAY -> {
						yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, ArrayExtensions.class, "equals", boolean.class, Object[].class, Object[].class), left, right);
					}
				};
			}
		};
	}

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
		if (ac != bc) return false;
		if (ac.isPrimitive()) {
			return primitiveArrayEquals(a, b);
		}
		else if (ac.isArray() || ac == Object.class) {
			return objectArrayEquals((Object[])(a), (Object[])(b));
		}
		else {
			return Arrays.equals((Object[])(a), (Object[])(b));
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

	public static InsnTree computeHashCode(InsnTree object) {
		TypeInfo type = object.getTypeInfo();
		return switch (type.getSort()) {
			case VOID    -> throw new IllegalArgumentException("Hash code of void");
			case BYTE    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Byte     .class, "hashCode", int.class, byte   .class), object);
			case SHORT   -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Short    .class, "hashCode", int.class, short  .class), object);
			case INT     -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Integer  .class, "hashCode", int.class, int    .class), object);
			case LONG    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Long     .class, "hashCode", int.class, long   .class), object);
			case FLOAT   -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Float    .class, "hashCode", int.class, float  .class), object);
			case DOUBLE  -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Double   .class, "hashCode", int.class, double .class), object);
			case CHAR    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Character.class, "hashCode", int.class, char   .class), object);
			case BOOLEAN -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Boolean  .class, "hashCode", int.class, boolean.class), object);
			case OBJECT  -> {
				if (ARRAY_CLASSES.contains(type)) {
					yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, ArrayExtensions.class, "hashCode", int.class, Object.class), object);
				}
				else {
					yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Objects.class, "hashCode", int.class, Object.class), object);
				}
			}
			case ARRAY -> {
				TypeInfo componentType = type.componentType;
				yield switch (componentType.getSort()) {
					case VOID    -> throw new IllegalArgumentException("Array of voids");
					case BYTE    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, byte   [].class), object);
					case SHORT   -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, short  [].class), object);
					case INT     -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, int    [].class), object);
					case LONG    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, long   [].class), object);
					case FLOAT   -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, float  [].class), object);
					case DOUBLE  -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, double [].class), object);
					case CHAR    -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, char   [].class), object);
					case BOOLEAN -> invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, boolean[].class), object);
					case OBJECT  -> {
						if (ARRAY_CLASSES.contains(componentType)) {
							yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, ArrayExtensions.class, "objectArrayHashCode", int.class, Object[].class), object);
						}
						else {
							yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, Arrays.class, "hashCode", int.class, Object[].class), object);
						}
					}
					case ARRAY -> {
						yield invokeStatic(method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, ArrayExtensions.class, "objectArrayHashCode", int.class, Object[].class), object);
					}
				};
			}
		};
	}

	public static int hashCode(Object object) {
		if (object == null) return 0;
		if (object.getClass().isArray()) {
			return arrayHashCode(object);
		}
		else {
			return object.hashCode();
		}
	}

	public static int arrayHashCode(Object object) {
		if (object == null) return 0;
		Class<?> c = object.getClass().getComponentType();
		if (c.isPrimitive()) {
			return primitiveArrayHashCode(object);
		}
		else if (c.isArray() || c == Object.class) {
			return objectArrayHashCode((Object[])(object));
		}
		else {
			return Arrays.hashCode((Object[])(object));
		}
	}

	public static int primitiveArrayHashCode(Object object) {
		if (object == null) return 0;
		Class<?> c = object.getClass();
		return switch (c.getName().charAt(1)) {
			case 'B' -> Arrays.hashCode((byte   [])(object));
			case 'S' -> Arrays.hashCode((short  [])(object));
			case 'I' -> Arrays.hashCode((int    [])(object));
			case 'J' -> Arrays.hashCode((long   [])(object));
			case 'F' -> Arrays.hashCode((float  [])(object));
			case 'D' -> Arrays.hashCode((double [])(object));
			case 'C' -> Arrays.hashCode((char   [])(object));
			case 'Z' -> Arrays.hashCode((boolean[])(object));
			default -> throw new AssertionError(c);
		};
	}

	public static int objectArrayHashCode(Object[] objects) {
		if (objects == null) return 0;
		int hash = 1;
		for (Object object : objects) {
			hash = hash * 31 + hashCode(object);
		}
		return hash;
	}
}