package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.rendering.lods.flat.LodQuadNode;

@Environment(EnvType.CLIENT)
public abstract class LodTree implements AutoCloseable {

	public abstract LodNode getRoot();

	public abstract int countTotalNodes();

	public abstract int countRenderingNodes();

	public abstract int countMeshyNodes();

	public abstract int countDirtyNodes();

	public String f3Message() {
		int meshedNodes = this.countMeshyNodes();
		int renderingNodes = this.countRenderingNodes();
		int dirtyNodes = this.countDirtyNodes();
		int totalNodes = this.countTotalNodes();
		return "[BG] LOD Nodes: R: " + renderingNodes + ", M: " + meshedNodes + ", D: " + dirtyNodes + ", T: " + totalNodes;
	}


	public abstract @Nullable LodNode getNodeAtPlayerForDowngradeChecking();

	public abstract void updateTree();

	public abstract void invalidateRegion(BoundingBox region);

	public abstract BoundingBox getBounds(LodNode node);

	public static double computeTreeQuality(double squareDistance, LodQuadNode node) {
		return FastMath.Log.fastLog2(squareDistance) * 0.5D - node.level;
	}
}