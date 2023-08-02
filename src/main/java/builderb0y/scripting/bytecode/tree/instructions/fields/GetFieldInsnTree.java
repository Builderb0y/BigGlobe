package builderb0y.scripting.bytecode.tree.instructions.fields;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.InstanceFieldUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class GetFieldInsnTree implements InsnTree {

	public InsnTree object;
	public FieldInfo field;

	public GetFieldInsnTree(InsnTree object, FieldInfo field) {
		check(object, field);
		this.object = object;
		this.field = field;
	}

	public static void check(InsnTree object, FieldInfo field) {
		if (field.isStatic()) {
			throw new IllegalArgumentException("Static field: " + field);
		}
		if (!object.getTypeInfo().extendsOrImplements(field.owner)) {
			throw new IllegalArgumentException("Can't get field " + field + " from object of type " + object.getTypeInfo());
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.object.emitBytecode(method);
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
				case VOID -> new InstanceFieldAssignVoidUpdateInsnTree(this.object, this.field, cast);
				case PRE  -> new  InstanceFieldAssignPreUpdateInsnTree(this.object, this.field, cast);
				case POST -> new InstanceFieldAssignPostUpdateInsnTree(this.object, this.field, cast);
			};
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return switch (order) {
				case VOID -> new InstanceFieldVoidUpdateInsnTree(this.object, this.field, updater);
				case PRE  -> new  InstanceFieldPreUpdateInsnTree(this.object, this.field, updater);
				case POST -> new InstanceFieldPostUpdateInsnTree(this.object, this.field, updater);
			};
		}
	}
}