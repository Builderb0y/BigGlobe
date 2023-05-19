package builderb0y.scripting.bytecode.tree.flow.compare;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

/**
a CompareInsnTree which supports exactly 3 outcomes:
greater than, less than, or equal to.
there is no case where the two values are not comparable,
nor is there a case where the two values lack a natural order with respect to each other.
*/
public abstract class IntLikeCompareInsnTree extends CompareInsnTree {

	public IntLikeCompareInsnTree(
		InsnTree left,
		InsnTree right,
		InsnTree lessThan,
		InsnTree equalTo,
		InsnTree greaterThan,
		TypeInfo type
	) {
		super(left, right, lessThan, equalTo, greaterThan, type);
	}
}