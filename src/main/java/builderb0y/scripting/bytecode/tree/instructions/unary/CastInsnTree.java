package builderb0y.scripting.bytecode.tree.instructions.unary;

import builderb0y.scripting.bytecode.CastingSupport.CasterData;
import builderb0y.scripting.bytecode.CastingSupport.ConstantCaster;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CastInsnTree extends UnaryInsnTree {

	public TypeInfo to;
	public CasterData[] steps;

	public CastInsnTree(InsnTree value, TypeInfo to, CasterData[] steps) {
		super(value);
		this.to = to;
		this.steps = steps;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		ConstantValue constant = this.operand.getConstantValue();
		if (!constant.isConstantOrDynamic()) {
			this.operand.emitBytecode(method);
		}
		for (CasterData step : this.steps) {
			if (step.caster instanceof ConstantCaster constantCaster) {
				if (constant.isConstantOrDynamic()) {
					constant = constant(constantCaster.factory.constantMethod, constant);
				}
				else {
					step.caster.emitBytecode(method);
				}
			}
			else {
				if (constant.isConstantOrDynamic()) {
					constant.emitBytecode(method);
					step.caster.emitBytecode(method);
					constant = ConstantValue.notConstant();
				}
				else {
					step.caster.emitBytecode(method);
				}
			}
		}
		if (constant.isConstantOrDynamic()) {
			constant.emitBytecode(method);
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.to;
	}
}