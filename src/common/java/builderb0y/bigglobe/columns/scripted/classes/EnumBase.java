package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Map;
import java.util.Set;

import builderb0y.bigglobe.columns.scripted2.ConstructorInfo;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

/**
differences from {@link Enum}:
values are unordered.
these "enums" don't have an ordinal.
values do not, by default, implement Comparable.
values() does not exist. instead, there are valueSet and valueMap fields.
the restriction that only one "layer" of subclassing is allowed does not apply here.
you can have A extends B extends C extends D extends EnumBase if you want to.
*/
@SuppressWarnings("FinalMethod")
public class EnumBase {

	public static final Info $INFO = new Info();

	public static class Info extends InfoHolder {

		public MethodInfo
			$createSet,
			$createMap;

		public FieldInfo
			name;
	}

	public static final ConstructorInfo $CONSTRUCTOR_INFO = new ConstructorInfo(EnumBase.class);

	public final String name;

	public EnumBase(String name) {
		this.name = name.intern();
	}

	//members of synthetic subclasses:
	//
	//public static final Set<E> valueSet;
	//public static final Map<String, E> valueMap;
	//
	//public static E valueOf(String name) {
	//	return valueMap.get(name);
	//}

	public static Set<EnumBase> $createSet(EnumBase... enums) {
		return Set.of(enums);
	}

	public static Map<String, EnumBase> $createMap(EnumBase... enums) {
		@SuppressWarnings("unchecked")
		Map.Entry<String, EnumBase>[] entries = new Map.Entry[enums.length];
		for (int index = 0; index < enums.length; index++) {
			entries[index] = Map.entry(enums[index].name, enums[index]);
		}
		return Map.ofEntries(entries);
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	protected final Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}