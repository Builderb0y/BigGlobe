package builderb0y.bigglobe.classes;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import builderb0y.bigglobe.columns.scripted.ConstructorInfo;
import builderb0y.bigglobe.scripting.wrappers.ConstantMap;
import builderb0y.bigglobe.scripting.wrappers.ConstantSet;
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
you can have A extends B extends C extends D extends ScriptEnum if you want to.
*/
@SuppressWarnings("FinalMethod")
public class ScriptEnum {

	public static final Info $INFO = new Info();

	public static class Info extends InfoHolder {

		public MethodInfo
			$createSet,
			$createMap;

		public FieldInfo
			name;
	}

	public static final ConstructorInfo $CONSTRUCTOR_INFO = new ConstructorInfo(ScriptEnum.class);

	public final String name;

	public ScriptEnum(String name) {
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

	public static Set<ScriptEnum> $createSet(ScriptEnum... enums) {
		return new ConstantSet<>((Object[])(enums));
	}

	public static Map<String, ScriptEnum> $createMap(ScriptEnum... enums) {
		return new ConstantMap<>((ScriptEnum e) -> e.name, Function.identity(), enums);
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