package builderb0y.scripting.bytecode.tree.flow.compare;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

/**
a CompareInsnTree which supports 4 outcomes:
greater than, less than, equal to, and incomparable.
the incomparable case may happen when one of the values is null or NaN.
*/
public abstract class FloatLikeCompareInsnTree extends CompareInsnTree {

	public InsnTree incomparable;

	public FloatLikeCompareInsnTree(
		InsnTree left,
		InsnTree right,
		InsnTree lessThan,
		InsnTree equalTo,
		InsnTree greaterThan,
		InsnTree incomparable,
		TypeInfo type
	) {
		super(left, right, lessThan, equalTo, greaterThan, type);
		this.incomparable = incomparable;
	}
}