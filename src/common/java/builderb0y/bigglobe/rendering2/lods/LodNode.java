package builderb0y.bigglobe.rendering2.lods;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.util.SafeCloseable;

public abstract class LodNode implements SafeCloseable {

	public static final int
		SIZE_SHIFT = 6,
		SIZE = 1 << SIZE_SHIFT;
	public static final int
		CLOSED = 1 << 0,
		QUEUED = 1 << 1,
		IN_RANGE = 1 << 4,
		IN_FRUSTUM = 1 << 5;

	public final byte level;
	public byte flags, ancestorDepth;
	public @Nullable SafeCloseable passes;
	public long rebuildTime = Long.MAX_VALUE;

	public LodNode(int level) {
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

	public int distanceBetweenSamples() {
		return 1 << this.level;
	}

	public int sizeInBlocks() {
		return 1 << (this.level + SIZE_SHIFT);
	}

	public abstract void split();

	public void merge() {
		this.setQueued(false);
		this.free();
	}

	@Override
	public void close() {
		this.flags = CLOSED;
		this.free();
	}

	public abstract void free();

	public abstract void mergeChildren();

	public void unload() {
		this.setQueued(false);
		SafeCloseable passes = this.passes;
		if (passes != null) {
			this.passes = null;
			passes.close();
		}
	}

	@Override
	public abstract String toString();
}