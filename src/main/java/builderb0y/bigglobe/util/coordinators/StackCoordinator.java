package builderb0y.bigglobe.util.coordinators;

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorBiConsumer;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorConsumer;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorRunnable;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorTriConsumer;

public class StackCoordinator extends ScratchPosCoordinator {

	public final Coordinator delegate;
	public final int dx, dy, dz, count;

	public StackCoordinator(Coordinator delegate, int dx, int dy, int dz, int count) {
		assert count > 1;
		assert dx != 0 || dy != 0 || dz != 0;
		this.delegate = delegate;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.count = count;
	}

	@Override
	public void genericPos(int x, int y, int z, CoordinatorRunnable callback) {
		callback.run(this.delegate, x, y, z);
		callback.run(this.delegate, x + this.dx, y + this.dy, z + this.dz);
		for (int i = 2; i < this.count; i++) {
			callback.run(this.delegate, x + this.dx * i, y + this.dy * i, z + this.dz * i);
		}
	}

	@Override
	public <A> void genericPos(int x, int y, int z, A arg, CoordinatorConsumer<A> callback) {
		callback.run(this.delegate, x, y, z, arg);
		callback.run(this.delegate, x + this.dx, y + this.dy, z + this.dz, arg);
		for (int i = 2; i < this.count; i++) {
			callback.run(this.delegate, x + this.dx * i, y + this.dy * i, z + this.dz * i, arg);
		}
	}

	@Override
	public <A, B> void genericPos(int x, int y, int z, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {
		callback.run(this.delegate, x, y, z, arg1, arg2);
		callback.run(this.delegate, x + this.dx, y + this.dy, z + this.dz, arg1, arg2);
		for (int i = 2; i < this.count; i++) {
			callback.run(this.delegate, x + this.dx * i, y + this.dy * i, z + this.dz * i, arg1, arg2);
		}
	}

	@Override
	public <A, B, C> void genericPos(int x, int y, int z, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {
		callback.run(this.delegate, x, y, z, arg1, arg2, arg3);
		callback.run(this.delegate, x + this.dx, y + this.dy, z + this.dz, arg1, arg2, arg3);
		for (int i = 2; i < this.count; i++) {
			callback.run(this.delegate, x + this.dx * i, y + this.dy * i, z + this.dz * i, arg1, arg2, arg3);
		}
	}

	@Override
	public int hashCode() {
		int hash = this.delegate.hashCode();
		hash = hash * 31 + this.dx;
		hash = hash * 31 + this.dy;
		hash = hash * 31 + this.dz;
		hash = hash * 31 + this.count;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof StackCoordinator)) return false;
		StackCoordinator that = (StackCoordinator)(obj);
		return (
			this.delegate.equals(that.delegate) &&
			this.dx == that.dx &&
			this.dy == that.dy &&
			this.dz == that.dz &&
			this.count == that.count
		);
	}

	@Override
	public String toString() {
		return this.delegate.toString() + " stacked " + this.count + "x (" + this.dx + ", " + this.dy + ", " + this.dz + ')';
	}
}