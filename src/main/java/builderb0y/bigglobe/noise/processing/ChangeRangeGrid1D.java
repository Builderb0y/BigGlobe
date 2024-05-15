package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid1D;

public class ChangeRangeGrid1D extends ChangeRangeGrid implements UnaryGrid1D {

	public final Grid1D grid;

	public ChangeRangeGrid1D(Grid1D grid, double min, double max) {
		super(grid, min, max);
		this.grid = grid;
	}

	@Override
	public Grid1D getGrid() {
		return this.grid;
	}
}