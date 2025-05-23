package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class LodQuadTree implements SafeCloseable {

	public static final int
		MIN_LEVEL = 6,
		MAX_LEVEL = Integer.numberOfTrailingZeros(MathHelper.smallestEncompassingPowerOfTwo(60_000_000));

	public static final int
		CLOSED = 1 << 0,
		QUEUED = 1 << 1,
		CAN_RENDER = 1 << 2,
		TRAVERSABLE_FOR_RENDER = 1 << 3,
		IN_RANGE = 1 << 4;

	public final int x, z, level;
	public @Nullable SafeCloseable passes;
	public @Nullable LodQuadTree x0z0, x0z1, x1z0, x1z1;
	public int flags;

	@Override
	public String toString() {
		return "LodQuadTree: [" + this.minX() + ", " + this.minZ() + "] -> [" + this.maxX() + ", " + this.maxZ() + "] @ " + this.level + " (" + this.sizeInBlocks() + ')';
	}

	public LodQuadTree(int x, int z, int level) {
		this.x = x;
		this.z = z;
		this.level = level;
	}

	public boolean getFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	public void setFlag(int flag, boolean set) {
		assert !this.isClosed() : "Modifying flags of closed LodQuadTree";
		if (set) this.flags |= flag;
		else this.flags &= ~flag;
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

	public boolean canRender() {
		return this.getFlag(CAN_RENDER);
	}

	public void setCanRender(boolean canRender) {
		this.setFlag(CAN_RENDER, canRender);
	}

	public boolean isTraversableForRender() {
		return this.getFlag(TRAVERSABLE_FOR_RENDER);
	}

	public void setTraversableForRender(boolean traversableForRender) {
		this.setFlag(TRAVERSABLE_FOR_RENDER, traversableForRender);
	}

	public boolean isInRange() {
		return this.getFlag(IN_RANGE);
	}

	public void setInRange(boolean inRange) {
		this.setFlag(IN_RANGE, inRange);
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
		//System.out.println("splitting " + this);
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
}