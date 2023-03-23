package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.VariableUpdateInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class LoadInsnTree implements InsnTree {

	public VarInfo variable;

	public LoadInsnTree(VarInfo variable) {
		this.variable = variable;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		switch (op) {
			case ASSIGN -> {
				return store(this.variable, rightValue.cast(parser, this.variable.type, CastMode.IMPLICIT_THROW));
			}
			case ADD -> {
				if (this.getTypeInfo().getSort() == Sort.INT) {
					ConstantValue constant = rightValue.getConstantValue();
					if (constant.isConstant() && constant.getTypeInfo().isSingleWidthInt() && constant.asInt() == constant.asShort()) {
						return inc(this.variable, constant.asInt());
					}
				}
			}
			case SUBTRACT -> {
				if (this.getTypeInfo().getSort() == Sort.INT) {
					ConstantValue constant = rightValue.getConstantValue();
					if (constant.isConstant() && constant.getTypeInfo().isSingleWidthInt() && -constant.asInt() == -constant.asShort()) {
						return inc(this.variable, -constant.asInt());
					}
				}
			}
		}
		return new VariableUpdateInsnTree(this.variable, op.createUpdater(parser, this.getTypeInfo(), rightValue));
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.variable.emitLoad(method.node);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.variable.type;
	}
}