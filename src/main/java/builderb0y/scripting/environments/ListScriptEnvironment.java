package builderb0y.scripting.environments;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ListScriptEnvironment extends NoFunctionalStuffClassScriptEnvironment {

	public ListScriptEnvironment() {
		super(List.class);
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		if (name.isEmpty() && receiver.getTypeInfo().extendsOrImplements(this.typeInfo)) {
			InsnTree index = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
			return automaticCast(new ListGetInsnTree(receiver, index));
		}
		return super.getMethod(parser, receiver, name, arguments);
	}

	public static class ListGetInsnTree extends InvokeInsnTree {

		public static final MethodInfo
			GET = method(ACC_PUBLIC | ACC_INTERFACE | ACC_PURE, List.class, "get", Object.class, int.class),
			SET = method(ACC_PUBLIC | ACC_INTERFACE, List.class, "set", void.class, int.class, Object.class);

		public ListGetInsnTree(InsnTree list, InsnTree index) {
			super(INVOKEINTERFACE, list, GET, index);
		}

		@Override
		public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
			if (op == UpdateOp.ASSIGN) {
				return invokeInterface(this.receiver, SET, this.args[0], rightValue.cast(parser, TypeInfos.OBJECT, CastMode.IMPLICIT_THROW)).cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
			}
			throw new ScriptParsingException("Updating List not yet implemented", parser.input);
		}
	}
}