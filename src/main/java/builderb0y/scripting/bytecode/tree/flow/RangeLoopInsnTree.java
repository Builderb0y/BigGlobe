package builderb0y.scripting.bytecode.tree.flow;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.AddInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RangeLoopInsnTree implements InsnTree {

	public String loopName;
	public VariableDeclarationInsnTree variable;
	public boolean ascending;
	public InsnTree lowerBound;
	public boolean lowerBoundInclusive;
	public InsnTree upperBound;
	public boolean upperBoundInclusive;
	public InsnTree step;
	public InsnTree body;

	public RangeLoopInsnTree(
		String loopName,
		VariableDeclarationInsnTree variable,
		boolean ascending,
		InsnTree lowerBound,
		boolean lowerBoundInclusive,
		InsnTree upperBound,
		boolean upperBoundInclusive,
		InsnTree step,
		InsnTree body
	) {
		this.loopName = loopName;
		this.variable = variable;
		this.ascending = ascending;
		this.lowerBound = lowerBound;
		this.lowerBoundInclusive = lowerBoundInclusive;
		this.upperBound = upperBound;
		this.upperBoundInclusive = upperBoundInclusive;
		this.step = step;
		this.body = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		LabelNode continuePoint = labelNode();
		Scope mainLoop = method.scopes.pushLoop(this.loopName, continuePoint);

		this.variable.emitBytecode(method);
		InsnTree lowerBound = this.lowerBound, upperBound = this.upperBound;
		if (!lowerBound.getConstantValue().isConstant() || !upperBound.getConstantValue().isConstant()) {
			VarInfo lowerBoundVariable = null, upperBoundVariable = null;
			if (!lowerBound.getConstantValue().isConstant()) {
				lowerBoundVariable = method.newVariable("lowerBound", TypeInfos.INT);
			}
			if (!upperBound.getConstantValue().isConstant()) {
				upperBoundVariable = method.newVariable("upperBound", TypeInfos.INT);
			}
			method.scopes.pushScope();
			if (lowerBoundVariable != null) {
				store(lowerBoundVariable, lowerBound).emitBytecode(method);
				lowerBound = load(lowerBoundVariable);
			}
			if (upperBoundVariable != null) {
				store(upperBoundVariable, upperBound).emitBytecode(method);
				upperBound = load(upperBoundVariable);
			}
			method.scopes.popScope();
		}
		InsnTree step = this.step;
		if (!step.getConstantValue().isConstant()) {
			VarInfo stepVariable = method.newVariable("step", TypeInfos.INT);
			store(stepVariable, step).emitBytecode(method);
			step = load(stepVariable);
		}
		if (this.ascending) {
			store(this.variable.variable, lowerBound).emitBytecode(method);
			if (this.lowerBoundInclusive) {
				Label start = label();
				method.node.visitLabel(start);
				if (this.upperBoundInclusive) {
					IntCompareConditionTree.lessThanOrEqual(load(this.variable.variable), upperBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				else {
					IntCompareConditionTree.lessThan(load(this.variable.variable), upperBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				this.body.emitBytecode(method);
				method.node.instructions.add(continuePoint);
				ConstantValue constantStep = step.getConstantValue();
				int stepSize;
				if (constantStep.isConstant() && (stepSize = constantStep.asInt()) >= Short.MIN_VALUE && stepSize <= Short.MAX_VALUE) {
					method.node.visitIincInsn(this.variable.variable.index, stepSize);
				}
				else {
					store(this.variable.variable, new AddInsnTree(load(this.variable.variable), step, IADD)).emitBytecode(method);
				}
				method.node.visitJumpInsn(GOTO, start);
			}
			else {
				method.node.instructions.add(continuePoint);
				ConstantValue constantStep = step.getConstantValue();
				int stepSize;
				if (constantStep.isConstant() && (stepSize = constantStep.asInt()) >= Short.MIN_VALUE && stepSize <= Short.MAX_VALUE) {
					method.node.visitIincInsn(this.variable.variable.index, stepSize);
				}
				else {
					store(this.variable.variable, new AddInsnTree(load(this.variable.variable), step, IADD)).emitBytecode(method);
				}
				if (this.upperBoundInclusive) {
					IntCompareConditionTree.lessThanOrEqual(load(this.variable.variable), upperBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				else {
					IntCompareConditionTree.lessThan(load(this.variable.variable), upperBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				this.body.emitBytecode(method);
				method.node.visitJumpInsn(GOTO, continuePoint.getLabel());
			}
		}
		else {
			store(this.variable.variable, upperBound).emitBytecode(method);
			if (this.upperBoundInclusive) {
				Label start = label();
				method.node.visitLabel(start);
				if (this.lowerBoundInclusive) {
					IntCompareConditionTree.greaterThanOrEqual(load(this.variable.variable), lowerBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				else {
					IntCompareConditionTree.greaterThan(load(this.variable.variable), lowerBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				this.body.emitBytecode(method);
				method.node.instructions.add(continuePoint);
				ConstantValue constantStep = step.getConstantValue();
				int stepSize;
				if (constantStep.isConstant() && (stepSize = -constantStep.asInt()) >= Short.MIN_VALUE && stepSize <= Short.MAX_VALUE) {
					method.node.visitIincInsn(this.variable.variable.index, stepSize);
				}
				else {
					store(this.variable.variable, new SubtractInsnTree(load(this.variable.variable), step, ISUB)).emitBytecode(method);
				}
				method.node.visitJumpInsn(GOTO, start);
			}
			else {
				method.node.instructions.add(continuePoint);
				ConstantValue constantStep = step.getConstantValue();
				int stepSize;
				if (constantStep.isConstant() && (stepSize = -constantStep.asInt()) >= Short.MIN_VALUE && stepSize <= Short.MAX_VALUE) {
					method.node.visitIincInsn(this.variable.variable.index, stepSize);
				}
				else {
					store(this.variable.variable, new SubtractInsnTree(load(this.variable.variable), step, ISUB)).emitBytecode(method);
				}
				if (this.lowerBoundInclusive) {
					IntCompareConditionTree.greaterThanOrEqual(load(this.variable.variable), lowerBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				else {
					IntCompareConditionTree.greaterThan(load(this.variable.variable), lowerBound).emitBytecode(method, null, mainLoop.end.getLabel());
				}
				this.body.emitBytecode(method);
				method.node.visitJumpInsn(GOTO, continuePoint.getLabel());
			}
		}

		method.scopes.popScope();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}