package builderb0y.scripting.bytecode;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public abstract class AbstractConstantFactory implements FunctionHandler {

	public final TypeInfo inType, outType;

	public AbstractConstantFactory(TypeInfo inType, TypeInfo outType) {
		this.inType = inType;
		this.outType = outType;
	}

	@Override
	public @Nullable CastResult create(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		if (arguments.length != 1) return null;
		return this.create(parser, arguments[0], false);
	}

	public CastResult create(ExpressionParser parser, InsnTree argument, boolean implicit) {
		if (argument.getTypeInfo().equals(this.inType)) {
			if (argument.getConstantValue().isConstant()) {
				return new CastResult(this.createConstant(argument.getConstantValue()), true);
			}
			else {
				if (implicit) ScriptLogger.LOGGER.warn("Non-constant " + this.inType.getClassName() + " input for implicit cast to " + this.outType.getClassName() + ". This will be worse on performance. Use an explicit cast to suppress this warning. " + ScriptParsingException.appendContext(parser.input));
				return new CastResult(this.createNonConstant(argument), true);
			}
		}
		else if (argument.getTypeInfo().equals(this.outType)) {
			return new CastResult(argument, false);
		}
		else {
			throw new InvalidOperandException("Must be a " + this.inType.getClassName() + " or a " + this.outType.getClassName() + "; was " + argument.getTypeInfo());
		}
	}

	public abstract InsnTree createConstant(ConstantValue constant);

	public abstract InsnTree createNonConstant(InsnTree tree);
}