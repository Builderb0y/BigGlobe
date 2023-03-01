package builderb0y.bigglobe.trees.decoration;

import java.util.ArrayList;
import java.util.List;

public class DecoratorConfig {

	public static final TrunkDecorator[] EMPTY_TRUNK_ARRAY = {};
	public static final TrunkLayerDecorator[] EMPTY_TRUNK_LAYER_ARRAY = {};
	public static final BranchDecorator[] EMPTY_BRANCH_ARRAY = {};
	public static final BlockDecorator[] EMPTY_BLOCK_ARRAY = {};


	public final TrunkDecorator[] trunk;
	public final TrunkLayerDecorator[] trunkLayer;
	public final BlockDecorator[] trunkBlock;
	public final BranchDecorator[] branch;
	public final BlockDecorator[] branchBlock;
	public final BlockDecorator[] leafBlock;

	public DecoratorConfig(
		TrunkDecorator[] trunk,
		TrunkLayerDecorator[] trunkLayer,
		BlockDecorator[] trunkBlock,
		BranchDecorator[] branch,
		BlockDecorator[] branchBlock,
		BlockDecorator[] leafBlock
	) {
		this.trunk       = trunk;
		this.trunkLayer  = trunkLayer;
		this.trunkBlock  = trunkBlock;
		this.branch      = branch;
		this.branchBlock = branchBlock;
		this.leafBlock   = leafBlock;
	}

	public static class Builder {

		public List<TrunkDecorator> trunk;
		public List<TrunkLayerDecorator> trunkLayer;
		public List<BlockDecorator> trunkBlock;
		public List<BranchDecorator> branch;
		public List<BlockDecorator> branchBlock;
		public List<BlockDecorator> leafBlock;

		public static <T> List<T> add(List<T> list, T element) {
			if (list == null) list = new ArrayList<>(2);
			list.add(element);
			return list;
		}

		public Builder trunk(TrunkDecorator trunk) {
			if (this.trunk == null) this.trunk = new ArrayList<>(2);
			this.trunk.add(trunk);
			return this;
		}

		public Builder trunkLayer(TrunkLayerDecorator trunkLayer) {
			if (this.trunkLayer == null) this.trunkLayer = new ArrayList<>(2);
			this.trunkLayer.add(trunkLayer);
			return this;
		}

		public Builder trunkBlock(BlockDecorator block) {
			if (this.trunkBlock == null) this.trunkBlock = new ArrayList<>(2);
			this.trunkBlock.add(block);
			return this;
		}

		public Builder branch(BranchDecorator branch) {
			if (this.branch == null) this.branch = new ArrayList<>(2);
			this.branch.add(branch);
			return this;
		}

		public Builder branchBlock(BlockDecorator block) {
			if (this.branchBlock == null) this.branchBlock = new ArrayList<>(2);
			this.branchBlock.add(block);
			return this;
		}

		public Builder leafBlock(BlockDecorator block) {
			if (this.leafBlock == null) this.leafBlock = new ArrayList<>(2);
			this.leafBlock.add(block);
			return this;
		}

		public static <T> T[] toArray(List<T> list, T[] emptyArray) {
			return list != null ? list.toArray(emptyArray) : emptyArray;
		}

		public DecoratorConfig build() {
			return new DecoratorConfig(
				toArray(this.trunk, EMPTY_TRUNK_ARRAY),
				toArray(this.trunkLayer, EMPTY_TRUNK_LAYER_ARRAY),
				toArray(this.trunkBlock, EMPTY_BLOCK_ARRAY),
				toArray(this.branch, EMPTY_BRANCH_ARRAY),
				toArray(this.branchBlock, EMPTY_BLOCK_ARRAY),
				toArray(this.leafBlock, EMPTY_BLOCK_ARRAY)
			);
		}
	}
}