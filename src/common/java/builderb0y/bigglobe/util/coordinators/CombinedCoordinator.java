package builderb0y.bigglobe.util.coordinators;

import java.util.Arrays;

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorBiConsumer;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorConsumer;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorRunnable;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinatorTriConsumer;

public class CombinedCoordinator extends ScratchPosCoordinator {

	public final Coordinator[] delegates;

	public CombinedCoordinator(Coordinator... delegates) {
		this.delegates = delegates;
	}

	@Override
	public void genericPos(int x, int y, int z, CoordinatorRunnable callback) {
		for (Coordinator delegate : this.delegates) {
			callback.run(delegate, x, y, z);
		}
	}

	@Override
	public <A> void genericPos(int x, int y, int z, A arg, CoordinatorConsumer<A> callback) {
		for (Coordinator delegate : this.delegates) {
			callback.run(delegate, x, y, z, arg);
		}
	}

	@Override
	public <A, B> void genericPos(int x, int y, int z, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {
		for (Coordinator delegate : this.delegates) {
			callback.run(delegate, x, y, z, arg1, arg2);
		}
	}

	@Override
	public <A, B, C> void genericPos(int x, int y, int z, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {
		for (Coordinator delegate : this.delegates) {
			callback.run(delegate, x, y, z, arg1, arg2, arg3);
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.delegates);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof CombinedCoordinator)) return false;
		CombinedCoordinator that = (CombinedCoordinator)(obj);
		return Arrays.equals(this.delegates, that.delegates);
	}

	@Override
	public String toString() {
		return Arrays.toString(this.delegates);
	}
}