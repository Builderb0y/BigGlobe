package builderb0y.bigglobe.scripting;

import java.lang.invoke.MethodHandles;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.environments.MutableScriptEnvironment2;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantFactory implements MutableScriptEnvironment2.FunctionHandler {

	public final MethodInfo constantMethod, variableMethod;
	public final TypeInfo type;

	public ConstantFactory(Class<?> owner, String name, Class<?> inType, Class<?> outType) {
		this.constantMethod = method(ACC_PUBLIC | ACC_STATIC, owner, name, outType, MethodHandles.Lookup.class, String.class, Class.class, String.class);
		this.variableMethod = method(ACC_PUBLIC | ACC_STATIC, owner, name, outType, inType);
		this.type           = this.variableMethod.returnType;
	}

	@Override
	public InsnTree create(ExpressionParser parser, String name, InsnTree[] arguments) throws ScriptParsingException {
		ScriptEnvironment.checkArgumentCount(parser, name, 1, arguments);
		return this.create(parser, name, arguments[0]);
	}

	public InsnTree create(ExpressionParser parser, String name, InsnTree argument) {
		if (argument.getTypeInfo().simpleEquals(this.variableMethod.paramTypes[0])) {
			if (argument.getConstantValue().isConstant()) {
				return ldc(this.constantMethod, argument.getConstantValue());
			}
			else {
				ScriptLogger.LOGGER.warn("", new ScriptParsingException("Non-constant String input for " + name + "(). This will be worse on performance", parser.input));
				return invokeStatic(this.variableMethod, argument);
			}
		}
		else if (argument.getTypeInfo().simpleEquals(this.type)) {
			return argument;
		}
		else {
			throw new InvalidOperandException("Must be a " + this.variableMethod.paramTypes[0] + " or a " + this.type + "; was " + argument.getTypeInfo());
		}
	}
}