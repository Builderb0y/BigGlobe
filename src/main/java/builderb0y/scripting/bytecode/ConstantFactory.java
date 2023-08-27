package builderb0y.scripting.bytecode;

import java.lang.StackWalker.Option;
import java.lang.invoke.MethodHandles;

import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantFactory extends AbstractConstantFactory {

	public static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	public final MethodInfo constantMethod, variableMethod;

	public ConstantFactory(Class<?> owner, String name, Class<?> inType, Class<?> outType) {
		super(type(inType), type(outType));
		this.constantMethod = MethodInfo.findMethod(owner, name, outType, MethodHandles.Lookup.class, String.class, Class.class, inType);
		this.variableMethod = MethodInfo.findMethod(owner, name, outType, inType);
	}

	public ConstantFactory(MethodInfo constantMethod, MethodInfo variableMethod, TypeInfo inType, TypeInfo outType) {
		super(inType, outType);
		this.constantMethod = constantMethod;
		this.variableMethod = variableMethod;
	}

	/**
	factory method for the most common case,
	where the owner is the caller class, the name is "of",
	inType is String.class, and outType is also the caller class.
	*/
	public static ConstantFactory autoOfString() {
		Class<?> caller = STACK_WALKER.getCallerClass();
		return new ConstantFactory(caller, "of", String.class, caller);
	}

	@Override
	public InsnTree createConstant(ConstantValue constant) {
		return ldc(this.constantMethod, constant);
	}

	@Override
	public InsnTree createNonConstant(InsnTree tree) {
		return invokeStatic(this.variableMethod, tree);
	}
}