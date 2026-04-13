package builderb0y.bigglobe.rendering.lods.flat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.rendering.lods.GenerationPipeline;
import builderb0y.bigglobe.rendering.lods.LodFrustum;
import builderb0y.bigglobe.rendering.lods.LodGenerator.LoadMode;
import builderb0y.bigglobe.rendering.lods.LodNode;
import builderb0y.bigglobe.rendering.lods.LodTree;

@Environment(EnvType.CLIENT)
public class FlatLodTree extends LodTree {

	public final FlatLodSystem system;
	public LodQuadNode root;

	public FlatLodTree(FlatLodSystem system) {
		this.system = system;
		this.root = new LodQuadNode(-(1 << (LodQuadNode.MAX_SIZE - 1)), -(1 << (LodQuadNode.MAX_SIZE - 1)), LodQuadNode.MAX_LEVEL);
	}

	@Override
	public LodNode getRoot() {
		return this.root;
	}

	@Override
	public int countTotalNodes() {
		return this.countTotalNodes(this.root);
	}

	public int countTotalNodes(LodQuadNode root) {
		return root == null ? 0 : (
			+ this.countTotalNodes(root.x0z0)
			+ this.countTotalNodes(root.x1z0)
			+ this.countTotalNodes(root.x0z1)
			+ this.countTotalNodes(root.x1z1)
			+ 1
		);
	}

	@Override
	public int countRenderingNodes() {
		return this.countRenderingNodes(this.root);
	}

	public int countRenderingNodes(LodQuadNode root) {
		if (root != null && root.isInRange() && root.isInFrustum()) {
			return root.getAncestorDepth() == 1 ? 1 : (
				+ this.countRenderingNodes(root.x0z0)
				+ this.countRenderingNodes(root.x0z1)
				+ this.countRenderingNodes(root.x1z0)
				+ this.countRenderingNodes(root.x1z1)
			);
		}
		return 0;
	}

	@Override
	public int countMeshyNodes() {
		return this.countMeshyNodes(this.root);
	}

	public int countMeshyNodes(LodQuadNode root) {
		return root == null ? 0 : (
			+ (root.mesh != null ? 1 : 0)
			+ this.countMeshyNodes(root.x0z0)
			+ this.countMeshyNodes(root.x0z1)
			+ this.countMeshyNodes(root.x1z0)
			+ this.countMeshyNodes(root.x1z1)
		);
	}

	@Override
	public int countDirtyNodes() {
		return this.countDirtyNodes(this.root);
	}

	public int countDirtyNodes(LodQuadNode root) {
		return root == null ? 0 : (
			+ (root.rebuildTime != Long.MAX_VALUE ? 1 : 0)
			+ this.countDirtyNodes(root.x0z0)
			+ this.countDirtyNodes(root.x0z1)
			+ this.countDirtyNodes(root.x1z0)
			+ this.countDirtyNodes(root.x1z1)
		);
	}

	@Override
	public BoundingBox getBounds(LodNode node) {
		return this.getBounds((LodQuadNode)(node));
	}

	public BoundingBox getBounds(LodQuadNode node) {
		return new BoundingBox(
			node.minX(),
			this.system.params.minY,
			node.minZ(),
			node.maxX() - 1,
			this.system.params.maxY - 1,
			node.maxZ() - 1
		);
	}

	@Override
	public @Nullable LodNode getNodeAtPlayerForDowngradeChecking() {
		LodFrustum frustum = this.system.getRenderer().frustum;
		//don't downgrade anything if we're above the world,
		//because the LodQuadTree at the player's horizontal position
		//will not match our current quality in this case no matter how long we wait.
		if (frustum.y > this.system.params.maxY) {
			return null;
		}
		//using previous frame data is fine here.
		LodQuadNode atPlayer = this.root;
		while (atPlayer.getAncestorDepth() != 1) {
			LodQuadNode next;
			if (frustum.x >= atPlayer.midX()) {
				if (frustum.z >= atPlayer.midZ()) {
					next = atPlayer.x1z1;
				}
				else {
					next = atPlayer.x1z0;
				}
			}
			else {
				if (frustum.z >= atPlayer.midZ()) {
					next = atPlayer.x0z1;
				}
				else {
					next = atPlayer.x0z0;
				}
			}
			if (next != null) atPlayer = next;
			else break;
		}
		return atPlayer;
	}

	@Override
	public void updateTree() {
		this.updateTree(this.root, System.currentTimeMillis(), null);
	}

	/**
	return value:
	0 - tree is non-existent (null or has no passes)
	1 - tree exists, but at least one child doesn't.
	2 - all tree children exist, but at least one grandchild doesn't.
	3 - all tree grandchildren exist, but at least one great grandchild doesn't.
	etc.
	*/
	public int updateTree(LodQuadNode tree, long time, Boolean frustumVisible) {
		if (tree == null) {
			return 0;
		}

		double squareDistance = this.squareDistanceTo(tree);
		double treeQuality = computeTreeQuality(squareDistance, tree);
		boolean inGenerationRange = squareDistance < BigGlobeMath.squareD(this.system.getRenderer().frustum.generationBuffer);
		boolean inRenderingRange = squareDistance < BigGlobeMath.squareD(this.system.getRenderer().frustum.farClippingPlane);
		boolean inLoadingRange = squareDistance < BigGlobeMath.squareD(this.system.loadDistance);
		boolean awaitingMerge = false;
		tree.setInRange(inRenderingRange);
		if (inGenerationRange) {
			if (treeQuality < this.system.qualityLimit) {
				if (treeQuality < this.system.currentQuality && tree.level > this.system.levelLimit) {
					tree.split();
				}
			}
			else {
				if (tree.mesh != null) {
					tree.mergeChildren();
				}
				else if (!tree.isQueued()) {
					this.system.generator.request(tree, inLoadingRange ? LoadMode.LOAD_OR_GENERATE : LoadMode.GENERATE_ONLY);
				}
				awaitingMerge = true;
			}
		}
		else {
			tree.merge();
		}

		if (tree.level > 0) {
			if (frustumVisible == null) {
				frustumVisible = this.system.getRenderer().frustum.test(
					tree.minX(),
					this.system.params.minY,
					tree.minZ(),
					tree.maxX(),
					this.system.params.maxY,
					tree.maxZ()
				);
			}
			int depth = min4(
				this.updateTree(tree.x0z0, time, frustumVisible),
				this.updateTree(tree.x0z1, time, frustumVisible),
				this.updateTree(tree.x1z0, time, frustumVisible),
				this.updateTree(tree.x1z1, time, frustumVisible)
			);
			//nothing exists: 0
			//only this tree exists: 1
			//all 4 children exist: child depth + 1
			//tree and children exist: child depth + 1
			if (depth > 0 || tree.mesh != null || !inRenderingRange) {
				depth++;
			}
			tree.setAncestorDepth(depth);
		}
		else {
			tree.setAncestorDepth(tree.mesh != null ? 1 : 0);
		}

		switch (tree.getAncestorDepth()) {
			case 0 -> {
				if (inGenerationRange && !tree.isQueued()) {
					this.system.generator.request(tree, inLoadingRange ? LoadMode.LOAD_OR_GENERATE : LoadMode.GENERATE_ONLY);
				}
			}
			case 1 -> {
				if (inLoadingRange && time > tree.rebuildTime && !tree.isQueued()) {
					this.system.generator.request(tree, LoadMode.LOAD_ONLY);
				}
			}
			default -> {
				if (!awaitingMerge) {
					tree.unload();
				}
			}
		}

		tree.setInFrustum(frustumVisible != Boolean.FALSE);
		return tree.getAncestorDepth();
	}

	public static int min4(int a, int b, int c, int d) {
		return Math.min(Math.min(a, b), Math.min(c, d));
	}

	@Override
	public void invalidateRegion(BoundingBox region) {
		this.invalidateRegion(region, this.root, System.currentTimeMillis() + GenerationPipeline.CHUNKLOAD_DELAY_MS);
	}

	public void invalidateRegion(BoundingBox region, LodQuadNode node, long time) {
		if (node != null && region.intersects(node.minX(), node.minZ(), node.maxX() - 1, node.maxZ() - 1)) {
			if (node.level < this.system.generator.generator.maxLoadLevel) {
				node.rebuildTime = time;
			}
			this.invalidateRegion(region, node.x0z0, time);
			this.invalidateRegion(region, node.x0z1, time);
			this.invalidateRegion(region, node.x1z0, time);
			this.invalidateRegion(region, node.x1z1, time);
		}
	}

	public double squareDistanceTo(LodQuadNode node) {
		LodFrustum frustum = this.system.getRenderer().frustum;
		double closestX = Mth.clamp(frustum.x, node.minX(), node.maxX());
		double closestY = Mth.clamp(frustum.y, this.system.params.minY, this.system.params.maxY);
		double closestZ = Mth.clamp(frustum.z, node.minZ(), node.maxZ());
		return BigGlobeMath.squareD(closestX - frustum.x, closestY - frustum.y, closestZ - frustum.z);
	}

	@Override
	public void close() throws Exception {
		if (this.root != null) this.root.close();
	}
}