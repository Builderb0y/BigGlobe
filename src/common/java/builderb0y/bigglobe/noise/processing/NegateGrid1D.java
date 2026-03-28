package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid1D;

public class NegateGrid1D extends NegateGrid implements UnaryGrid1D {

	public final Grid1D grid;

	public NegateGrid1D(Grid1D grid) {
		this.grid = grid;
	}

	@Override
	public Grid1D getGrid() {
		return this.grid;
	}
}