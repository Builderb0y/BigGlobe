package builderb0y.scripting.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public class CollectionTransformer {

	public static <T_FromElement, T_ToElement> T_ToElement[] convertArray(
		T_FromElement[] from,
		IntFunction<T_ToElement[]> toConstructor,
		Function<? super T_FromElement, ? extends T_ToElement> mapper
	) {
		if (from == null) return null;
		int length = from.length;
		T_ToElement[] to = toConstructor.apply(length);
		for (int index = 0; index < length; index++) {
			to[index] = mapper.apply(from[index]);
		}
		return to;
	}

	public static <
		T_FromElement,
		T_ToElement,
		T_FromCollection extends Collection<T_FromElement>,
		T_ToCollection extends Collection<T_ToElement>
	>
	T_ToCollection convertCollection(
		T_FromCollection from,
		IntFunction<T_ToCollection> toConstructor,
		Function<? super T_FromElement, ? extends T_ToElement> mapper
	) {
		if (from == null) return null;
		int size = from.size();
		T_ToCollection to = toConstructor.apply(size);
		for (T_FromElement element : from) {
			to.add(mapper.apply(element));
		}
		return to;
	}

	public static <
		T_Key,
		T_FromValue,
		T_ToValue,
		T_FromMap extends Map<T_Key, T_FromValue>,
		T_ToMap extends Map<T_Key, T_ToValue>
	>
	T_ToMap convertMap(
		T_FromMap fromMap,
		IntFunction<T_ToMap> toConstructor,
		Function<? super T_FromValue, ? extends T_ToValue> mapper
	) {
		if (fromMap == null) return null;
		T_ToMap toMap = toConstructor.apply(fromMap.size());
		for (Map.Entry<T_Key, T_FromValue> entry : fromMap.entrySet()) {
			toMap.put(entry.getKey(), mapper.apply(entry.getValue()));
		}
		return toMap;
	}

	public static <
		T_Key,
		T_FromValue,
		T_ToValue,
		T_FromMap extends Map<T_Key, T_FromValue>,
		T_ToMap extends Map<T_Key, T_ToValue>
	>
	T_ToMap convertMapWithKeys(
		T_FromMap fromMap,
		IntFunction<T_ToMap> toConstructor,
		BiFunction<? super T_Key, ? super T_FromValue, ? extends T_ToValue> mapper
	) {
		if (fromMap == null) return null;
		T_ToMap toMap = toConstructor.apply(fromMap.size());
		for (Map.Entry<T_Key, T_FromValue> entry : fromMap.entrySet()) {
			toMap.put(entry.getKey(), mapper.apply(entry.getKey(), entry.getValue()));
		}
		return toMap;
	}
}