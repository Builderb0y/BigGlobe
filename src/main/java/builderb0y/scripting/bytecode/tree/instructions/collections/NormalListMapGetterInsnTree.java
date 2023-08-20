package builderb0y.scripting.bytecode.tree.instructions.collections;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.BaseInvokeInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.NormalListMapUpdaterInsnTree;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class NormalListMapGetterInsnTree extends BaseInvokeInsnTree {

	public MethodInfo replacer;
	public String type;

	public NormalListMapGetterInsnTree(
		InsnTree receiver,
		MethodInfo getter,
		InsnTree key,
		MethodInfo replacer,
		String type
	) {
		super(getter, receiver, key);
		this.replacer = replacer;
		this.type = type;
		checkArguments(getter.getInvokeTypes(), this.args);
	}

	public static InsnTree from(InsnTree receiver, MethodInfo getter, InsnTree key, MethodInfo replacer, String type, GetMethodMode mode) {
		return switch (mode) {
			case NORMAL -> new NormalListMapGetterInsnTree(receiver, getter, key, replacer, type);
			case NULLABLE -> new NullableListMapGetterInsnTree(receiver, getter, key, replacer, type);
			case RECEIVER -> new ReceiverListMapGetterInsnTree(receiver, getter, key, replacer, type);
			case NULLABLE_RECEIVER -> new NullableReceiverListMapGetterInsnTree(receiver, getter, key, replacer, type);
		};
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.method.returnType, CastMode.IMPLICIT_THROW);
			return new NormalListMapUpdaterInsnTree(order, true, this.args[0], this.args[1], cast, this.replacer);
		}
		else {
			throw new ScriptParsingException("Updating " + this.type + " not yet implemented", parser.input);
		}
	}
}