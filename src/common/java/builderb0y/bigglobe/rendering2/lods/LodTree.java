package builderb0y.bigglobe.rendering2.lods;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.rendering2.lods.flat.LodQuadNode;

public abstract class LodTree implements AutoCloseable {

	public abstract LodNode getRoot();

	public abstract int countTotalNodes();

	public abstract int countRenderingNodes();

	public abstract int countMeshyNodes();

	public abstract int countDirtyNodes();

	public abstract void pruneTree();

	public abstract @Nullable LodNode getNodeAtPlayerForDowngradeChecking();

	public abstract void drawTree();

	public abstract void updateTree();

	public abstract void invalidateRegion(BoundingBox region);

	public static double computeTreeQuality(double squareDistance, LodQuadNode node) {
		return FastMath.Log.fastLog2(squareDistance) * 0.5D - node.level;
	}
}