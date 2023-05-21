package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.StaticFieldUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

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
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (this.field.isFinal()) {
			throw new ScriptParsingException("Can't write to final field: " + this.field, parser.input);
		}
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.field.type, CastMode.IMPLICIT_THROW);
			return switch (order) {
				case VOID -> new StaticFieldAssignVoidUpdateInsnTree(this.field, cast);
				case PRE  -> new  StaticFieldAssignPreUpdateInsnTree(this.field, cast);
				case POST -> new StaticFieldAssignPostUpdateInsnTree(this.field, cast);
			};
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return switch (order) {
				case VOID -> new StaticFieldVoidUpdateInsnTree(this.field, updater);
				case PRE  -> new  StaticFieldPreUpdateInsnTree(this.field, updater);
				case POST -> new StaticFieldPostUpdateInsnTree(this.field, updater);
			};
		}
	}
}