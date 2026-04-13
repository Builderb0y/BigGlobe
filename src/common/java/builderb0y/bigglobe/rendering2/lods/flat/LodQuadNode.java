package builderb0y.bigglobe.rendering2.lods.flat;

import net.minecraft.util.Mth;

import builderb0y.bigglobe.rendering2.ResourceTracker;
import builderb0y.bigglobe.rendering2.lods.LodNode;

public class LodQuadNode extends LodNode {

	public static final int
		AREA = SIZE * SIZE,
		MAX_SIZE = Mth.ceillog2(60_000_000),
		MAX_LEVEL = MAX_SIZE - SIZE_SHIFT;

	public final int x, z;
	public LodQuadNode x0z0, x0z1, x1z0, x1z1;

	public LodQuadNode(int x, int z, int level) {
		super(level);
		this.x = x;
		this.z = z;
	}

	public int minX() { return this.x; }
	public int minZ() { return this.z; }
	public int midX() { return this.x + (1 << (this.level + (SIZE_SHIFT - 1))); }
	public int midZ() { return this.z + (1 << (this.level + (SIZE_SHIFT - 1))); }
	public int maxX() { return this.x + (1 << (this.level +  SIZE_SHIFT     )); }
	public int maxZ() { return this.z + (1 << (this.level +  SIZE_SHIFT     )); }

	@Override
	public void split() {
		if (this.x0z0 == null) this.x0z0 = new LodQuadNode(this.minX(), this.minZ(), this.level - 1);
		if (this.x1z0 == null) this.x1z0 = new LodQuadNode(this.midX(), this.minZ(), this.level - 1);
		if (this.x0z1 == null) this.x0z1 = new LodQuadNode(this.minX(), this.midZ(), this.level - 1);
		if (this.x1z1 == null) this.x1z1 = new LodQuadNode(this.midX(), this.midZ(), this.level - 1);
	}

	@Override
	public void free() {
		try {
			ResourceTracker.closeAll(this.x0z0, this.x0z1, this.x1z0, this.x1z1, this.mesh);
		}
		finally {
			this.x0z0 = this.x0z1 = this.x1z0 = this.x1z1 = null;
			this.mesh = null;
		}
	}

	@Override
	public void mergeChildren() {
		try {
			ResourceTracker.closeAll(this.x0z0, this.x0z1, this.x1z0, this.x1z1);
		}
		finally {
			this.x0z0 = this.x0z1 = this.x1z0 = this.x1z1 = null;
		}
	}

	@Override
	public String toString() {
		return "LodQuadNode: [" + this.minX() + ", " + this.minZ() + "] -> [" + this.maxX() + ", " + this.maxZ() + "] @ " + this.level + " (" + this.sizeInBlocks() + ')';
	}
}