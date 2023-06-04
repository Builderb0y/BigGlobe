package builderb0y.scripting.bytecode.tree.flow.compare;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public abstract class CompareInsnTree implements InsnTree {

	public InsnTree left, right;
	public InsnTree lessThan, equalTo, greaterThan;
	public TypeInfo type;

	public CompareInsnTree(
		InsnTree left,
		InsnTree right,
		InsnTree lessThan,
		InsnTree equalTo,
		InsnTree greaterThan,
		TypeInfo type
	) {
		this.left        = left;
		this.right       = right;
		this.lessThan    = lessThan;
		this.equalTo     = equalTo;
		this.greaterThan = greaterThan;
		this.type        = type;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}
}