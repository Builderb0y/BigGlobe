package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.IncrementUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.VariableUpdaterInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.VariableUpdaterInsnTree.VariableUpdaterEmitters;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class LoadInsnTree implements InsnTree {

	public LazyVarInfo variable;

	public LoadInsnTree(LazyVarInfo variable) {
		this.variable = variable;
	}

	public LazyVarInfo variable() {
		return this.variable;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.variable.type, CastMode.IMPLICIT_THROW);
			return new VariableUpdaterInsnTree(order, true, VariableUpdaterEmitters.forLazyVariable(this.variable, cast));
		}
		if ((op == UpdateOp.ADD || op == UpdateOp.SUBTRACT) && this.getTypeInfo().getSort() == Sort.INT) {
			ConstantValue constant = rightValue.getConstantValue();
			int increment;
			if (
				constant.isConstant() &&
				constant.getTypeInfo().isSingleWidthInt() &&
				(increment = op == UpdateOp.ADD ? constant.asInt() : -constant.asInt()) == (short)(increment)
			) {
				return new IncrementUpdateInsnTree(order, this.variable, increment);
			}
		}
		InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
		return new VariableUpdaterInsnTree(order, false, VariableUpdaterEmitters.forLazyVariable(this.variable, updater));
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