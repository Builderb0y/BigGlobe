package builderb0y.scripting.environments;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MapScriptEnvironment extends NoFunctionalStuffClassScriptEnvironment {

	public MapScriptEnvironment() {
		super(Map.class);
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		if (name.isEmpty() && receiver.getTypeInfo().extendsOrImplements(this.typeInfo)) {
			InsnTree key = ScriptEnvironment.castArgument(parser, "", TypeInfos.OBJECT, CastMode.IMPLICIT_THROW, arguments);
			return automaticCast(new MapGetInsnTree(receiver, key));
		}
		return super.getMethod(parser, receiver, name, arguments);
	}

	public static class MapGetInsnTree extends InvokeInsnTree {

		public static final MethodInfo
			GET = method(ACC_PUBLIC | ACC_INTERFACE | ACC_PURE, Map.class, "get", Object.class, Object.class),
			PUT = method(ACC_PUBLIC | ACC_INTERFACE, Map.class, "put", Object.class, Object.class, Object.class);

		public MapGetInsnTree(InsnTree map, InsnTree key) {
			super(INVOKEINTERFACE, map, GET, key);
		}

		@Override
		public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
			if (op == UpdateOp.ASSIGN) {
				return invokeInterface(this.receiver, PUT, this.args[0], rightValue.cast(parser, TypeInfos.OBJECT, CastMode.IMPLICIT_THROW)).cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
			}
			throw new ScriptParsingException("Updating Map not yet implemented", parser.input);
		}
	}
}