package builderb0y.scripting.bytecode.tree.flow.loop;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.AddInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForIntRangeInsnTree extends AbstractForRangeInsnTree {

	public ForIntRangeInsnTree(
		LoopName loopName,
		VariableDeclarationInsnTree variable,
		boolean ascending,
		InsnTree lowerBound,
		boolean lowerBoundInclusive,
		LazyVarInfo lowerBoundVariable,
		InsnTree upperBound,
		boolean upperBoundInclusive,
		LazyVarInfo upperBoundVariable,
		InsnTree step,
		LazyVarInfo stepVariable,
		InsnTree body
	) {
		super(
			loopName,
			variable,
			ascending,
			lowerBound,
			lowerBoundInclusive,
			lowerBoundVariable,
			upperBound,
			upperBoundInclusive,
			upperBoundVariable,
			step,
			stepVariable,
			body
		);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		LabelNode continuePoint = labelNode();
		Scope mainLoop = method.scopes.pushLoop(this.loopName, continuePoint);

		this.variable.emitBytecode(method);
		InsnTree lowerBound = this.lowerBound, upperBound = this.upperBound;
		if (this.lowerBoundVariable != null || this.upperBoundVariable != null) {
			if (this.lowerBoundVariable != null) method.scopes.addVariable(this.lowerBoundVariable);
			if (this.upperBoundVariable != null) method.scopes.addVariable(this.upperBoundVariable);
			method.scopes.pushScope();
			if (this.lowerBoundVariable != null) {
				store(this.lowerBoundVariable, lowerBound).emitBytecode(method);
				lowerBound = load(this.lowerBoundVariable);
			}
			if (this.upperBoundVariable != null) {
				store(this.upperBoundVariable, upperBound).emitBytecode(method);
				upperBound = load(this.upperBoundVariable);
			}
			method.scopes.popScope();
		}
		InsnTree step = this.step;
		if (this.stepVariable != null) {
			method.scopes.addVariable(this.stepVariable);
			store(this.stepVariable, step).emitBytecode(method);
			step = load(this.stepVariable);
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
					method.node.visitIincInsn(method.scopes.getVariableIndex(this.variable.variable), stepSize);
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
					method.node.visitIincInsn(method.scopes.getVariableIndex(this.variable.variable), stepSize);
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
					method.node.visitIincInsn(method.scopes.getVariableIndex(this.variable.variable), stepSize);
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
					method.node.visitIincInsn(method.scopes.getVariableIndex(this.variable.variable), stepSize);
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
}