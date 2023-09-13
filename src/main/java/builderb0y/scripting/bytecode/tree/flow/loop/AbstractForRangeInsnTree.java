package builderb0y.scripting.bytecode.tree.flow.loop;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.util.TypeInfos;

public abstract class AbstractForRangeInsnTree implements InsnTree {

	public String loopName;
	public VariableDeclarationInsnTree variable;
	public boolean ascending;
	public InsnTree lowerBound;
	public boolean lowerBoundInclusive;
	public InsnTree upperBound;
	public boolean upperBoundInclusive;
	public InsnTree step;
	public InsnTree body;

	public AbstractForRangeInsnTree(
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
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}