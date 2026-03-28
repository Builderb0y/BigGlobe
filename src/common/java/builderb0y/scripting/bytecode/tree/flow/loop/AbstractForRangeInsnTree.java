package builderb0y.scripting.bytecode.tree.flow.loop;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.util.TypeInfos;

public abstract class AbstractForRangeInsnTree implements InsnTree {

	public LoopName loopName;
	public VariableDeclarationInsnTree variable;
	public boolean ascending;

	public InsnTree lowerBound;
	public boolean lowerBoundInclusive;
	public LazyVarInfo lowerBoundVariable;

	public InsnTree upperBound;
	public boolean upperBoundInclusive;
	public LazyVarInfo upperBoundVariable;

	public InsnTree step;
	public LazyVarInfo stepVariable;
	public InsnTree body;

	public AbstractForRangeInsnTree(
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
		this.loopName = loopName;
		this.variable = variable;
		this.ascending = ascending;
		this.lowerBound = lowerBound;
		this.lowerBoundInclusive = lowerBoundInclusive;
		this.lowerBoundVariable = lowerBoundVariable;
		this.upperBound = upperBound;
		this.upperBoundInclusive = upperBoundInclusive;
		this.upperBoundVariable = upperBoundVariable;
		this.step = step;
		this.stepVariable = stepVariable;
		this.body = body;
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