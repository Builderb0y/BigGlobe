package builderb0y.bigglobe.util.coordinators;

import net.minecraft.util.math.Vec3i;

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorBiConsumer;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorConsumer;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorRunnable;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorTriConsumer;

public abstract class AbstractTranslateCoordinator extends ScratchPosCoordinator {

	public final Coordinator delegate;

	public AbstractTranslateCoordinator(Coordinator delegate) {
		this.delegate = delegate;
	}

	public abstract int offsetX();

	public abstract int offsetY();

	public abstract int offsetZ();

	@Override
	public void genericPos(int x, int y, int z, CoordinatorRunnable callback) {
		callback.run(this.delegate, x + this.offsetX(), y + this.offsetY(), z + this.offsetZ());
	}

	@Override
	public <A> void genericPos(int x, int y, int z, A arg, CoordinatorConsumer<A> callback) {
		callback.run(this.delegate, x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), arg);
	}

	@Override
	public <A, B> void genericPos(int x, int y, int z, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {
		callback.run(this.delegate, x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), arg1, arg2);
	}

	@Override
	public <A, B, C> void genericPos(int x, int y, int z, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {
		callback.run(this.delegate, x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), arg1, arg2, arg3);
	}

	public static class TranslateCoordinator extends AbstractTranslateCoordinator {

		public final int offsetX, offsetY, offsetZ;

		public TranslateCoordinator(Coordinator delegate, int offsetX, int offsetY, int offsetZ) {
			super(delegate);
			assert offsetX != 0 || offsetY != 0 || offsetZ != 0;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
		}

		@Override public int offsetX() { return this.offsetX; }
		@Override public int offsetY() { return this.offsetY; }
		@Override public int offsetZ() { return this.offsetZ; }

		@Override
		public Coordinator translate(int offsetX, int offsetY, int offsetZ) {
			return this.delegate.translate(offsetX + this.offsetX(), offsetY + this.offsetY(), offsetZ + this.offsetZ());
		}

		@Override
		public int hashCode() {
			int hash = this.delegate.hashCode();
			hash = hash * 31 + this.offsetX;
			hash = hash * 31 + this.offsetY;
			hash = hash * 31 + this.offsetZ;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof TranslateCoordinator that)) return false;
			return (
				this.delegate.equals(that.delegate) &&
				this.offsetX == that.offsetX &&
				this.offsetY == that.offsetY &&
				this.offsetZ == that.offsetZ
			);
		}

		@Override
		public String toString() {
			return this.delegate + " translated by (" + this.offsetX + ", " + this.offsetY + ", " + this.offsetZ + ')';
		}
	}

	public static class LazyTranslateCoordinator extends AbstractTranslateCoordinator {

		public final Vec3i offset;

		public LazyTranslateCoordinator(Coordinator delegate, Vec3i offset) {
			super(delegate);
			this.offset = offset;
		}

		@Override public int offsetX() { return this.offset.getX(); }
		@Override public int offsetY() { return this.offset.getY(); }
		@Override public int offsetZ() { return this.offset.getZ(); }

		@Override
		public int hashCode() {
			return this.delegate.hashCode() ^ System.identityHashCode(this.offset);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof LazyTranslateCoordinator that)) return false;
			return this.delegate.equals(that.delegate) && this.offset == that.offset;
		}

		@Override
		public String toString() {
			return this.delegate + " lazily translated by currently (" + this.offset.getX() + ", " + this.offset.getY() + ", " + this.offset.getZ() + ')';
		}
	}
}