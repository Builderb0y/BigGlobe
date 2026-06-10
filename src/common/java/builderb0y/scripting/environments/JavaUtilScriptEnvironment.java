package builderb0y.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.random.RandomGenerator;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.ArrayWrapper;
import builderb0y.bigglobe.scripting.wrappers.ConstantMap;
import builderb0y.bigglobe.scripting.wrappers.ConstantSet;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.NullConstantValue;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class JavaUtilScriptEnvironment {

	public static final MethodInfo
		CONSTANT_LIST = MethodInfo.inCaller("constantList"),
		CONSTANT_MAP  = MethodInfo.inCaller("constantMap"),
		CONSTANT_SET  = MethodInfo.inCaller("constantSet");

	public static void swap(Object[] array, int index1, int index2) {
		Object tmp = array[index1];
		array[index1] = array[index2];
		array[index2] = tmp;
	}

	/**
	mostly a copy-paste of {@link Collections#shuffle(List, Random)},
	but adapted to work with a {@link RandomGenerator} instead of a {@link Random}.
	an overload in Collections which uses a RandomGenerator was added in java 21.
	*/
	public static <T> void shuffle(List<T> list, RandomGenerator random) {
		int size = list.size();
		if (size < 5 || list instanceof RandomAccess) {
			for (int index = size; index > 1; index--) {
				Collections.swap(list, index - 1, random.nextInt(index));
			}
		}
		else {
			@SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
			T[] array = (T[])(list.toArray());
			for (int index = size; index > 1; index--) {
				swap(array, index - 1, random.nextInt(index));
			}
			ListIterator<T> iterator = list.listIterator();
			for (T element : array) {
				iterator.next();
				iterator.set(element);
			}
		}
	}

	public static <T> void shuffle(List<T> list, long seed) {
		int size = list.size();
		if (size < 5 || list instanceof RandomAccess) {
			for (int index = size; index > 1; index--) {
				Collections.swap(list, index - 1, Permuter.nextBoundedInt(seed += Permuter.PHI64, index));
			}
		}
		else {
			@SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
			T[] array = (T[])(list.toArray());
			for (int index = size; index > 1; index--) {
				swap(array, index - 1, Permuter.nextBoundedInt(seed += Permuter.PHI64, index));
			}
			ListIterator<T> iterator = list.listIterator();
			for (T element : array) {
				iterator.next();
				iterator.set(element);
			}
		}
	}

	/** returns void instead of old value. */
	public static <K, V> void setEntryValue(Map.Entry<K, V> entry, V value) {
		entry.setValue(value);
	}

	public static ArrayWrapper<Object> constantList(MethodHandles.Lookup caller, String name, Class<?> type, Object... contents) {
		return new ArrayWrapper<>(deflate(contents));
	}

	public static ConstantMap<Object, Object> constantMap(MethodHandles.Lookup caller, String name, Class<?> type, Object... arguments) {
		return new ConstantMap<>(deflate(arguments));
	}

	public static ConstantSet<Object> constantSet(MethodHandles.Lookup caller, String name, Class<?> type, Object... args) {
		return new ConstantSet<>(deflate(args));
	}

	public static ConstantValue[] inflate(ConstantValue[] args) {
		int length = args.length;
		ConstantValue[] result = new ConstantValue[(length * (Long.SIZE / 3 + 1) + (Long.SIZE / 3 - 1)) / (Long.SIZE / 3)];
		int writeIndex = 0;
		for (int baseIndex = 0; baseIndex < length; baseIndex += Long.SIZE / 3) {
			long types = 0L;
			int typeIndex = writeIndex++;
			for (int offset = 0; offset < Long.SIZE / 3; offset++) {
				int index = baseIndex + offset;
				if (index >= length) {
					types >>>= 3;
				}
				else {
					int type = inflateOne(args[index]);
					result[writeIndex++] = args[index];
					types = (types >>> 3) | (((long)(type)) << (Long.SIZE - 4));
				}
			}
			result[typeIndex] = constant(types);
		}
		assert writeIndex == result.length;
		return result;
	}

	public static Object[] deflate(Object[] args) {
		int length = args.length;
		int writeIndex = 0;
		outer:
		for (int baseIndex = 0; baseIndex < length; baseIndex += Long.SIZE / 3 + 1) {
			long types = (Long)(args[baseIndex]);
			for (int offset = 1; offset < Long.SIZE / 3 + 1; offset++) {
				int index = baseIndex + offset;
				if (index >= length) break outer;
				args[writeIndex++] = deflateOne(((int)(types)) & 7, args[index]);
				types >>>= 3;
			}
		}
		return Arrays.copyOf(args, writeIndex);
	}

	public static int inflateOne(ConstantValue value) {
		return switch (value.getTypeInfo().getSort()) {
			case BOOLEAN -> 1;
			case BYTE -> 2;
			case SHORT -> 3;
			case CHAR -> 4;
			case OBJECT, ARRAY -> value instanceof NullConstantValue ? 5 : 0;
			default -> 0;
		};
	}

	public static Object deflateOne(int type, Object value) {
		return switch (type) {
			case 0 -> value;
			case 1 -> Boolean.valueOf(((Integer)(value)).intValue() != 0);
			case 2 -> Byte.valueOf(((Integer)(value)).byteValue());
			case 3 -> Short.valueOf(((Integer)(value)).shortValue());
			case 4 -> Character.valueOf((char)(((Integer)(value)).intValue()));
			case 5 -> null;
			default -> throw new IllegalArgumentException(value + " cannot be cast with type " + type);
		};
	}
}