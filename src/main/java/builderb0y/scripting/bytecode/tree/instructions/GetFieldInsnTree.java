package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.InstanceFieldUpdateInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class GetFieldInsnTree implements InsnTree {

	public InsnTree object;
	public FieldInfo field;

	public GetFieldInsnTree(InsnTree object, FieldInfo field) {
		this.object = object;
		this.field = field;
	}

	public static GetFieldInsnTree create(InsnTree receiver, FieldInfo field) {
		if (field.isStatic()) throw new IllegalArgumentException("Static field: " + field);
		if (!receiver.getTypeInfo().extendsOrImplements(field.owner)) {
			throw new IllegalArgumentException("Can't get field " + field + " from object of type " + receiver.getTypeInfo());
		}
		return new GetFieldInsnTree(receiver, field);
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
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		if (this.field.isFinal()) {
			throw new ScriptParsingException("Can't write to final field: " + this.field, parser.input);
		}
		if (op == UpdateOp.ASSIGN) {
			return putField(this.object, this.field, rightValue);
		}
		return new InstanceFieldUpdateInsnTree(this.object, this.field, op.createUpdater(parser, this.getTypeInfo(), rightValue));
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return this.object.then(parser, nextStatement);
	}
}