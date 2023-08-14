package builderb0y.scripting.bytecode.tree.instructions.fields;

import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.AbstractObjectUpdaterInsnTree.ObjectUpdaterEmitters;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public abstract class AbstractInstanceGetFieldInsnTree implements InsnTree {

	public InsnTree object;
	public FieldInfo field;

	public AbstractInstanceGetFieldInsnTree(InsnTree object, FieldInfo field) {
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

	public abstract InsnTree constructUpdater(UpdateOrder order, boolean isAssignment, ObjectUpdaterEmitters emitters);

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (this.field.isFinal()) {
			throw new ScriptParsingException("Can't write to final field: " + this.field, parser.input);
		}
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.field.type, CastMode.IMPLICIT_THROW);
			return this.constructUpdater(order, true, ObjectUpdaterEmitters.forField(this.object, this.field, cast));
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return this.constructUpdater(order, false, ObjectUpdaterEmitters.forField(this.object, this.field, updater));
		}
	}
}