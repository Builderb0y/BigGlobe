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

	public static final int
		CLIENT   = 1 << 0,
		NULLABLE = 1 << 1;

	public static int flags(ExpressionParser parser, boolean nullable) {
		int flags = 0;
		if ((parser.flags & ExpressionParser.CLIENT) != 0) flags |= CLIENT;
		if (nullable) flags |= NULLABLE;
		return flags;
	}

	public final TypeInfo inType, outType;

	public AbstractConstantFactory(TypeInfo inType, TypeInfo outType) {
		this.inType = inType;
		this.outType = outType;
	}

	@Override
	public @Nullable CastResult create(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		if (arguments.length != 1) return null;
		return this.create(parser, arguments[0], false, false);
	}

	public CastResult create(ExpressionParser parser, InsnTree argument, boolean implicit, boolean nullable) {
		if (argument.getTypeInfo().equals(this.inType)) {
			if (argument.getConstantValue().isConstant()) {
				return new CastResult(this.createConstant(argument.getConstantValue(), flags(parser, nullable)), true);
			}
			else {
				if (implicit) ScriptLogger.LOGGER.warn(ScriptParsingException.appendContext("Non-constant " + this.inType.getClassName() + " input for implicit cast to " + this.outType.getClassName() + ". This will be worse on performance. Use an explicit cast to suppress this warning.", parser.input));
				return new CastResult(this.createNonConstant(argument, flags(parser, nullable)), true);
			}
		}
		else if (argument.getTypeInfo().equals(this.outType)) {
			return new CastResult(argument, false);
		}
		else {
			throw new InvalidOperandException("Must be a " + this.inType.getClassName() + " or a " + this.outType.getClassName() + "; was " + argument.getTypeInfo());
		}
	}

	public abstract InsnTree createConstant(ConstantValue constant, int flags);

	public abstract InsnTree createNonConstant(InsnTree tree, int flags);
}