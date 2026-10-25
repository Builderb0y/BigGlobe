package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public class LodQuadTree implements SafeCloseable {

	public static final int
		MIN_LEVEL = 6,
		MAX_LEVEL = Integer.numberOfTrailingZeros(Mth.smallestEncompassingPowerOfTwo(60_000_000));

	public static final int
		CLOSED = 1 << 0,
		QUEUED = 1 << 1,
		IN_RANGE = 1 << 4,
		IN_FRUSTUM = 1 << 5;

	public final int x, z;
	public final byte level;
	public byte flags, ancestorDepth;
	public @Nullable SafeCloseable passes;
	public @Nullable LodQuadTree x0z0, x0z1, x1z0, x1z1;
	public long rebuildTime = Long.MAX_VALUE;

	@Override
	public String toString() {
		return "LodQuadTree: [" + this.minX() + ", " + this.minZ() + "] -> [" + this.maxX() + ", " + this.maxZ() + "] @ " + this.level + " (" + this.sizeInBlocks() + ')';
	}

	public LodQuadTree(int x, int z, int level) {
		this.x = x;
		this.z = z;
		this.level = (byte)(level);
	}

	public boolean getFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	public void setFlag(int flag, boolean set) {
		assert !this.isClosed() : "Modifying flags of closed LodQuadTree";
		if (set) this.flags |= flag;
		else this.flags &= ~flag;
	}

	public int getAncestorDepth() {
		return this.ancestorDepth;
	}

	public void setAncestorDepth(int ancestorDepth) {
		this.ancestorDepth = (byte)(ancestorDepth);
	}

	public boolean isClosed() {
		return this.getFlag(CLOSED);
	}

	public void setClosed(boolean closed) {
		this.setFlag(CLOSED, closed);
	}

	public boolean isQueued() {
		return this.getFlag(QUEUED);
	}

	public void setQueued(boolean queued) {
		this.setFlag(QUEUED, queued);
	}

	public boolean isInRange() {
		return this.getFlag(IN_RANGE);
	}

	public void setInRange(boolean inRange) {
		this.setFlag(IN_RANGE, inRange);
	}

	public boolean isInFrustum() {
		return this.getFlag(IN_FRUSTUM);
	}

	public void setInFrustum(boolean inFrustum) {
		this.setFlag(IN_FRUSTUM, inFrustum);
	}

	public int minX() {
		return this.x;
	}

	public int minZ() {
		return this.z;
	}

	public int maxX() {
		return this.x + (1 << this.level);
	}

	public int maxZ() {
		return this.z + (1 << this.level);
	}

	public int midX() {
		return this.x + (1 << (this.level - 1));
	}

	public int midZ() {
		return this.z + (1 << (this.level - 1));
	}

	public int sizeInBlocks() {
		return 1 << this.level;
	}

	public void split() {
		if (this.x0z0 == null) this.x0z0 = new LodQuadTree(this.minX(), this.minZ(), this.level - 1);
		if (this.x1z0 == null) this.x1z0 = new LodQuadTree(this.midX(), this.minZ(), this.level - 1);
		if (this.x0z1 == null) this.x0z1 = new LodQuadTree(this.minX(), this.midZ(), this.level - 1);
		if (this.x1z1 == null) this.x1z1 = new LodQuadTree(this.midX(), this.midZ(), this.level - 1);
	}

	public void merge() {
		this.setQueued(false);
		this.free();
	}

	@Override
	public void close() {
		this.flags = CLOSED;
		this.free();
	}

	public void free() {
		ResourceTracker.closeAll(this.x0z0, this.x0z1, this.x1z0, this.x1z1, this.passes);
		this.x0z0 = this.x0z1 = this.x1z0 = this.x1z1 = null;
		this.passes = null;
	}

	public void mergeChildren() {
		ResourceTracker.closeAll(this.x0z0, this.x0z1, this.x1z0, this.x1z1);
		this.x0z0 = this.x0z1 = this.x1z0 = this.x1z1 = null;
	}

	public void unload() {
		this.setQueued(false);
		if (this.passes != null) {
			this.passes.close();
			this.passes = null;
		}
	}
}