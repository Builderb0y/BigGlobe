package builderb0y.scripting.bytecode;

import java.lang.StackWalker.Option;
import java.lang.invoke.MethodHandles;

import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantFactory implements MutableScriptEnvironment.FunctionHandler {

	public static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	public final MethodInfo constantMethod, variableMethod;
	public final TypeInfo inType, outType;

	public ConstantFactory(Class<?> owner, String name, Class<?> inType, Class<?> outType) {
		this.constantMethod = MethodInfo.findMethod(owner, name, outType, MethodHandles.Lookup.class, String.class, Class.class, inType);
		this.variableMethod = MethodInfo.findMethod(owner, name, outType, inType);
		this.        inType = type( inType);
		this.       outType = type(outType);
	}

	public ConstantFactory(MethodInfo constantMethod, MethodInfo variableMethod, TypeInfo inType, TypeInfo outType) {
		this.constantMethod = constantMethod;
		this.variableMethod = variableMethod;
		this.inType = inType;
		this.outType = outType;
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
	public CastResult create(ExpressionParser parser, String name, InsnTree[] arguments) throws ScriptParsingException {
		if (arguments.length != 1) return null;
		return this.create(parser, arguments[0], false);
	}

	public CastResult create(ExpressionParser parser, InsnTree argument, boolean implicit) {
		if (argument.getTypeInfo().equals(this.inType)) {
			if (argument.getConstantValue().isConstant()) {
				return new CastResult(ldc(this.constantMethod, argument.getConstantValue()), true);
			}
			else {
				if (implicit) ScriptLogger.LOGGER.warn("Non-constant " + this.inType.getClassName() + " input for implicit cast to " + this.outType.getClassName() + ". This will be worse on performance. Use an explicit cast to suppress this warning. " + ScriptParsingException.appendContext(parser.input));
				return new CastResult(invokeStatic(this.variableMethod, argument), true);
			}
		}
		else if (argument.getTypeInfo().equals(this.outType)) {
			return new CastResult(argument, false);
		}
		else {
			throw new InvalidOperandException("Must be a " + this.inType.getClassName() + " or a " + this.outType.getClassName() + "; was " + argument.getTypeInfo());
		}
	}

	public InsnTree create(ConstantValue constant) {
		return ldc(this.constantMethod, constant);
	}
}