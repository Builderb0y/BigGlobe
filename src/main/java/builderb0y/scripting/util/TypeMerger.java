package builderb0y.scripting.util;

import java.util.*;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.TypeInfo;

public class TypeMerger {

	public static Set<TypeInfo> computeAllCommonTypes(TypeInfo... types) {
		int length = types.length;
		if (length == 0) return Collections.emptySet();
		if (length == 1) return types[0].getAllAssignableTypes();
		Set<TypeInfo> set = new HashSet<>(types[0].getAllAssignableTypes());
		for (int index = 1; index < length; index++) {
			set.retainAll(types[index].getAllAssignableTypes());
		}
		return set;
	}

	public static TypeInfo computeMostSpecificType(TypeInfo... types) {
		if (types.length == 0) throw new NoSuchElementException();
		if (types.length == 1) return types[0];

		if (tryAllSame(types)) return types[0];
		if (tryVoid(types)) return TypeInfos.VOID;

		TypeInfo primitive = tryPrimitive(types);
		if (primitive != null) return primitive;

		types = wrapAll(types);
		TypeInfo[] array = extractComponents(types);
		if (array != null) return TypeInfo.makeArray(computeMostSpecificType(array));

		return tryObject(types);
	}

	public static Set<TypeInfo> computeMostSpecificTypes(TypeInfo... types) {
		if (types.length == 0) throw new NoSuchElementException();
		if (types.length == 1) return Collections.singleton(types[0]);

		if (tryAllSame(types)) return Collections.singleton(types[0]);
		if (tryVoid(types)) return Collections.singleton(TypeInfos.VOID);

		TypeInfo primitive = tryPrimitive(types);
		if (primitive != null) return Collections.singleton(primitive);

		types = wrapAll(types);
		TypeInfo[] array = extractComponents(types);
		if (array != null) return CollectionTransformer.convertCollection(computeMostSpecificTypes(array), HashSet::new, TypeInfo::makeArray);

		return tryObjects(types);
	}

	//////////////////////////////// internal logic ////////////////////////////////

	public static final Object2IntMap<TypeInfo> PRIMITIVE_WIDENING = new Object2IntOpenHashMap<>(8);
	static {
		PRIMITIVE_WIDENING.put(TypeInfos.BOOLEAN, 0);
		PRIMITIVE_WIDENING.put(TypeInfos.BYTE,    1);
		PRIMITIVE_WIDENING.put(TypeInfos.CHAR,    2);
		PRIMITIVE_WIDENING.put(TypeInfos.SHORT,   3);
		PRIMITIVE_WIDENING.put(TypeInfos.INT,     4);
		PRIMITIVE_WIDENING.put(TypeInfos.LONG,    5);
		PRIMITIVE_WIDENING.put(TypeInfos.FLOAT,   6);
		PRIMITIVE_WIDENING.put(TypeInfos.DOUBLE,  7);
	}
	public static final Object2ObjectMap<TypeInfo, TypeInfo> PRIMITIVE_WRAPPING = new Object2ObjectOpenHashMap<>(8);
	static {
		PRIMITIVE_WRAPPING.put(TypeInfos.BOOLEAN, TypeInfos.BOOLEAN_WRAPPER);
		PRIMITIVE_WRAPPING.put(TypeInfos.BYTE,    TypeInfos.   BYTE_WRAPPER);
		PRIMITIVE_WRAPPING.put(TypeInfos.CHAR,    TypeInfos.   CHAR_WRAPPER);
		PRIMITIVE_WRAPPING.put(TypeInfos.SHORT,   TypeInfos.  SHORT_WRAPPER);
		PRIMITIVE_WRAPPING.put(TypeInfos.INT,     TypeInfos.    INT_WRAPPER);
		PRIMITIVE_WRAPPING.put(TypeInfos.LONG,    TypeInfos.   LONG_WRAPPER);
		PRIMITIVE_WRAPPING.put(TypeInfos.FLOAT,   TypeInfos.  FLOAT_WRAPPER);
		PRIMITIVE_WRAPPING.put(TypeInfos.DOUBLE,  TypeInfos. DOUBLE_WRAPPER);
	}

	public static boolean tryAllSame(TypeInfo... types) {
		TypeInfo type = types[0];
		for (int index = 1, length = types.length; index < length; index++) {
			if (!types[index].equals(type)) return false;
		}
		return true;
	}

	public static boolean tryVoid(TypeInfo... types) {
		for (TypeInfo type : types) {
			if (type.isVoid()) {
				return true;
			}
		}
		return false;
	}

	public static @Nullable TypeInfo tryPrimitive(TypeInfo... types) {
		TypeInfo best = null;
		int bestValue = -1;
		for (TypeInfo type : types) {
			if (type.isPrimitive()) {
				int value = PRIMITIVE_WIDENING.getInt(type);
				if (value > bestValue) {
					best = type;
					bestValue = value;
				}
			}
			else {
				return null;
			}
		}
		return best;
	}

	public static TypeInfo[] wrapAll(TypeInfo[] types) {
		TypeInfo[] result = types;
		for (int index = 0, length = types.length; index < length; index++) {
			TypeInfo newType = PRIMITIVE_WRAPPING.get(types[index]);
			if (newType != null) {
				if (result == types) result = result.clone();
				result[index] = newType;
			}
		}
		return result;
	}

	public static TypeInfo @Nullable [] extractComponents(TypeInfo[] types) {
		for (TypeInfo type : types) {
			if (!type.isArray() || type.componentType.isPrimitive()) return null;
		}
		return CollectionTransformer.convertArray(types, TypeInfo.ARRAY_FACTORY, type -> type.componentType);
	}

	public static Set<TypeInfo> tryObjects(TypeInfo[] types) {
		assert types.length > 1;
		Set<TypeInfo> set = computeAllCommonTypes(types);
		//it is safe to modify the set here for the sole reason that
		//computeAllCommonTypes() allocates a new HashSet when types.length > 1.
		set.remove(TypeInfos.VOID);
		set.remove(TypeInfos.OBJECT);
		for (TypeInfo placeholder : set.toArray(new TypeInfo[set.size()])) {
			set.remove(placeholder.superClass);
			for (TypeInfo anInterface : placeholder.superInterfaces) {
				set.remove(anInterface);
			}
		}
		if (set.isEmpty()) set.add(TypeInfos.OBJECT);
		return set;
	}

	public static TypeInfo tryObject(TypeInfo[] types) {
		Set<TypeInfo> set = tryObjects(types);
		return (
			set.size() == 1
			? set.iterator().next()
			: set.stream().max(
				Comparator.comparingInt(
					(TypeInfo placeholder) -> placeholder.type.isInterface ? 0 : 1
				)
				.thenComparing(TypeInfo::getInternalName)
			)
			.orElseThrow()
		);
	}
}