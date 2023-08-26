package builderb0y.bigglobe.util.coordinators;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public abstract class AbstractLimitAreaCoordinator implements Coordinator {

	public final Coordinator delegate;

	public AbstractLimitAreaCoordinator(Coordinator delegate) {
		this.delegate = delegate;
	}

	public abstract boolean test(int x, int y, int z);

	@Override
	public void genericPos(int x, int y, int z, CoordinatorRunnable callback) {
		if (this.test(x, y, z)) {
			callback.run(this.delegate, x, y, z);
		}
	}

	@Override
	public <A> void genericPos(int x, int y, int z, A arg, CoordinatorConsumer<A> callback) {
		if (this.test(x, y, z)) {
			callback.run(this.delegate, x, y, z, arg);
		}
	}

	@Override
	public <A, B> void genericPos(int x, int y, int z, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {
		if (this.test(x, y, z)) {
			callback.run(this.delegate, x, y, z, arg1, arg2);
		}
	}

	@Override
	public <A, B, C> void genericPos(int x, int y, int z, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {
		if (this.test(x, y, z)) {
			callback.run(this.delegate, x, y, z, arg1, arg2, arg3);
		}
	}

	public static class LimitArea extends AbstractLimitAreaCoordinator {

		public final CoordinateBooleanSupplier predicate;
		public final BlockPos.Mutable scratchPos;

		public LimitArea(Coordinator delegate, CoordinateBooleanSupplier predicate) {
			super(delegate);
			this.predicate = predicate;
			this.scratchPos = new BlockPos.Mutable();
		}

		@Override
		public boolean test(int x, int y, int z) {
			return this.predicate.test(this.scratchPos.set(x, y, z));
		}

		@Override
		public Coordinator limitArea(CoordinateBooleanSupplier predicate) {
			return this.delegate.limitArea(this.predicate.and(predicate));
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode() ^ this.predicate.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof LimitArea)) return false;
			LimitArea that = (LimitArea)(obj);
			return this.delegate.equals(that.delegate) && this.predicate.equals(that.predicate);
		}

		@Override
		public String toString() {
			return this.delegate + " limited by " + this.predicate;
		}
	}

	public static class InBox extends AbstractLimitAreaCoordinator {

		public final int minX, minY, minZ, maxX, maxY, maxZ;

		public InBox(Coordinator delegate, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(delegate);
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxY = maxY;
			this.maxZ = maxZ;
		}

		public InBox(Coordinator delegate, BlockBox box) {
			this(delegate, box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
		}

		@Override
		public boolean test(int x, int y, int z) {
			return (
				x >= this.minX && x <= this.maxX &&
				z >= this.minZ && z <= this.maxZ &&
				y >= this.minY && y <= this.maxY
			);
		}

		@Override
		public Coordinator inBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			return this.delegate.inBox(
				Math.max(this.minX, minX),
				Math.max(this.minY, minY),
				Math.max(this.minZ, minZ),
				Math.min(this.maxX, maxX),
				Math.min(this.maxY, maxY),
				Math.min(this.maxZ, maxZ)
			);
		}

		@Override
		public int hashCode() {
			int hash = this.delegate.hashCode();
			hash = hash * 31 + this.minX;
			hash = hash * 31 + this.minY;
			hash = hash * 31 + this.minZ;
			hash = hash * 31 + this.maxX;
			hash = hash * 31 + this.maxY;
			hash = hash * 31 + this.maxZ;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof InBox)) return false;
			InBox that = (InBox)(obj);
			return (
				this.delegate.equals(that.delegate) &&
				this.minX == that.minX &&
				this.minY == that.minY &&
				this.minZ == that.minZ &&
				this.maxX == that.maxX &&
				this.maxY == that.maxY &&
				this.maxZ == that.maxZ
			);
		}

		@Override
		public String toString() {
			return this.delegate + " in box (" + this.minX + ", " + this.minY + ", " + this.minZ + ") to (" + this.maxX + ", " + this.maxY + ", " + this.maxZ + ')';
		}
	}

	public static class LazyInBox extends AbstractLimitAreaCoordinator {

		public final BlockBox box;

		public LazyInBox(Coordinator delegate, BlockBox box) {
			super(delegate);
			this.box = box;
		}

		@Override
		public boolean test(int x, int y, int z) {
			return (
				x >= this.box.getMinX() && x <= this.box.getMaxX() &&
				z >= this.box.getMinZ() && z <= this.box.getMaxZ() &&
				y >= this.box.getMinY() && y <= this.box.getMaxY()
			);
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode() ^ System.identityHashCode(this.box);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof LazyInBox)) return false;
			LazyInBox that = (LazyInBox)(obj);
			return this.delegate.equals(that.delegate) && this.box == that.box;
		}

		@Override
		public String toString() {
			return this.delegate + " in lazy box currently at (" + this.box.getMinX() + ", " + this.box.getMinY() + ", " + this.box.getMinZ() + ") to (" + this.box.getMaxX() + ", " + this.box.getMaxY() + ", " + this.box.getMaxZ() + ')';
		}
	}
}