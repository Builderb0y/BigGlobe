package builderb0y.scripting.bytecode.tree.instructions.fields;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.VariableUpdaterInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.VariableUpdaterInsnTree.VariableUpdaterEmitters;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class GetStaticInsnTree implements InsnTree {

	public FieldInfo field;

	public GetStaticInsnTree(FieldInfo field) {
		check(field);
		this.field = field;
	}

	public static void check(FieldInfo field) {
		if (!field.isStatic()) {
			throw new IllegalArgumentException("Non-static field: " + field);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.field.emitGet(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.field.type;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (this.field.isFinal()) {
			throw new ScriptParsingException("Can't modify final field: " + this.field, parser.input);
		}
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.getTypeInfo(), CastMode.IMPLICIT_THROW);
			return new VariableUpdaterInsnTree(order, true, VariableUpdaterEmitters.forField(this.field, cast));
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return new VariableUpdaterInsnTree(order, false, VariableUpdaterEmitters.forField(this.field, updater));
		}
	}
}