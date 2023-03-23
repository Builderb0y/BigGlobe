package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.StaticFieldUpdateInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class GetStaticInsnTree implements InsnTree {

	public FieldInfo field;

	public GetStaticInsnTree(FieldInfo field) {
		this.field = field;
	}

	public static GetStaticInsnTree create(FieldInfo field) {
		if (!field.isStatic()) throw new IllegalArgumentException("Non-static field: " + field);
		return new GetStaticInsnTree(field);
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
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		if (this.field.isFinal()) {
			throw new ScriptParsingException("Can't write to final field: " + this.field, parser.input);
		}
		if (op == UpdateOp.ASSIGN) {
			return putStatic(this.field, rightValue);
		}
		return new StaticFieldUpdateInsnTree(this.field, op.createUpdater(parser, this.getTypeInfo(), rightValue));
	}
}