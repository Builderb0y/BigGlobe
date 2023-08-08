package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.VariableUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class LoadInsnTree implements InsnTree {

	public VarInfo variable;

	public LoadInsnTree(VarInfo variable) {
		this.variable = variable;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (this.variable.isFinal) {
			throw new ScriptParsingException("Can't modify final variable", parser.input);
		}
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.variable.type, CastMode.IMPLICIT_THROW);
			return switch (order) {
				case VOID -> new VariableAssignVoidUpdateInsnTree(this.variable, cast);
				case PRE  -> new  VariableAssignPreUpdateInsnTree(this.variable, cast);
				case POST -> new VariableAssignPostUpdateInsnTree(this.variable, cast);
			};
		}
		if ((op == UpdateOp.ADD || op == UpdateOp.SUBTRACT) && this.getTypeInfo().getSort() == Sort.INT) {
			ConstantValue constant = rightValue.getConstantValue();
			int increment;
			if (
				constant.isConstant() &&
				constant.getTypeInfo().isSingleWidthInt() &&
				(increment = op == UpdateOp.ADD ? constant.asInt() : -constant.asInt()) == (short)(increment)
			) {
				return switch (order) {
					case VOID -> new VariableIncrementVoidUpdateInsnTree(this.variable, increment);
					case PRE  -> new  VariableIncrementPreUpdateInsnTree(this.variable, increment);
					case POST -> new VariableIncrementPostUpdateInsnTree(this.variable, increment);
				};
			}
		}
		InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
		return switch (order) {
			case VOID -> new VariableVoidUpdateInsnTree(this.variable, updater);
			case PRE  -> new  VariablePreUpdateInsnTree(this.variable, updater);
			case POST -> new VariablePostUpdateInsnTree(this.variable, updater);
		};
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.variable.emitLoad(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.variable.type;
	}
}