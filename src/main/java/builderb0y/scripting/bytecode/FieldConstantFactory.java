package builderb0y.scripting.bytecode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FieldConstantFactory extends AbstractConstantFactory {

	public static final MethodInfo ENUM_VALUE_OF = MethodInfo.getMethod(Enum.class, "valueOf");

	public final Map<String, InsnTree> lookup;
	public final UnaryOperator<InsnTree> nonConstantGetter;

	public FieldConstantFactory(TypeInfo outType, Map<String, InsnTree> constantLookup, UnaryOperator<InsnTree> getter) {
		super(TypeInfos.STRING, outType);
		this.lookup = constantLookup;
		this.nonConstantGetter = getter;
	}

	public static <E extends Enum<E>> FieldConstantFactory forEnum(Class<E> enumClass) {
		E[] enums = enumClass.getEnumConstants();
		Map<String, InsnTree> lookup = new HashMap<>(enums.length);
		for (E enum_ : enums) {
			lookup.put(enum_.name(), getStatic(FieldInfo.getField(enumClass, enum_.name())));
		}
		return new FieldConstantFactory(type(enumClass), lookup, (InsnTree tree) -> {
			return invokeStatic(ENUM_VALUE_OF, ldc(type(enumClass)), tree);
		});
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public InsnTree createConstant(ConstantValue constant) {
		return this.lookup.get(constant.asJavaObject());
	}

	@Override
	public InsnTree createNonConstant(InsnTree tree) {
		return this.nonConstantGetter.apply(tree);
	}
}