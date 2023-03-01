package builderb0y.bigglobe.trees.decoration;

import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.branches.BranchConfig;

public interface BranchDecorator {

	public abstract void decorate(TreeGenerator generator, BranchConfig branch);
}